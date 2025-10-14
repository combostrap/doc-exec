package com.combostrap.docExec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of a run executed on a file
 * via {@link DocExecutor#run(Path...)}
 */
public class DocExecutorResult {


    private final Path path;
    private String doc;
    private int error = 0;
    // Indicate if the doc has been executed
    private boolean docHasBeenExecuted = false;
    private boolean cacheHit = false;
    private int codeExecutionCounter = 0;
    private List<String> warnings = new ArrayList<>();


    private DocExecutorResult(Path path) {
        this.path = path;
    }

    public static DocExecutorResult get(Path path) {
        return new DocExecutorResult(path);
    }

    public String getNewDoc() {
        return this.doc;
    }

    public void setNewDoc(String doc) {
        this.doc = doc;
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

    public DocExecutorResult setHasBeenExecuted(boolean b) {
        this.docHasBeenExecuted = b;
        return this;
    }

    public int getCodeExecution() {
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

    public DocExecutorResult setCacheHit(boolean b) {
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
