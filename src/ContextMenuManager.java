import java.io.File;

/**
 * Windows 右键菜单集成（写 HKCU\Software\Classes，不需要管理员权限）
 */
public class ContextMenuManager {

    private static final String KEY_DIR = "HKEY_CURRENT_USER\\Software\\Classes\\Directory\\shell\\JDClear";
    private static final String KEY_BG = "HKEY_CURRENT_USER\\Software\\Classes\\Directory\\Background\\shell\\JDClear";

    /** 获取当前程序启动命令 */
    private static String getLaunchCommand() {
        // 打包后优先用 exe
        String exe = System.getenv("JDCLEAR_EXE");
        if (exe != null && !exe.isEmpty()) {
            return "\"" + exe + "\" \"%1\"";
        }

        // 开发阶段：javaw -cp "classpath" CleanerUI "%1"
        String javaHome = System.getProperty("java.home");
        String javaw = javaHome + File.separator + "bin" + File.separator + "javaw.exe";
        String classPath = System.getProperty("java.class.path");
        return "\"" + javaw + "\" -cp \"" + classPath + "\" CleanerUI \"%1\"";
    }

    /** 检查是否已集成 */
    public static boolean isIntegrated() {
        try {
            Process p = new ProcessBuilder("reg", "query", KEY_DIR)
                    .redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 启用右键菜单 */
    public static boolean enable() {
        try {
            String cmd = getLaunchCommand();

            // Directory\shell\JDClear
            new ProcessBuilder("reg", "add", KEY_DIR, "/ve", "/t", "REG_SZ", "/d", "用 JDClear 清理", "/f")
                    .redirectErrorStream(true).start().waitFor();
            new ProcessBuilder("reg", "add", KEY_DIR, "/v", "Icon", "/t", "REG_SZ", "/d", "shell32.dll,131", "/f")
                    .redirectErrorStream(true).start().waitFor();
            new ProcessBuilder("reg", "add", KEY_DIR + "\\command", "/ve", "/t", "REG_SZ", "/d", cmd, "/f")
                    .redirectErrorStream(true).start().waitFor();

            // Directory\Background\shell\JDClear
            new ProcessBuilder("reg", "add", KEY_BG, "/ve", "/t", "REG_SZ", "/d", "用 JDClear 清理", "/f")
                    .redirectErrorStream(true).start().waitFor();
            new ProcessBuilder("reg", "add", KEY_BG, "/v", "Icon", "/t", "REG_SZ", "/d", "shell32.dll,131", "/f")
                    .redirectErrorStream(true).start().waitFor();
            new ProcessBuilder("reg", "add", KEY_BG + "\\command", "/ve", "/t", "REG_SZ", "/d", cmd, "/f")
                    .redirectErrorStream(true).start().waitFor();

            return true;
        } catch (Exception e) {
            System.err.println("启用右键菜单失败：" + e.getMessage());
            return false;
        }
    }

    /** 禁用右键菜单 */
    public static boolean disable() {
        try {
            new ProcessBuilder("reg", "delete", KEY_DIR, "/f")
                    .redirectErrorStream(true).start().waitFor();
            new ProcessBuilder("reg", "delete", KEY_BG, "/f")
                    .redirectErrorStream(true).start().waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 设置 */
    public static boolean setEnabled(boolean enabled) {
        return enabled ? enable() : disable();
    }
}