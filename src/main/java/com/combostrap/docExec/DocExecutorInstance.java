package com.combostrap.docExec;


import com.combostrap.docExec.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The instance that gets all env and derived property
 * This instance is printed with {@link DocExecutorCliEnvCommand}
 * So the name of the properties have meaning
 */
public class DocExecutorInstance {

    public static final String STOP_AT_FIRST_ERROR = "Stop at first error";
    private final DocExecutor conf;
    private final String eol = System.lineSeparator();

    @JsonIgnore
    private final DocLog log;
    @JsonIgnore
    private final DocExecutorUnit docExecutorUnit;
    private final DocCache cache;
    @JsonIgnore
    private ProgressDisplay progressDisplay;
    private final DocExecutorResultStore results;

    public DocExecutorInstance(DocExecutor conf) {
        this.conf = conf;
        /**
         * Log Init
         */
        this.log = DocLog.build(conf);
        /**
         * The unit runner
         */
        this.docExecutorUnit = DocExecutorUnit.create(this);
        /**
         * The result store
         */
        results = new DocExecutorResultStore(this);
        if (this.conf.getIsCacheEnabled()) {
            this.cache = DocCache.get(this.conf.getName());
        } else {
            this.cache = null;
        }
    }

    /**
     * Execute doc test file and the child of directory defined by the paths parameter
     *
     * @param paths the files to execute
     * @return the list of results
     */
    public DocExecutorResultRun run(Path... paths) {

        DocExecutorResultRun docExecutorResultRun = new DocExecutorResultRun(this);
        this.progressDisplay = new ProgressDisplay(paths.length, false);

        DocCache docCache = this.cache;
        if (docCache != null && conf.getPurgeCache()) {
            docCache.purgeAll();
        }

        for (Path path : paths) {

            /**
             * We skip at execution, not at selection so that
             * we get the messages in order of execution
             */
            Path resumeFrom = this.conf.getResumeFromPath();
            if (resumeFrom != null && Sorts.naturalSortComparator(resumeFrom.toString(), path.toString()) > 0) {
                progressDisplay.addExecutionClosingStatus("ResumeFrom is on. Skipping: " + path);
                continue;
            }

            if (!Files.exists(path)) {
                String msg = "The path (" + path.toAbsolutePath() + ") does not exist";
                this.log.severe(msg);
                throw new RuntimeException(msg);
            }

            List<Path> childPaths = Fs.getDescendantFiles(path);

            for (Path childPath : childPaths) {

                /**
                 * Cache ?
                 */
                if (docCache != null) {
                    String md5Cache = docCache.getMd5(childPath);
                    String md5 = Fs.getMd5(childPath);
                    if (md5.equals(md5Cache)) {
                        progressDisplay.addExecutionClosingStatus("Cache is on and the file (" + childPath + ") has already been executed. Skipping the execution.");
                        docExecutorResultRun.createResultForDoc(childPath)
                                .setCacheHit(true);
                        continue;
                    }
                }

                /**
                 * Execution
                 */
                progressDisplay.addExecutionStatus("Executing the doc file (" + childPath + ")");
                DocExecutorResultDocExecution docExecutorResultDocExecution;
                try {
                    docExecutorResultDocExecution = this.execute(docExecutorResultRun, childPath);
                } catch (NoSuchFileException e) {
                    throw new RuntimeException(e);
                }

                if (!conf.getIsDryRun()) {
                    // Overwrite the new doc
                    Fs.toFile(docExecutorResultDocExecution.getNewDoc(), childPath);
                }

                if (docCache != null) {
                    docCache.store(childPath);
                }

                if (docExecutorResultDocExecution.hasWarnings()) {
                    for (String warning : docExecutorResultDocExecution.getWarnings()) {
                        System.err.println("Warning: " + warning);
                    }
                    throw new DocWarning("Warning were seen");
                }

                progressDisplay.addExecutionClosingStatus("Doc file (" + childPath + ") executed successfully");
            }
        }

        return docExecutorResultRun;

    }

