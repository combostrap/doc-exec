package com.combostrap.docExec;


import com.combostrap.docExec.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Files;
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

    private final DocExecutor docExecutor;
    private final String eol = System.lineSeparator();

    @JsonIgnore
    private final DocLog log;
    @JsonIgnore
    private final DocExecutorUnit docExecutorUnit;
    private final DocCache cache;
    private final DocExecutorResultStore results;

    public DocExecutorInstance(DocExecutor docExecutor) {
        this.docExecutor = docExecutor;
        /**
         * Log Init
         */
        this.log = DocLog.build(docExecutor);
        /**
         * The unit runner
         */
        this.docExecutorUnit = DocExecutorUnit.create(this);
        /**
         * The result store
         */
        results = new DocExecutorResultStore(this);
        if (this.docExecutor.getIsCacheEnabled()) {
            this.cache = DocCache.get(this.docExecutor.getName());
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
        DocExecutorResultRun docExecutorResultRun = new DocExecutorResultRun(this, paths.length);
        try {

            DocCache docCache = this.cache;
            if (docCache != null && docExecutor.getPurgeCache()) {
                docCache.purgeAll();
            }

            for (Path path : paths) {

                try (DocExecutorResultDocExecution docResult = docExecutorResultRun.createResultForDoc(path)) {
                    /**
                     * We skip at execution, not at selection so that
                     * we get the messages in order of execution
                     */
                    Path resumeFrom = this.docExecutor.getResumeFromPath();
                    if (resumeFrom != null && Sorts.naturalSortComparator(resumeFrom.toString(), path.toString()) > 0) {
                        docResult.setSkippedStatus();
                        continue;
                    }

                    if (!Files.exists(path)) {
                        String msg = "The path (" + path.toAbsolutePath() + ") does not exist";
                        this.log.severe(msg);
                        RuntimeException exception = new RuntimeException(msg);
                        docResult.setErrorStatus(exception);
                        if (this.docExecutor.doesStopAtFirstError()) {
                            throw new DocFirstErrorOrWarning(exception);
                        }
                        continue;
                    }


                    /**
                     * Cache ?
                     */
                    if (docCache != null) {
                        String md5Cache = docCache.getMd5(path);
                        String md5 = Fs.getMd5(path);
                        if (md5.equals(md5Cache)) {
                            docResult.setCacheHitStatus();
                            continue;
                        }
                    }

                    /**
                     * Execution
                     */
                    DocExecutorResultDocExecution docExecutorResultDocExecution = this.execute(docResult, path);
                    if (!docExecutor.getIsDryRun()) {
                        // Overwrite the new doc
                        Fs.toFile(docExecutorResultDocExecution.getNewDoc(), path);
                    }

                    if (docCache != null) {
                        docCache.store(path);
                    }

                    if (docExecutorResultDocExecution.hasWarnings()) {
                        for (String warning : docExecutorResultDocExecution.getWarnings()) {
                            System.err.println("Warning: " + warning);
                        }
                        if (this.docExecutor.getStopAtFirstErrorOrWarning()) {
                            DocWarning e = new DocWarning("Warning were seen");
                            throw new DocFirstErrorOrWarning(e);
                        }
                    }
                }

            }
            return docExecutorResultRun;
        } finally {
            docExecutorResultRun.close();
        }

    }

    /**
     * @param path the doc to execute
     * @return the new page
     */
    private DocExecutorResultDocExecution execute(DocExecutorResultDocExecution docExecutorResultDocExecution, Path path) {


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

            docExecutorResultDocExecution.logInfo("Processing doc-exec node " + (i + 1));
            try {
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

                        docExecutorResultDocExecution.logFine("Replacing the file block");
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
                        docExecutorResultDocExecution.logInfo("Running the code (" + Strings.onOneLine(code) + ")");
                        docExecutorResultDocExecution.incrementExecutionCount();
                        result = docExecutorUnit.run(docUnit);
                        docExecutorResultDocExecution.logInfo("Code executed successfully");
                        oneCodeBlockHasAlreadyRun = true;
                    } else {
                        docExecutorResultDocExecution.logInfo("The run of the code (" + Strings.onOneLine(code) + ") was skipped due to caching");
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
                        // The output does not have the EOL, so the console should not
                        // <console>
                        //   result
                        // </console>
                        console = console.trim();
                        if (docExecutor.getTrimLeadingAndTrailingLines()) {
                            result = result.trim();
                        }
                        if (!result.equals(console)) {

                            int resultLineCount = Strings.getLineCount(result.trim());
                            int actualConsoleLineCount = Strings.getLineCount(console);
                            if (resultLineCount < actualConsoleLineCount && docExecutor.isContentShrinkingWarning()) {
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
            } catch (Exception e) {
                docExecutorResultDocExecution.setErrorStatus(e);
                if (docExecutor.doesStopAtFirstError()) {
                    throw new DocFirstErrorOrWarning(e);
                }
                String result;
                if (e.getClass().equals(NullPointerException.class)) {
                    result = "null pointer exception";
                } else {
                    result = e.getMessage();
                }
                docExecutorResultDocExecution.logSevere("Error during execute: " + result);
            }
        }
        targetDoc.append(originalDoc, previousEnd, originalDoc.length());
        docExecutorResultDocExecution
                .setNewDoc(targetDoc.toString())
                .setSuccessfulStatus();
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
        for (Path searchFile : docExecutor.getSearchFilePaths()) {
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
            this.log.infoFirstLevel("Processing the glob: " + globPattern);
            if (globPattern.endsWith(Glob.DOUBLE_STAR)) {
                globPattern += "/*";
            }
            String docFileExtension = ".{" + String.join(",", docExecutor.getDocExtensions()) + "}";
            if (!globPattern.contains(".")) {
                globPattern = globPattern + docFileExtension;
            }
            GlobPath globPathObject = new GlobPath(globPattern);
            Path docPath = this.docExecutor.getSearchDocPath();
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

    @JsonProperty("conf")
    public DocExecutor getDocExecutor() {
        return this.docExecutor;
    }

    public DocLog getLog() {
        return this.log;
    }

    @JsonProperty("results")
    public DocExecutorResultStore getResultStore() {
        return results;
    }
}
