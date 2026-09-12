import java.io.*;
import java.util.Properties;

public class AppConfig {

    private static final String CONFIG_FILE = System.getProperty("user.dir")
            + File.separator + "config.properties";

    public static final String KEY_BACKUP_DIR = "backup.dir";
    public static final String KEY_LOG_DIR = "log.dir";
    public static final String KEY_DEFAULT_MODE = "default.mode";
    public static final String KEY_DEFAULT_LANGUAGE = "default.language";
    public static final String KEY_SIMULATE_DEFAULT = "simulate.default";
    public static final String KEY_FORCE_DELETE_DEFAULT = "forceDelete.default";
    public static final String KEY_AUTO_START = "autoStart";
    public static final String KEY_AUTO_CLEAN_ENABLED = "autoClean.enabled";
    public static final String KEY_AUTO_CLEAN_INTERVAL = "autoClean.interval";
    public static final String KEY_AUTO_CLEAN_LAST_TIME = "autoClean.lastTime";
    public static final String KEY_CONTEXT_MENU = "contextMenu.enabled";

    public static final String DEFAULT_BACKUP_DIR = System.getProperty("user.dir") + File.separator + "backup";
    public static final String DEFAULT_LOG_DIR = System.getProperty("user.dir") + File.separator + "log";
    public static final String DEFAULT_MODE = "recycleBin";
    public static final String DEFAULT_LANGUAGE = "auto";
    public static final boolean DEFAULT_SIMULATE = false;
    public static final boolean DEFAULT_FORCE_DELETE = false;
    public static final boolean DEFAULT_AUTO_START = false;
    public static final boolean DEFAULT_AUTO_CLEAN_ENABLED = false;
    public static final String DEFAULT_AUTO_CLEAN_INTERVAL = "daily";
    public static final long DEFAULT_AUTO_CLEAN_LAST_TIME = 0L;
    public static final boolean DEFAULT_CONTEXT_MENU = false;

    private Properties props = new Properties();

    public AppConfig() {
        load();
    }

    public void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("加载配置失败：" + e.getMessage());
            }
        }
    }

    public void save() {
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "JDClear Configuration");
        } catch (IOException e) {
            System.err.println("保存配置失败：" + e.getMessage());
        }
    }

    public String getBackupDir() { return props.getProperty(KEY_BACKUP_DIR, DEFAULT_BACKUP_DIR); }
    public void setBackupDir(String dir) { props.setProperty(KEY_BACKUP_DIR, dir); }

    public String getLogDir() { return props.getProperty(KEY_LOG_DIR, DEFAULT_LOG_DIR); }
    public void setLogDir(String dir) { props.setProperty(KEY_LOG_DIR, dir); }

    public String getDefaultMode() { return props.getProperty(KEY_DEFAULT_MODE, DEFAULT_MODE); }
    public void setDefaultMode(String mode) { props.setProperty(KEY_DEFAULT_MODE, mode); }

    public String getDefaultLanguage() { return props.getProperty(KEY_DEFAULT_LANGUAGE, DEFAULT_LANGUAGE); }
    public void setDefaultLanguage(String lang) { props.setProperty(KEY_DEFAULT_LANGUAGE, lang); }

    public boolean getSimulateDefault() {
        return Boolean.parseBoolean(props.getProperty(KEY_SIMULATE_DEFAULT, String.valueOf(DEFAULT_SIMULATE)));
    }
    public void setSimulateDefault(boolean value) { props.setProperty(KEY_SIMULATE_DEFAULT, String.valueOf(value)); }

    public boolean getForceDeleteDefault() {
        return Boolean.parseBoolean(props.getProperty(KEY_FORCE_DELETE_DEFAULT, String.valueOf(DEFAULT_FORCE_DELETE)));
    }
    public void setForceDeleteDefault(boolean value) { props.setProperty(KEY_FORCE_DELETE_DEFAULT, String.valueOf(value)); }

    public boolean getAutoStart() {
        return Boolean.parseBoolean(props.getProperty(KEY_AUTO_START, String.valueOf(DEFAULT_AUTO_START)));
    }
    public void setAutoStart(boolean value) { props.setProperty(KEY_AUTO_START, String.valueOf(value)); }

    public boolean getAutoCleanEnabled() {
        return Boolean.parseBoolean(props.getProperty(KEY_AUTO_CLEAN_ENABLED, String.valueOf(DEFAULT_AUTO_CLEAN_ENABLED)));
    }
    public void setAutoCleanEnabled(boolean value) { props.setProperty(KEY_AUTO_CLEAN_ENABLED, String.valueOf(value)); }

    public String getAutoCleanInterval() {
        return props.getProperty(KEY_AUTO_CLEAN_INTERVAL, DEFAULT_AUTO_CLEAN_INTERVAL);
    }
    public void setAutoCleanInterval(String interval) { props.setProperty(KEY_AUTO_CLEAN_INTERVAL, interval); }

    public long getAutoCleanLastTime() {
        try {
            return Long.parseLong(props.getProperty(KEY_AUTO_CLEAN_LAST_TIME, String.valueOf(DEFAULT_AUTO_CLEAN_LAST_TIME)));
        } catch (Exception e) {
            return DEFAULT_AUTO_CLEAN_LAST_TIME;
        }
    }
    public void setAutoCleanLastTime(long time) { props.setProperty(KEY_AUTO_CLEAN_LAST_TIME, String.valueOf(time)); }

    public boolean getContextMenuEnabled() {
        return Boolean.parseBoolean(props.getProperty(KEY_CONTEXT_MENU, String.valueOf(DEFAULT_CONTEXT_MENU)));
    }
    public void setContextMenuEnabled(boolean value) { props.setProperty(KEY_CONTEXT_MENU, String.valueOf(value)); }

    public String getConfigFilePath() { return CONFIG_FILE; }
}