    /**
     * @param path the doc to execute
     * @return the new page
     */
    private DocExecutorResultDocExecution execute(DocExecutorResultRun docExecutorResultRun, Path path) throws NoSuchFileException {

        DocExecutorResultDocExecution docExecutorResultDocExecution = docExecutorResultRun.createResultForDoc(path)
                .setHasBeenExecuted(true);

        // Parsing
        List<DocUnit> docTests = DocParser.getDocTests(path);
        String originalDoc = Fs.toString(path);
        StringBuilder targetDoc = new StringBuilder();

        // A code executor
        DocExecutorUnit docExecutorUnit = DocExecutorUnit.create(this);

        List<DocUnit> cachedDocUnits = new ArrayList<>();
        if (this.cache != null) {
            cachedDocUnits = this.cache.getDocTestUnits(path);
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

                    this.progressDisplay.addExecutionStatus("Replacing the file block (" + DocLog.onOneLine(docFileBlock.getPath()) + ") from the file (" + docUnit.getPath() + ")");
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
                    this.progressDisplay.addExecutionStatus("Running the code (" + DocLog.onOneLine(code) + ") from the file (" + docUnit.getPath() + ")");
                    try {
                        docExecutorResultDocExecution.incrementCodeExecutionCounter();
                        result = docExecutorUnit.run(docUnit).trim();
                        this.log.fine("Code executed, no error");
                        oneCodeBlockHasAlreadyRun = true;
                    } catch (Exception e) {
                        docExecutorResultDocExecution.addError();

                        if (conf.doesStopAtFirstError()) {
                            this.progressDisplay.addExecutionStatus("Stop at first run. Throwing the error");
                            /**
                             * The message can be huge if the error adds a usage
                             * We don't add it in message
                             */
                            throw new DocFirstError(STOP_AT_FIRST_ERROR, e);
                        } else {
                            if (e.getClass().equals(NullPointerException.class)) {
                                result = "null pointer exception";
                            } else {
                                result = e.getMessage();
                            }
                            this.log.severe("Error during execute: " + result);
                        }
                    }
                } else {
                    this.progressDisplay.addExecutionClosingStatus("The run of the code (" + DocLog.onOneLine(code) + ") was skipped due to caching from the file (" + docUnit.getPath() + ")");
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
                        if (resultLineCount < actualConsoleLineCount && conf.isContentShrinkingWarning()) {
                            String s = "A unit code produces less console lines (" + resultLineCount + ") than the actual (" + actualConsoleLineCount + ") in the page. Unit code: " + Strings.toPrintableCharacter(docUnit.getCode());
                            docExecutorResultDocExecution.addWarning(s);
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
        docExecutorResultDocExecution.setNewDoc(targetDoc.toString());
        return docExecutorResultDocExecution;

    }

    /**
     * Search an inline file in the list of search paths
     *
     * @param fileStringPath - the relative path found in the doc
     * @return the real path
     * @throws RuntimeException if not found
     */
    private Path searchInlineFile(String fileStringPath) {
        List<Path> resolvedPaths = new ArrayList<>();
        for (Path searchFile : conf.getSearchFilePaths()) {
            Path file = searchFile.resolve(fileStringPath);
            resolvedPaths.add(file);
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        throw new RuntimeException("The file path (" + fileStringPath + ") found in the doc was not found. No files located at: " + resolvedPaths.stream().map(Path::toAbsolutePath).map(Path::normalize).map(Path::toString).sorted().collect(Collectors.joining(", ")));

    }

    /**
     * @return if the cache is on
     */
    private Boolean cacheIsOn() {
        return this.cache != null;
    }

    public DocCache getCache() {
        return this.cache;
    }

    public DocExecutorResultRun run(String... globPaths) {
        return run(Arrays.asList(globPaths));
    }

    public DocExecutorResultRun run(List<String> globPaths) {

        List<Path> totalPaths = getPathsFromGlobs(globPaths);
        return run(totalPaths.toArray(new Path[0]));
    }

    protected List<Path> getPathsFromGlobs(String... globPaths) {
        return getPathsFromGlobs(Arrays.asList(globPaths));
    }

    protected List<Path> getPathsFromGlobs(List<String> globPaths) {
        this.log.infoFirstLevel("Processing " + globPaths.size() + " glob path(s)...");
        List<Path> totalPaths = new ArrayList<>();
        for (String globPattern : globPaths) {
            this.log.infoFirstLevel("Processing: " + globPattern);
            if (globPattern.endsWith(Glob.DOUBLE_STAR)) {
                globPattern += "/*";
            }
            String docFileExtension = ".{" + String.join(",", conf.getDocExtensions()) + "}";
            if (!globPattern.contains(".")) {
                globPattern = globPattern + docFileExtension;
            }
            GlobPath globPathObject = new GlobPath(globPattern);
            Path docPath = this.conf.getSearchDocPath();
            List<Path> paths = Fs.getFilesByGlob(docPath, globPathObject)
                    .stream()
                    // Natural Order
                    // [1-one, 2-two, 3-three, 10-ten, 20-twenty, 100-hundred]
                    .sorted((x, y) -> Sorts.naturalSortComparator(x.toString(), y.toString()))
                    .collect(Collectors.toList());
            if (paths.isEmpty()) {
                throw new RuntimeException("No docs selected for the glob (" + globPattern + ") with the doc path (" + docPath + ")");
            }
            totalPaths.addAll(paths);
        }
        return totalPaths;
    }

    public DocExecutorUnit getDocExecutorUnit() {
        return this.docExecutorUnit;
    }

    public DocExecutor getConf() {
        return this.conf;
    }

    public DocLog getLog() {
        return this.log;
    }

    public DocExecutorResultStore getResults() {

        return results;
    }
}
