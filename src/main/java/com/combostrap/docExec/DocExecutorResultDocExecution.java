package com.combostrap.docExec;

import com.combostrap.docExec.util.Timer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of a run executed on a file
 * via {@link DocExecutorInstance#run(Path...)}
 * Note: all getters are persisted as result in the json file
 */
public class DocExecutorResultDocExecution implements AutoCloseable {


    private final Path path;
    private final Timer timer;
    @JsonIgnore
    private String newDoc;
    private int errorCount = 0;
    // Indicate if the doc has been executed
    private boolean cacheHit = false;
    private int executionCount = 0;

    @JsonIgnore
    private final List<String> warnings = new ArrayList<>();
    private boolean skipped = false;
    /**
     * Null so that the persistence will fail if it's still null
     */
    private Integer exitCode = null;

    @JsonIgnore
    private final String logPrefix;

    @JsonIgnore
    private Exception exception;


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


    @JsonProperty
    public int getErrorCount() {
        return this.errorCount;
    }


    @JsonProperty
    public int getExecutionCount() {
        return this.executionCount;
    }

    public void incrementExecutionCount() {
        this.executionCount++;
    }

    public void addWarning(String s) {
        this.warnings.add(s);
    }

    @JsonProperty
    public int getWarningCount() {
        return this.warnings.size();
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
        logInfo("Cache hit. Skipping execution.");
    }

    @JsonProperty
    public Path getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return path.toString();
    }

    @JsonIgnore
    public boolean isClosed() {
        return this.timer.hasStopped();
    }

    @JsonProperty
    public long getDurationMs() {
        return this.timer.getDuration().toMillis();
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

    @JsonIgnore
    public boolean hasRun() {
        return !this.cacheHit && !this.skipped;
    }

    public void setSuccessfulStatus() {
        this.exitCode = 0;
    }

    @JsonProperty
    public String getStatus() {
        if (this.cacheHit) {
            return "CacheHit";
        }
        if (this.skipped) {
            return "Skipped";
        }
        if (this.exitCode == 0) {
            return "Success";
        }
        return "Failure";
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

    @JsonIgnore
    public boolean wasSkipped() {
        return this.skipped;
    }

    @JsonProperty
    public Integer getExitStatus() {
        return this.exitCode;
    }

    public void setErrorStatus(Exception exception) {
        this.exception = exception;
        this.errorCount++;
        this.exitCode = 1;
        logSevere(exception.toString());
    }

}
