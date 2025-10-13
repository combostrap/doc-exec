package com.combostrap.docExec;

/**
 * An exit status exception that is created
 * during unit code execution so that DocExecutor can choose what to do with it
 */
public class DocExitStatusException extends RuntimeException {

    private final int exitStatus;

    public DocExitStatusException(int exitStatus) {
        super();
        this.exitStatus = exitStatus;
    }

    public int getExitStatus() {
        return exitStatus;
    }

}
