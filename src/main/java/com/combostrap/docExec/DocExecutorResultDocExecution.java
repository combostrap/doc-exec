package com.combostrap.docExec;

import com.combostrap.docExec.util.Timer;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of a run executed on a file
 * via {@link DocExecutorInstance#run(Path...)}
 */
public class DocExecutorResultDocExecution implements AutoCloseable {


    private final Path path;
    private final Timer timer;
    @JsonIgnore
    private String newDoc;
    private int error = 0;
    // Indicate if the doc has been executed
    private boolean cacheHit = false;
    private int codeExecutionCounter = 0;

    @JsonIgnore
    private final List<String> warnings = new ArrayList<>();
    private boolean skipped = false;
    private Integer exitCode = null;

    @JsonIgnore
    private final String logPrefix;


    /**
     * @param run      - the run
     * @param runIndex - the index (counter)
     * @param path     - the doc path
     */
    DocExecutorResultDocExecution(DocExecutorResultRun run, int runIndex, Path path) {
        this.path = path;
        this.timer = Timer.create(path.toString()).start();
        this.logPrefix = runIndex + "/" + run.getRunSize() + " : " + path + " : ";
        logInfo("Starting execution");
    }


    public String getNewDoc() {
        return this.newDoc;
    }

    public DocExecutorResultDocExecution setNewDoc(String doc) {
        this.newDoc = doc;
        return this;
    }

    public void addError() {
        this.error++;
    }

    public int getErrors() {
        return this.error;
    }


    public int getCodeExecutionCounter() {
        return this.codeExecutionCounter;
    }

    public void incrementCodeExecutionCounter() {
        this.codeExecutionCounter++;
    }

    public void addWarning(String s) {
        this.warnings.add(s);
    }

    public boolean hasWarnings() {
        return !this.warnings.isEmpty();
    }

    public List<String> getWarnings() {
        return this.warnings;
    }

    public void setCacheHitStatus() {
        this.cacheHit = true;
        this.exitCode = -1;
        logInfo( "Cache hit. Skipping execution.");
    }

    public Path getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return path.toString();
    }

    public boolean isClosed() {
        return this.timer.hasStopped();
    }

    public void close() {
        if (this.exitCode == null) {
            throw new RuntimeException("The exit code is null");
        }
        if (this.exitCode == 0) {
            logInfo("Doc executed successfully");
        }
        this.timer.stop();
    }

    public void setSkippedStatus() {
        this.skipped = true;
        this.exitCode = -1;
        logInfo("ResumeFrom is on. Skipping");
    }

    public boolean hasRun() {
        return !this.cacheHit && !this.skipped;
    }

    public void setSuccessfulStatus() {
        this.exitCode = 0;
    }

    public void logInfo(String s) {

        DocLog.LOGGER.info(logPrefix + s);
    }

    public void logFine(String s) {
        DocLog.LOGGER.fine(logPrefix + s);
    }

    public void logSevere(String s) {
        DocLog.LOGGER.severe(logPrefix + s);
    }

    public boolean wasSkipped() {
        return this.skipped;
    }

    public Integer getExitStatus() {
        return this.exitCode;
    }
}
