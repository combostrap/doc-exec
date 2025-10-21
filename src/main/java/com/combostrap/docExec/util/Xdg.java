package com.combostrap.docExec.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cross implementation of
 * <a href="https://specifications.freedesktop.org/basedir-spec/latest/">...</a>
 */
public class Xdg {


    /**
     * @param appName - the app name
     * @return XDG_DATA_HOME
     * where user-specific data files should be stored.
     */
    public static Path getDataHome(String appName) {

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows
            String appData = System.getenv("LOCALAPPDATA");
            if (appData == null) {
                appData = System.getProperty("user.home") + "\\AppData\\Local";
            }
            return Paths.get(appData, appName);

        }
        if (os.contains("mac")) {
            // macOS
            return Paths.get(System.getProperty("user.home"), "Library", appName);
        }


        // Linux/Unix
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome == null) {
            xdgDataHome = System.getProperty("user.home") + "/.local/share";
        }
        return Paths.get(xdgDataHome, appName);


    }

    /**
     * Runtime-only, user-specific files that need to exist only while the user is logged in.
     * Appropriate Uses
     * * Unix sockets - IPC (inter-process communication) sockets
     * * Named pipes (FIFOs) - For process communication
     * * PID files - Process identifiers for running instances
     * * Temporary lock files - Session-scoped locks
     * * Short-lived session data - Data that's only relevant during the current login session
     *
     * @return the runtime directory or the temporary
     */
    public static Path getRuntimeUserDir(String appName) {
        String xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");
        if (xdgRuntimeDir == null) {
            return Fs.getTempDirectory().resolve(appName);
        }
        return Paths.get(xdgRuntimeDir).resolve(appName);
    }

    /**
     * Characteristics:
     * * Data here can be deleted without breaking your app
     * * System cleaners may purge these directories
     * * Not included in backups (generally)
     * * Should be regenerable from source data
     */
    public static Path getCacheHome(String appName) {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows
            String appData = System.getenv("LOCALAPPDATA");
            if (appData == null) {
                appData = System.getProperty("user.home") + "\\AppData\\Local";
            }
            return Paths.get(appData, appName, "Cache");

        }
        if (os.contains("mac")) {
            // macOS
            return Paths.get(System.getProperty("user.home"), "Library", "Caches", appName);
        }


        // Linux/Unix
        String xdgDataHome = System.getenv("XDG_CACHE_HOME");
        if (xdgDataHome == null) {
            xdgDataHome = System.getProperty("user.home") + "/.cache";
        }
        return Paths.get(xdgDataHome, appName);
    }
}
