package com.combostrap.docExec;


import java.util.logging.*;

public class DocLog {

    public final static Logger LOGGER = Logger.getLogger(DocLog.class.getPackageName());


    public static String onOneLine(String string) {
        return string.replaceAll("\r\n|\n", " ") // No new line
                .replaceAll(" ", " ");
    }

    /**
     * Init log
     */
    public static DocLog build(DocExecutor docExecutor) {

        String packageName = DocExecutorInstance.class.getPackage().getName();
        Logger logger = Logger.getLogger(packageName);
        logger.setLevel(docExecutor.getLogLevel());

        /**
         * If no handlers were defined
         */
        if (logger.getHandlers().length == 0) {
            /**
             * JUL has a cascading feature
             * so the handler for the parent will also produce
             * output. We get therefore 2 times the output
             * We disable it.
             */
            logger.setUseParentHandlers(false);
            /**
             * {@link ConsoleHandler} sends to `std.err` and you can't modify it
             * You need to use a {@link StreamHandler}
             */
            StreamHandler infoHandler = new StreamHandler(
                    System.out,
                    new java.util.logging.Formatter() {
                        @Override
                        public String format(LogRecord record) {
                            return record.getMessage() + "\n";
                        }
                    });
            infoHandler.setLevel(Level.INFO);
            logger.addHandler(infoHandler);
            /**
             * Handler for all other level
             */
            StreamHandler otherLevelHandler = new StreamHandler(
                    System.err,
                    new java.util.logging.Formatter() {
                        @Override
                        public String format(LogRecord record) {
                            return record.getMessage() + "\n";
                        }
                    });
            otherLevelHandler.setFilter(record -> record.getLevel() != Level.INFO);
            otherLevelHandler.setLevel(Level.ALL);
            logger.addHandler(otherLevelHandler);
        }
        return new DocLog();
    }

    /**
     * Page Execution Info
     */
    public void infoSecondLevel(String s) {
        LOGGER.info("   - " + s);
    }

    public void severe(String s) {
        LOGGER.severe(s);
    }

    /**
     * Page Info
     */
    public void infoFirstLevel(String s) {
        LOGGER.info(s);
    }

    public void fine(String s) {
        LOGGER.fine(s);
    }
}
