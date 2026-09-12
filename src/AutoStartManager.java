import java.io.File;

public class AutoStartManager {

    private static final String REG_KEY = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "JDClear";

    private static String getExecutablePath() {
        String exe = System.getenv("JDCLEAR_EXE");
        if (exe != null && !exe.isEmpty()) {
            return exe;
        }
        String javaHome = System.getProperty("java.home");
        String javaw = javaHome + File.separator + "bin" + File.separator + "javaw.exe";
        String classPath = System.getProperty("java.class.path");
        String mainClass = "CleanerUI";
        return "\"" + javaw + "\" -cp \"" + classPath + "\" " + mainClass;
    }

    public static boolean isAutoStartEnabled() {
        try {
            Process p = new ProcessBuilder("reg", "query", REG_KEY, "/v", APP_NAME)
                    .redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean enableAutoStart() {
        try {
            String exePath = getExecutablePath();
            Process p = new ProcessBuilder(
                    "reg", "add", REG_KEY, "/v", APP_NAME,
                    "/t", "REG_SZ", "/d", exePath, "/f"
            ).redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean disableAutoStart() {
        try {
            Process p = new ProcessBuilder(
                    "reg", "delete", REG_KEY, "/v", APP_NAME, "/f"
            ).redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean setAutoStart(boolean enable) {
        return enable ? enableAutoStart() : disableAutoStart();
    }
}