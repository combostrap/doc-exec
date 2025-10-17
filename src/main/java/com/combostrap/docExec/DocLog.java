package com.combostrap.docExec;


import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
             * We use the same stderr for all level so that they are in the same buffer
             * We flush also instantly because we use it as user feedback
             */
            ConsoleHandler docExecHandler = new ConsoleHandler() {

                private final Formatter formatter = new Formatter() {
                    @Override
                    public String format(LogRecord record) {

                        String message = String.format("%-7s - %s%n",
                                record.getLevel(),
                                record.getMessage()
                        );
                        /**
                         * Adding stack trace if any error
                         */
                        if (record.getThrown() != null) {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            record.getThrown().printStackTrace(pw);
                            message += sw + System.lineSeparator();
                        }
                        return message;
                    }
                };

                @Override
                public synchronized void publish(LogRecord record) {
                    super.publish(record);
                    // no buffer
                    flush();
                }

                @Override
                public Formatter getFormatter() {
                    return formatter;
                }
            };

            logger.addHandler(docExecHandler);
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
