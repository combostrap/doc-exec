package com.combostrap.docExec;

public class DocFirstErrorOrWarning extends RuntimeException {


    public static final String STOP_AT_FIRST_ERROR = "Stop at first error or warning";

    public DocFirstErrorOrWarning(Exception e) {
        /**
         * The message can be huge if the error adds a usage
         * We don't add e.getMessage() as message
         */
        super(STOP_AT_FIRST_ERROR, e);
    }
}
