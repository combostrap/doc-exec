package com.combostrap.docExec;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of a run
 */
public class DocExecutorResultRun implements AutoCloseable {


    private final LocalDateTime startTime;
    private final DocExecutorInstance docExecutorInstance;
    private final int runSize;
    List<DocExecutorResultDocExecution> results = new ArrayList<>();
    private DocExecutorResultDocExecution actualDocExecutionResult;

    public DocExecutorResultRun(DocExecutorInstance docExecutorInstance, int runSize) {
        this.docExecutorInstance = docExecutorInstance;
        startTime = java.time.LocalDateTime.now();
        this.runSize = runSize;
        DocLog.LOGGER.info("Starting execution of " + runSize + " docs");
    }


    public List<DocExecutorResultDocExecution> getDocExecutionResults() {
        return results;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public DocExecutorInstance getDocExecutorInstance() {
        return docExecutorInstance;
    }

    public DocExecutorResultDocExecution createResultForDoc(Path childPath) {
        if (actualDocExecutionResult != null && !actualDocExecutionResult.isClosed()) {
            throw new RuntimeException("Internal error, the previous execution is still open.");
        }
        Path normalize;
        try {
            normalize = this.docExecutorInstance.getConf().getSearchDocPath().relativize(childPath);
        } catch (Exception e) {
            // Maybe a file passed directly and not from a glob pattern
            // so not relative
            normalize = childPath;
        }
        DocExecutorResultDocExecution result = new DocExecutorResultDocExecution(this, this.results.size() + 1, normalize);
        this.results.add(result);
        this.actualDocExecutionResult = result;
        return result;
    }

    public Integer getRunSize() {
        return this.runSize;
    }

    public void close() {
        DocLog.LOGGER.info("Execution finished. " + this.results.size() + " docs were executed");
    }
}
