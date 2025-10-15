package com.combostrap.docExec;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of a run executed on a file
 * via {@link DocExecutorInstance#run(Path...)}
 */
public class DocExecutorResultDocExecution {


    private final Path path;
    @JsonIgnore
    private String newDoc;
    private int error = 0;
    // Indicate if the doc has been executed
    private boolean docHasBeenExecuted = false;
    private boolean cacheHit = false;
    private int codeExecutionCounter = 0;

    @JsonIgnore
    private List<String> warnings = new ArrayList<>();


    private DocExecutorResultDocExecution(Path path) {
        this.path = path;
    }

    public static DocExecutorResultDocExecution get(Path path) {
        return new DocExecutorResultDocExecution(path);
    }

    public String getNewDoc() {
        return this.newDoc;
    }

    public void setNewDoc(String doc) {
        this.newDoc = doc;
    }

    public void addError() {
        this.error++;
    }

    public int getErrors() {
        return this.error;
    }

    public boolean hasRun() {
        return this.docHasBeenExecuted;
    }

    public DocExecutorResultDocExecution setHasBeenExecuted(boolean b) {
        this.docHasBeenExecuted = b;
        return this;
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

    public DocExecutorResultDocExecution setCacheHit(boolean b) {
        this.cacheHit = b;
        return this;
    }

    public Path getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
