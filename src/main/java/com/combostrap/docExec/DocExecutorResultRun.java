package com.combostrap.docExec;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of a run
 */
public class DocExecutorResultRun {


    private final LocalDateTime startTime;
    private final DocExecutorInstance docExecutorInstance;
    List<DocExecutorResultDocExecution> results = new ArrayList<>();

    public DocExecutorResultRun(DocExecutorInstance docExecutorInstance) {
        this.docExecutorInstance = docExecutorInstance;
        startTime = java.time.LocalDateTime.now();
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
        Path normalize;
        try {
            normalize = this.docExecutorInstance.getDocExecutor().getSearchDocPath().relativize(childPath);
        } catch (Exception e) {
            // Maybe a file passed directly and not from a glob pattern
            normalize = childPath;
        }
        DocExecutorResultDocExecution result = DocExecutorResultDocExecution
                .get(normalize);
        this.results.add(result);
        return result;
    }
}
