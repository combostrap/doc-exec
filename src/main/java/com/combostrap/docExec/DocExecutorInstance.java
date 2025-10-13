package com.combostrap.docExec;


import com.combostrap.docExec.util.Fs;
import com.combostrap.docExec.util.Strings;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DocExecutorInstance {

    public static final String STOP_AT_FIRST_ERROR = "Stop at first error";
    private final DocExecutor docExecutor;
    private final String eol = System.lineSeparator();

    public DocExecutorInstance(DocExecutor docExecutor) {
        this.docExecutor = docExecutor;
    }

    /**
     * Execute doc test file and the child of directory defined by the paths parameter
     *
     * @param paths the files to execute
     * @return the list of results
     */
    public List<DocExecutorResult> run(Path... paths) {

        String packageName = DocExecutorInstance.class.getPackage().getName();
        Logger logger = Logger.getLogger(packageName);
        logger.setLevel(docExecutor.getLogLevel());

        List<DocExecutorResult> results = new ArrayList<>();
        for (Path path : paths) {

            if (!Files.exists(path)) {
                String msg = "The path (" + path.toAbsolutePath() + ") does not exist";
                DocLog.LOGGER.severe(docExecutor.getName() + " " + msg);
                throw new RuntimeException(msg);
            }

            List<Path> childPaths = Fs.getDescendantFiles(path);

            for (Path childPath : childPaths) {

                /**
                 * Cache ?
                 */
                if (docExecutor.getDocCache() != null) {
                    String md5Cache = docExecutor.getDocCache().getMd5(childPath);
                    String md5 = Fs.getMd5(childPath);
                    if (md5.equals(md5Cache)) {
                        DocLog.LOGGER.info(docExecutor.getName() + " - Cache is on and the file (" + childPath + ") has already been executed. Skipping the execution");
                        DocExecutorResult docExecutorResult =
                                DocExecutorResult
                                        .get(childPath)
                                        .setCacheHit(true);
                        results.add(docExecutorResult);
                        continue;
                    }
                }

                /**
                 * Execution
                 */
                DocLog.LOGGER.info(docExecutor.getName() + " - Executing the doc file (" + childPath + ")");
                DocExecutorResult docExecutorResult;
                try {
                    docExecutorResult = this.execute(childPath);
                } catch (NoSuchFileException e) {
                    throw new RuntimeException(e);
                }
                results.add(docExecutorResult);
                if (docExecutor.isOverwrite()) {
                    // Overwrite the new doc
                    Fs.toFile(docExecutorResult.getNewDoc(), childPath);
                }

                if (docExecutor.getDocCache() != null) {
                    docExecutor.getDocCache().store(childPath);
                }

                if (docExecutorResult.hasWarnings()) {
                    for (String warning : docExecutorResult.getWarnings()) {
                        System.err.println("Warning: " + warning);
                    }
                    throw new DocWarning("Warning were seen");
                }

            }
        }
        return results;

    }

    /**
     * @param path the doc to execute
     * @return the new page
     */
    private DocExecutorResult execute(Path path) throws NoSuchFileException {

        DocExecutorResult docExecutorResult = DocExecutorResult
                .get(path)
                .setHasBeenExecuted(true);

        // Parsing
        List<DocUnit> docTests = DocParser.getDocTests(path);
        String originalDoc = Fs.toString(path);
        StringBuilder targetDoc = new StringBuilder();

        // A code executor
        DocExecutorUnit docExecutorUnit = DocExecutorUnit.create(docExecutor);

        List<DocUnit> cachedDocUnits = new ArrayList<>();
        if (docExecutor.getDocCache() != null) {
            cachedDocUnits = docExecutor.getDocCache().getDocTestUnits(path);
        }
        Integer previousEnd = 0;
        boolean oneCodeBlockHasAlreadyRun = false;
        for (int i = 0; i < docTests.size(); i++) {

            DocUnit docUnit = docTests.get(i);
            DocUnit cachedDocUnit = null;
            if (cachedDocUnits != null && i < cachedDocUnits.size()) {
                cachedDocUnit = cachedDocUnits.get(i);
            }
            // Boolean to decide if we need to execute
            boolean codeChange = false;
            boolean fileChange = false;
            // ############################################
            // The order of execution is important here to reconstruct the new document
            //    * First the processing of the file nodes
            //    * then the code
            //    * then the console
            // ############################################

            // Replace file node with the file content on the file system
            final List<DocFileBlock> files = docUnit.getFileBlocks();
            if (!files.isEmpty()) {

                for (int j = 0; j < files.size(); j++) {

                    DocFileBlock docFileBlock = files.get(j);

                    final String fileStringPath = docFileBlock.getPath();
                    if (fileStringPath == null) {
                        throw new RuntimeException("The file path for this unit is null (<file type file.extension>");
                    }
                    // No need of cache test here because it's going very quick
                    if (cachedDocUnit != null) {
                        List<DocFileBlock> fileBlocks = cachedDocUnit.getFileBlocks();
                        if (fileBlocks.size() > j) {
                            DocFileBlock cachedDocFileBlock = fileBlocks.get(j);
                            if (!(fileStringPath.equals(cachedDocFileBlock.getPath()))) {
                                fileChange = true;
                            }
                        }
                    } else {
                        fileChange = true;
                    }


                    Path filePath = searchInlineFile(fileStringPath);
                    String fileContent = Fs.toString(filePath);

                    int start = docFileBlock.getLocationStart();
                    targetDoc.append(originalDoc, previousEnd, start);

                    DocLog.LOGGER.info(docExecutor.getName() + " - Replacing the file block (" + DocLog.onOneLine(docFileBlock.getPath()) + ") from the file (" + docUnit.getPath() + ")");
                    targetDoc
                            .append(eol)
                            .append(fileContent)
                            .append(eol);

                    previousEnd = docFileBlock.getLocationEnd();

                }
            }

            // ######################## Code Block Processing #####################
            // Code block is not mandatory, you may just have a file
            String code = docUnit.getCode();
            if (code != null && !code.trim().isEmpty()) {
                // Check if this unit has already been executed and that the code has not changed
                if (cachedDocUnit != null) {
                    if (!(code.equals(cachedDocUnit.getCode()))) {
                        codeChange = true;
                    }
                } else {
                    codeChange = true;
                }

                // Run
                String result;
                if (
                        ((codeChange || fileChange) & cacheIsOn())
                                || (!cacheIsOn())
                                || oneCodeBlockHasAlreadyRun
                ) {
                    DocLog.LOGGER.info(docExecutor.getName() + " - Running the code (" + DocLog.onOneLine(code) + ") from the file (" + docUnit.getPath() + ")");
                    try {
                        docExecutorResult.incrementCodeExecutionCounter();
                        result = docExecutorUnit.run(docUnit).trim();
                        DocLog.LOGGER.fine(docExecutor.getName() + " - Code executed, no error");
                        oneCodeBlockHasAlreadyRun = true;
                    } catch (Exception e) {
                        docExecutorResult.addError();


                        if (docExecutor.doesStopAtFirstError()) {
                            DocLog.LOGGER.fine(docExecutor.getName() + " - Stop at first run. Throwing the error");
                            /**
                             * The message can be huge if the error adds a usage
                             * We don't add it in message
                             */
                            throw new RuntimeException(STOP_AT_FIRST_ERROR, e);
                        } else {
                            if (e.getClass().equals(NullPointerException.class)) {
                                result = "null pointer exception";
                            } else {
                                result = e.getMessage();
                            }
                            DocLog.LOGGER.severe(docExecutor.getName() + " - Error during execute: " + result);
                        }
                    }
                } else {
                    DocLog.LOGGER.info(docExecutor.getName() + " - The run of the code (" + DocLog.onOneLine(code) + ") was skipped due to caching from the file (" + docUnit.getPath() + ")");
                    assert cachedDocUnit != null;
                    result = cachedDocUnit.getConsole();
                }

                // Console
                DocBlockLocation consoleLocation = docUnit.getConsoleLocation();
                if (consoleLocation != null) {
                    int start = consoleLocation.getStart();
                    targetDoc.append(originalDoc, previousEnd, start);
                    String console = docUnit.getConsole();
                    if (console == null) {
                        throw new RuntimeException("No console were found, try a run without cache");
                    }
                    // The result does not have the EOL, so th console should not
                    // <console>
                    //   output
                    // </console>
                    String consoleTrim = console.trim();
                    if (!result.equals(consoleTrim)) {

                        int resultLineCount = Strings.getLineCount(result);
                        int actualConsoleLineCount = Strings.getLineCount(consoleTrim);
                        if (resultLineCount < actualConsoleLineCount && docExecutor.isContentShrinkingWarning()) {
                            String s = "A unit code produces less console lines (" + resultLineCount + ") than the actual (" + actualConsoleLineCount + ") in the page. Unit code: " + Strings.toPrintableCharacter(docUnit.getCode());
                            docExecutorResult.addWarning(s);
                        }
                        targetDoc
                                .append(eol)
                                .append(result)
                                .append(eol);

                        previousEnd = consoleLocation.getEnd();

                    } else {

                        previousEnd = consoleLocation.getStart();

                    }
                }
            }
        }
        targetDoc.append(originalDoc, previousEnd, originalDoc.length());
        docExecutorResult.setNewDoc(targetDoc.toString());
        return docExecutorResult;

    }

    /**
     * Search an inline file in the list of search paths
     * @param fileStringPath - the relative path found in the doc
     * @return the real path
     * @throws RuntimeException if not found
     */
    private Path searchInlineFile(String fileStringPath) {
        List<Path> resolvedPaths = new ArrayList<>();
        for (Path searchFile : docExecutor.getSearchFilePaths()) {
            Path file = searchFile.resolve(fileStringPath);
            resolvedPaths.add(file);
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        throw new RuntimeException("The file path (" + fileStringPath + ") found in the doc was not found. No files located at: " + resolvedPaths.stream().map(Path::toAbsolutePath).map(Path::toString).sorted().collect(Collectors.joining(", ")));

    }

    /**
     * @return if the cache is on
     */
    private Boolean cacheIsOn() {
        return docExecutor.getDocCache() != null;
    }

    public DocCache getCache() {
        return docExecutor.getDocCache();
    }
}
