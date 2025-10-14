package com.combostrap.docExec;

public class DocFirstError extends RuntimeException {
    public DocFirstError(String stopAtFirstError, Exception e) {
        super(stopAtFirstError, e);
    }
}
