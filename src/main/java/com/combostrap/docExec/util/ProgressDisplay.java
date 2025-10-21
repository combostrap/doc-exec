package com.combostrap.docExec.util;

import com.combostrap.docExec.DocLog;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A progress component
 * <p>
 * terminal based gui needs an async (ie thread)
 * and to call the {@link #waitingToClose()}
 * terminal-based progress display component using Lanterna.
 * Shows a progress bar and the last 10 lines of status text.
 */
public class ProgressDisplay {

    private final int totalCount;
    private int currentCount = 0;
    private final List<String> statusLines = new ArrayList<>();
    private final int maxStatusLines = 10;

    private Terminal terminal;
    private Screen screen;
    private WindowBasedTextGUI textGUI;
    private BasicWindow window;
    private ProgressBar progressBar;
    private Panel statusPanel;

    private final CountDownLatch latch = new CountDownLatch(1);
    /**
     * Multi-thread safety
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new ProgressDisplay with the specified total count.
     *
     * @param totalCount the total number of items to process
     */
    public ProgressDisplay(int totalCount, boolean tui) {
        this.totalCount = totalCount;

        /**
         * TUI is by default false
         * because you got a lot of derived problem
         * Such as what to do when an error occurs
         * As you get a screen. We wanted to show 10 lines only
         * May be without screen is it easier better, no time to explore further
         */
        if (tui) {
            try {
                initializeTerminal();
            } catch (IOException e) {
                // no textual terminal (ie no new File("/dev/tty") in Unix
            }
        }

    }

    /**
     * <a href="https://github.com/mabe02/lanterna/blob/master/docs/using-gui.md">...</a>
     */
    private void initializeTerminal() throws IOException {


        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory(System.out, System.in, StandardCharsets.UTF_8);
        defaultTerminalFactory.setForceTextTerminal(true);
        terminal = defaultTerminalFactory.createTerminal();
        /**
         * https://github.com/mabe02/lanterna/blob/master/docs/using-terminal.md#entering-and-exiting-private-mode
         * In this mode the screen is cleared, scrolling is disabled and the previous content is stored away.
         */
        // terminal.enterPrivateMode();

        /**
         * Screen is a model to redraw only what has changed
         * https://github.com/mabe02/lanterna/blob/master/docs/using-screen.md
         */
        screen = new TerminalScreen(terminal);
        // StartScreen Error: Cannot call enterPrivateMode() when already in private mode
        screen.startScreen();
        textGUI = new MultiWindowTextGUI(screen);
        createUI();


    }

    private void createUI() {


        // Progress bar
        progressBar = new ProgressBar(0, totalCount, 50);
        progressBar.setValue(0);


        // Status panel for text lines
        statusPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        statusPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        statusPanel.setPreferredSize(new TerminalSize(80, maxStatusLines));
        // Add border around the panel
        statusPanel.setLayoutData(
                LinearLayout.createLayoutData(
                        LinearLayout.Alignment.Center
                )
        );
        statusPanel.addComponent(new Label("Status"));

        /**
         * Parent
         */
        Panel mainPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        mainPanel.addComponent(new Label("Progress:"));
        mainPanel.addComponent(progressBar);

        // Status label
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        mainPanel.addComponent(new Label("Status:"));
        mainPanel.addComponent(statusPanel);

        Button button = new Button("Enter", new Runnable() {
            @Override
            public void run() {
                window.close();
            }
        });
        mainPanel.addComponent(button);

        window = new BasicWindow("Progress");
        window.setHints(List.of(Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));
        window.setComponent(mainPanel);
        textGUI.addWindow(window);
    }

    /**
     * Adds a status line and increments the progress counter.
     * The display will refresh automatically.
     *
     * @param statusLine the status text to add
     */
    protected void addExecutionStatus(String statusLine, boolean pageExecuted) {


        lock.lock();
        try {


            /**
             * No textual terminal
             */
            if (window == null) {
                DocLog.LOGGER.info((currentCount + 1) + "/" + totalCount + ": " + statusLine);

                // Increment progress
                if (pageExecuted) {
                    currentCount++;
                }

                if (currentCount == totalCount) {
                    // Release waiting threads
                    latch.countDown();
                }

                return;
            }

            // Increment progress
            if (pageExecuted) {
                currentCount++;
            }

            // Add new status line
            statusLines.add(statusLine);

            // Keep only the last 10 lines
            if (statusLines.size() > maxStatusLines) {
                statusLines.remove(0);
            }

            // Update UI
            updateTuiDisplay();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add a status line and increment the execution counter
     */
    public void addExecutionClosingStatus(String statusLine) {
        addExecutionStatus(statusLine, true);
    }

    /**
     * Add a status line without incrementing the counter
     */
    public void addExecutionStatus(String statusLine) {
        addExecutionStatus(statusLine, false);
    }

    private void updateTuiDisplay() {
        // Update progress bar
        progressBar.setValue(currentCount);

        // Clear and update status panel
        statusPanel.removeAllComponents();

        for (String line : statusLines) {
            Label statusLabel = new Label(line);
            statusLabel.setForegroundColor(TextColor.ANSI.BLACK);
            statusPanel.addComponent(statusLabel);
        }

        // Fill remaining space with empty labels to maintain consistent height
        int remainingLines = maxStatusLines - statusLines.size();
        for (int i = 0; i < remainingLines; i++) {
            statusPanel.addComponent(new Label(""));
        }


        // Refresh the screen
        try {
            textGUI.updateScreen();
        } catch (IOException e) {
            // Handle silently or log if needed
        }
        // Wait for refresh to complete (synchronous)
        try {
            /**
             * https://github.com/mabe02/lanterna/blob/master/docs/using-screen.md#clearing-the-screen
             */
            screen.clear();
            // COMPLETE: Full refresh (redraws everything)
            // DELTA: refresh (only changed parts) - faster
            // AUTOMATIC: lets Lanterna decide
            screen.refresh(Screen.RefreshType.COMPLETE);
            // https://github.com/mabe02/lanterna/blob/master/docs/using-terminal.md#flushing
            // To be sure that the text has been sent to the client terminal, you should call the flush() method on the Terminal interface when you have done all your operations.
            terminal.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Gets the current progress count.
     *
     * @return the current progress count
     */
    public int getCurrentCount() {
        return currentCount;
    }

    /**
     * Gets the total count.
     *
     * @return the total count
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Checks if the progress is complete.
     *
     * @return true if current count equals or exceeds total count
     */
    public boolean isComplete() {
        return currentCount >= totalCount;
    }

    /**
     * Closes the progress display and cleans up terminal resources.
     * Call this when done with the progress display.
     */
    public void close() {

        try {

            if (window != null) {
                window.close();
            }
            if (screen != null) {
                // https://github.com/mabe02/lanterna/blob/master/docs/using-terminal.md#entering-and-exiting-private-mode
                // terminal.exitPrivateMode();
                screen.stopScreen();
            }
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException e) {
            // Handle silently or log if needed
        }
    }

    /**
     * Shows a completion message and waits for user input before closing.
     *
     * @param message the completion message to show
     */
    public void showCompletionMessage(String message) {
        new MessageDialogBuilder()
                .setTitle("Complete")
                .setText(message)
                .build()
                .showDialog(textGUI);
        this.close();
    }

    /**
     * The waiting to close function
     */
    @SuppressWarnings("unused")
    public void waitingToClose() {

        if (textGUI != null) {
            textGUI.waitForWindowToClose(window);
            this.close();
            return;
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
}
