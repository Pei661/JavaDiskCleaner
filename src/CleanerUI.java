import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CleanerUI extends JFrame {

    private static final String[] WHITE_LIST = {
            ".tmp", ".temp", ".log", ".cache", ".dmp", ".old", ".bak", ".chk"
    };

    private static final String[] BLACK_LIST_DIRS = {
            "Steam", "steamapps", "Program Files", "Program Files (x86)",
            "ProgramData", "System32", "SysWOW64",
            "AppData\\Local\\Google", "AppData\\Local\\Microsoft\\Edge", "AppData\\Roaming\\Tencent"
    };

    private static final String[] CRITICAL_DIRS = {
            "C:\\Windows\\System32", "C:\\Windows\\SysWOW64", "C:\\Windows\\WinSxS",
            "C:\\Windows\\Boot", "C:\\Windows\\Fonts", "C:\\Windows\\assembly",
            "C:\\Windows\\Microsoft.NET", "C:\\Program Files\\WindowsApps",
            "C:\\ProgramData\\Microsoft\\Windows"
    };

    private static final String[] DEEP_CLEAN_TARGETS = {
            "C:\\Windows\\Temp",
            "C:\\Windows\\SoftwareDistribution\\Download",
            System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Cache",
            System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Default\\Cache",
            System.getProperty("user.home") + "\\AppData\\Local\\Mozilla\\Firefox\\Profiles",
            System.getProperty("user.home") + "\\AppData\\Roaming\\360se6\\User Data\\Default\\Cache",
            System.getProperty("user.home") + "\\AppData\\Local\\Tencent\\QQBrowser\\User Data\\Default\\Cache",
            System.getProperty("user.home") + "\\AppData\\Local\\Temp"
    };

    private static final Locale ES = new Locale("es", "ES");
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final Locale RU = new Locale("ru", "RU");
    private static final Locale ZH_TW = Locale.TRADITIONAL_CHINESE;
    private static final Locale AR_SA = new Locale("ar", "SA");
    private static final Locale HI_IN = new Locale("hi", "IN");

    private static final Locale[] LOCALES = {
            Locale.SIMPLIFIED_CHINESE, ZH_TW, Locale.US, Locale.JAPAN, Locale.KOREA,
            Locale.GERMANY, Locale.FRANCE, ES, PT_BR, RU, AR_SA, HI_IN
    };
    private static final String[] LOCALE_NAMES = {
            "中文（简体）", "中文（繁體）", "English", "日本語", "한국어", "Deutsch",
            "Français", "Español", "Português", "Русский", "العربية", "हिन्दी"
    };

    private ResourceBundle bundle;
    private Locale currentLocale;

    private static final Map<Locale, String> SAMPLE_CHARS = new HashMap<>();
    static {
        SAMPLE_CHARS.put(Locale.SIMPLIFIED_CHINESE, "磁盘清理工具扫描");
        SAMPLE_CHARS.put(ZH_TW, "磁碟清理工具掃描");
        SAMPLE_CHARS.put(Locale.US, "Disk Cleaner Scan");
        SAMPLE_CHARS.put(Locale.JAPAN, "ディスククリーナー");
        SAMPLE_CHARS.put(Locale.KOREA, "디스크 클리너");
        SAMPLE_CHARS.put(Locale.GERMANY, "Festplattenreiniger");
        SAMPLE_CHARS.put(Locale.FRANCE, "Nettoyeur de disque");
        SAMPLE_CHARS.put(ES, "Limpiador de disco");
        SAMPLE_CHARS.put(PT_BR, "Limpador de disco");
        SAMPLE_CHARS.put(RU, "Очистка диска");
        SAMPLE_CHARS.put(AR_SA, "منظف القرص");
        SAMPLE_CHARS.put(HI_IN, "डिस्क क्लीनर");
    }

    private static final String[] FONT_CANDIDATES = {
            "Microsoft YaHei UI", "Microsoft YaHei", "微软雅黑",
            "Malgun Gothic", "Meiryo UI", "Yu Gothic UI",
            "Arial Unicode MS", "Segoe UI", "SimSun", "宋体",
            "Dialog", Font.SANS_SERIF
    };

    private static String pickFontNameForLocale(Locale locale) {
        String sample = SAMPLE_CHARS.getOrDefault(locale, "ABC");
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Set<String> available = new HashSet<>(Arrays.asList(ge.getAvailableFontFamilyNames()));
        for (String candidate : FONT_CANDIDATES) {
            if (!available.contains(candidate)) continue;
            Font f = new Font(candidate, Font.PLAIN, 13);
            if (f.canDisplayUpTo(sample) == -1) return candidate;
        }
        for (String name : available) {
            Font f = new Font(name, Font.PLAIN, 13);
            if (f.canDisplayUpTo(sample) == -1) return name;
        }
        return Font.SANS_SERIF;
    }

    private Font uiFont, titleFont, logFont;

    private void updateFonts() {
        String name = pickFontNameForLocale(currentLocale);
        uiFont = new Font(name, Font.PLAIN, 13);
        titleFont = new Font(name, Font.BOLD, 13);
        logFont = new Font(name, Font.PLAIN, 12);
    }

    private Locale detectSystemLocale() {
        Locale sys = Locale.getDefault();
        for (Locale loc : LOCALES) if (loc.equals(sys)) return loc;
        for (Locale loc : LOCALES) if (loc.getLanguage().equals(sys.getLanguage())) return loc;
        return Locale.US;
    }

    private Locale findLocaleByName(String name) {
        if (name == null || "auto".equals(name)) return detectSystemLocale();
        for (int i = 0; i < LOCALE_NAMES.length; i++)
            if (LOCALE_NAMES[i].equals(name)) return LOCALES[i];
        return detectSystemLocale();
    }

    private AppConfig config;
    private AutoCleanScheduler autoCleanScheduler;

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel statusLabel, titleLabel;
    private List<File> foundFiles = new ArrayList<>();
    private JCheckBox simulateModeCheckBox, forceDeleteCheckBox;
    private JComboBox<String> driveComboBox, deleteModeComboBox, languageComboBox;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JLabel progressLabel;

    private JButton scanBtn, deepScanBtn, cleanBtn, selectAllBtn, deselectAllBtn;
    private JButton openBackupBtn, openRecycleBinBtn, exportLogBtn, exitBtn;
    private JButton aboutBtn, settingsBtn, backupManagerBtn, openLogFolderBtn;
    private JLabel driveLabel, modeLabel, langLabel;
    private TitledBorder scanResultBorder, logBorder;

    private String backupDir, logDir, logFile;

    private TrayIcon trayIcon;
    private boolean trayInitialized = false;

    public CleanerUI() {
        config = new AppConfig();

        String cfgLang = config.getDefaultLanguage();
        currentLocale = "auto".equals(cfgLang) ? detectSystemLocale() : findLocaleByName(cfgLang);
        loadBundle();
        updateFonts();

        backupDir = config.getBackupDir();
        logDir = config.getLogDir();
        logFile = logDir + File.separator + "JDClear.log";

        setTitle(bundle.getString("window.title"));
        setSize(1050, 680);
        setMinimumSize(new Dimension(950, 580));
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(new EmptyBorder(10, 10, 5, 10));

        titleLabel = new JLabel(bundle.getString("title"));
        titleLabel.setFont(new Font(titleFont.getName(), Font.BOLD, 18));
        titleLabel.setForeground(new Color(30, 100, 200));

        statusLabel = new JLabel(bundle.getString("status.ready"));
        statusLabel.setFont(uiFont);
        statusLabel.setForeground(new Color(80, 80, 80));
        statusLabel.setBorder(new EmptyBorder(5, 0, 5, 0));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(statusLabel, BorderLayout.SOUTH);
        topPanel.add(titlePanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        buttonPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        driveLabel = new JLabel(bundle.getString("label.drive"));
        row1.add(driveLabel);
        driveComboBox = new JComboBox<>();
        loadDrives();
        row1.add(driveComboBox);
        modeLabel = new JLabel(bundle.getString("label.mode"));
        row1.add(modeLabel);
        deleteModeComboBox = new JComboBox<>(new String[]{
                bundle.getString("mode.recycleBin"), bundle.getString("mode.zipBackup")
        });
        deleteModeComboBox.setSelectedIndex("zipBackup".equals(config.getDefaultMode()) ? 1 : 0);
        row1.add(deleteModeComboBox);

        scanBtn = createButton(bundle.getString("btn.scan"));
        deepScanBtn = createButton(bundle.getString("btn.deepScan"));
        cleanBtn = createButton(bundle.getString("btn.clean"));
        row1.add(scanBtn); row1.add(deepScanBtn); row1.add(cleanBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        selectAllBtn = createButton(bundle.getString("btn.selectAll"));
        deselectAllBtn = createButton(bundle.getString("btn.deselectAll"));
        simulateModeCheckBox = new JCheckBox(bundle.getString("check.simulate"));
        simulateModeCheckBox.setSelected(config.getSimulateDefault());
        forceDeleteCheckBox = new JCheckBox(bundle.getString("check.forceDelete"));
        forceDeleteCheckBox.setForeground(new Color(200, 50, 50));
        forceDeleteCheckBox.setSelected(config.getForceDeleteDefault());
        row2.add(selectAllBtn); row2.add(deselectAllBtn);
        row2.add(simulateModeCheckBox); row2.add(forceDeleteCheckBox);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        openBackupBtn = createButton(bundle.getString("btn.openBackup"));
        openRecycleBinBtn = createButton(bundle.getString("btn.openRecycleBin"));
        exportLogBtn = createButton(bundle.getString("btn.exportLog"));
        aboutBtn = createButton(bundle.getString("btn.about"));
        settingsBtn = createButton(bundle.getString("btn.settings"));
        backupManagerBtn = createButton(bundle.getString("btn.backupManager"));
        openLogFolderBtn = createButton(bundle.getString("btn.openLogFolder"));
        exitBtn = createButton(bundle.getString("btn.exit"));

        langLabel = new JLabel(bundle.getString("label.language"));
        languageComboBox = new JComboBox<>(LOCALE_NAMES);
        for (int i = 0; i < LOCALES.length; i++) {
            if (LOCALES[i].equals(currentLocale)) { languageComboBox.setSelectedIndex(i); break; }
        }

        row3.add(openBackupBtn); row3.add(openRecycleBinBtn); row3.add(exportLogBtn);
        row3.add(aboutBtn); row3.add(settingsBtn); row3.add(backupManagerBtn);
        row3.add(openLogFolderBtn); row3.add(exitBtn);
        row3.add(langLabel); row3.add(languageComboBox);

        buttonPanel.add(row1); buttonPanel.add(row2); buttonPanel.add(row3);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(new EmptyBorder(0, 10, 5, 10));

        JPanel progressPanel = new JPanel(new BorderLayout(5, 0));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(uiFont);
        progressBar.setForeground(new Color(60, 170, 90));
        progressLabel = new JLabel(bundle.getString("progress.ready"));
        progressLabel.setFont(uiFont);
        progressLabel.setForeground(new Color(100, 100, 100));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(progressLabel, BorderLayout.SOUTH);
        centerPanel.add(progressPanel, BorderLayout.NORTH);

        String[] columns = {
                bundle.getString("table.header.select"),
                bundle.getString("table.header.name"),
                bundle.getString("table.header.size"),
                bundle.getString("table.header.path")
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int row, int column) { return column == 0; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(220, 235, 255));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                                                                     boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 248, 252));
                return c;
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(600);

        JScrollPane scrollPane = new JScrollPane(table);
        scanResultBorder = BorderFactory.createTitledBorder(bundle.getString("border.scanResult"));
        scrollPane.setBorder(scanResultBorder);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        logArea = new JTextArea(5, 20);
        logArea.setEditable(false);
        logArea.setFont(logFont);
        logArea.setBackground(new Color(250, 250, 250));
        JScrollPane logScroll = new JScrollPane(logArea);
        logBorder = BorderFactory.createTitledBorder(bundle.getString("border.log"));
        logScroll.setBorder(logBorder);
        centerPanel.add(logScroll, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        scanBtn.addActionListener(e -> startScan());
        deepScanBtn.addActionListener(e -> startDeepScan());
        cleanBtn.addActionListener(e -> startClean());
        selectAllBtn.addActionListener(e -> selectAll(true));
        deselectAllBtn.addActionListener(e -> selectAll(false));
        openBackupBtn.addActionListener(e -> openFolder(backupDir));
        openRecycleBinBtn.addActionListener(e -> openRecycleBin());
        exportLogBtn.addActionListener(e -> exportLog());
        aboutBtn.addActionListener(e -> new AboutDialog(this, bundle).setVisible(true));
        settingsBtn.addActionListener(e -> openSettings());
        backupManagerBtn.addActionListener(e ->
                new BackupManagerDialog(this, bundle, backupDir).setVisible(true));
        openLogFolderBtn.addActionListener(e -> openFolder(logDir));
        exitBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    bundle.getString("confirm.exitMessage"),
                    bundle.getString("confirm.exitTitle"),
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                if (autoCleanScheduler != null) autoCleanScheduler.stop();
                if (trayInitialized) {
                    try { SystemTray.getSystemTray().remove(trayIcon); } catch (Exception ignored) {}
                }
                System.exit(0);
            }
        });
        languageComboBox.addActionListener(e -> {
            currentLocale = LOCALES[languageComboBox.getSelectedIndex()];
            loadBundle();
            updateFonts();
            refreshTexts();
            refreshFonts();
            applyOrientation();
        });

        try { setIconImage(TrayIconFactory.createWindowIcon()); } catch (Exception ignored) {}

        applyOrientation();
        initSystemTray();

        autoCleanScheduler = new AutoCleanScheduler(config, this::runAutoClean);
        autoCleanScheduler.start();

        setVisible(true);
    }

    /** 外部指定目录，自动扫描 */
    public void scanFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) return;
        driveComboBox.removeAllItems();
        driveComboBox.addItem(folder.getAbsolutePath());
        driveComboBox.setSelectedIndex(0);
        startScan();
    }

    // ========== 自动清理 ==========
    private void runAutoClean() {
        SwingUtilities.invokeLater(() -> {
            try {
                writeLog("========== Auto clean started ==========");
                List<File> tempList = new ArrayList<>();
                for (String target : DEEP_CLEAN_TARGETS) {
                    File dir = new File(target);
                    if (dir.exists() && dir.isDirectory()) {
                        deepScanDirectory(dir, tempList);
                    }
                }
                int cleaned = 0;
                long size = 0;
                for (File f : tempList) {
                    if (isCriticalDir(f.getAbsolutePath())) continue;
                    long fs = f.length();
                    int r = moveToRecycleBin(f);
                    if (r == 1) { cleaned++; size += fs; }
                }
                String msgText = "Auto clean done: " + cleaned + " files, "
                        + String.format("%.2f", size / 1024.0 / 1024.0) + " MB";
                writeLog(msgText);
                if (trayInitialized && trayIcon != null) {
                    trayIcon.displayMessage("JDClear", msgText, TrayIcon.MessageType.INFO);
                }
            } catch (Exception e) {
                writeLog("Auto clean error: " + e.getMessage());
            }
        });
    }

    // ========== 系统托盘 ==========
    private void initSystemTray() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            if (trayInitialized && trayIcon != null) tray.remove(trayIcon);
            Image image = TrayIconFactory.createTrayIcon();
            PopupMenu popup = new PopupMenu();
            MenuItem openItem = new MenuItem("Open JDClear");
            openItem.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
                toFront();
            });
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                if (autoCleanScheduler != null) autoCleanScheduler.stop();
                tray.remove(trayIcon);
                System.exit(0);
            });
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon = new TrayIcon(image, "JDClear", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
                toFront();
            });
            tray.add(trayIcon);
            trayInitialized = true;
        } catch (Exception e) {
            System.err.println("托盘初始化失败：" + e.getMessage());
        }
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this, bundle, config);
        dialog.setVisible(true);
        config.load();
        backupDir = config.getBackupDir();
        logDir = config.getLogDir();
        logFile = logDir + File.separator + "JDClear.log";

        deleteModeComboBox.setSelectedIndex("zipBackup".equals(config.getDefaultMode()) ? 1 : 0);
        simulateModeCheckBox.setSelected(config.getSimulateDefault());
        forceDeleteCheckBox.setSelected(config.getForceDeleteDefault());
    }

    private void applyOrientation() {
        boolean rtl = "ar".equals(currentLocale.getLanguage()) || "he".equals(currentLocale.getLanguage());
        ComponentOrientation orientation = rtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;
        applyOrientationRecursively(this, orientation);
        revalidate(); repaint();
    }

    private void applyOrientationRecursively(Component comp, ComponentOrientation orientation) {
        comp.setComponentOrientation(orientation);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents())
                applyOrientationRecursively(child, orientation);
        }
    }

    private void loadBundle() { bundle = ResourceBundle.getBundle("messages", currentLocale); }

    private void refreshTexts() {
        setTitle(bundle.getString("window.title"));
        titleLabel.setText(bundle.getString("title"));
        statusLabel.setText(bundle.getString("status.ready"));
        driveLabel.setText(bundle.getString("label.drive"));
        modeLabel.setText(bundle.getString("label.mode"));
        langLabel.setText(bundle.getString("label.language"));
        scanBtn.setText(bundle.getString("btn.scan"));
        deepScanBtn.setText(bundle.getString("btn.deepScan"));
        cleanBtn.setText(bundle.getString("btn.clean"));
        selectAllBtn.setText(bundle.getString("btn.selectAll"));
        deselectAllBtn.setText(bundle.getString("btn.deselectAll"));
        simulateModeCheckBox.setText(bundle.getString("check.simulate"));
        forceDeleteCheckBox.setText(bundle.getString("check.forceDelete"));
        openBackupBtn.setText(bundle.getString("btn.openBackup"));
        openRecycleBinBtn.setText(bundle.getString("btn.openRecycleBin"));
        exportLogBtn.setText(bundle.getString("btn.exportLog"));
        exitBtn.setText(bundle.getString("btn.exit"));
        aboutBtn.setText(bundle.getString("btn.about"));
        settingsBtn.setText(bundle.getString("btn.settings"));
        backupManagerBtn.setText(bundle.getString("btn.backupManager"));
        openLogFolderBtn.setText(bundle.getString("btn.openLogFolder"));
        progressLabel.setText(bundle.getString("progress.ready"));

        tableModel.setColumnIdentifiers(new String[]{
                bundle.getString("table.header.select"),
                bundle.getString("table.header.name"),
                bundle.getString("table.header.size"),
                bundle.getString("table.header.path")
        });

        scanResultBorder.setTitle(bundle.getString("border.scanResult"));
        logBorder.setTitle(bundle.getString("border.log"));

        deleteModeComboBox.removeAllItems();
        deleteModeComboBox.addItem(bundle.getString("mode.recycleBin"));
        deleteModeComboBox.addItem(bundle.getString("mode.zipBackup"));

        initSystemTray();
        repaint();
    }

    private void refreshFonts() {
        applyFontRecursively(this, uiFont);
        titleLabel.setFont(new Font(titleFont.getName(), Font.BOLD, 18));
        logArea.setFont(logFont);
        repaint();
    }

    private void applyFontRecursively(Component comp, Font font) {
        comp.setFont(font);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents())
                applyFontRecursively(child, font);
        }
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(uiFont);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void loadDrives() {
        File[] roots = File.listRoots();
        for (File root : roots) driveComboBox.addItem(root.getAbsolutePath());
        if (driveComboBox.getItemCount() > 0) driveComboBox.setSelectedIndex(0);
    }

    private String msg(String key, Object... args) { return MessageFormat.format(bundle.getString(key), args); }

    private boolean isWhiteListed(String fileName) {
        String lower = fileName.toLowerCase();
        for (String ext : WHITE_LIST) if (lower.endsWith(ext)) return true;
        return false;
    }

    private boolean isBlackListed(String path) {
        for (String dir : BLACK_LIST_DIRS) if (path.contains(dir)) return true;
        return false;
    }

    private boolean isCriticalDir(String path) {
        for (String dir : CRITICAL_DIRS)
            if (path.toLowerCase().startsWith(dir.toLowerCase())) return true;
        return false;
    }

    private void updateProgress(int value, String text) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            progressBar.setString(value + "%");
            progressLabel.setText(text);
        });
    }

    private void writeLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String line = "[" + time + "] " + message;
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
        try {
            File dir = new File(logDir);
            if (!dir.exists()) dir.mkdirs();
            try (FileWriter fw = new FileWriter(logFile, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException ignored) {}
    }

    private void exportLog() {
        try {
            File dir = new File(logDir);
            if (!dir.exists()) dir.mkdirs();
            String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File exportFile = new File(dir, "log_" + time + ".txt");
            try (FileWriter fw = new FileWriter(exportFile);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(logArea.getText());
            }
            String path = exportFile.getAbsolutePath();
            Runtime.getRuntime().exec("explorer /select,\"" + path + "\"");
            JOptionPane.showMessageDialog(this, msg("dialog.exportDone", path));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, msg("dialog.exportFailed", e.getMessage()),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startScan() {
        tableModel.setRowCount(0);
        foundFiles.clear();
        writeLog(bundle.getString("log.startScan"));

        String selectedDrive = (String) driveComboBox.getSelectedItem();
        if (selectedDrive == null) {
            JOptionPane.showMessageDialog(this, bundle.getString("dialog.selectDrive"));
            return;
        }

        statusLabel.setText(msg("status.scanning", selectedDrive));
        writeLog(msg("log.scanTarget", selectedDrive));
        updateProgress(0, bundle.getString("progress.scanning"));

        new Thread(() -> {
            File rootDir = new File(selectedDrive);
            List<File> tempList = new ArrayList<>();
            scanDirectory(rootDir, tempList);

            SwingUtilities.invokeLater(() -> {
                foundFiles.addAll(tempList);
                for (File f : foundFiles) {
                    tableModel.addRow(new Object[]{
                            false, f.getName(),
                            String.format("%.2f", f.length() / 1024.0 / 1024.0),
                            f.getAbsolutePath()
                    });
                }
                long totalSize = 0;
                for (File f : foundFiles) totalSize += f.length();
                double totalMB = totalSize / 1024.0 / 1024.0;
                statusLabel.setText(msg("status.scanDone", foundFiles.size(), String.format("%.2f", totalMB)));
                writeLog(msg("log.scanDone", foundFiles.size(), String.format("%.2f", totalMB)));
                updateProgress(100, bundle.getString("progress.done"));
            });
        }).start();
    }

    private void startDeepScan() {
        tableModel.setRowCount(0);
        foundFiles.clear();
        writeLog(bundle.getString("log.startDeepScan"));
        statusLabel.setText(bundle.getString("status.deepScanning"));
        updateProgress(0, bundle.getString("progress.deepScanning"));

        new Thread(() -> {
            List<File> tempList = new ArrayList<>();
            int total = DEEP_CLEAN_TARGETS.length;
            int done = 0;
            for (String target : DEEP_CLEAN_TARGETS) {
                File dir = new File(target);
                if (dir.exists() && dir.isDirectory()) {
                    writeLog(msg("log.scanDir", target));
                    deepScanDirectory(dir, tempList);
                }
                done++;
                updateProgress(done * 100 / total, (done + "/" + total));
            }

            SwingUtilities.invokeLater(() -> {
                foundFiles.addAll(tempList);
                for (File f : foundFiles) {
                    tableModel.addRow(new Object[]{
                            false, f.getName(),
                            String.format("%.2f", f.length() / 1024.0 / 1024.0),
                            f.getAbsolutePath()
                    });
                }
                long totalSize = 0;
                for (File f : foundFiles) totalSize += f.length();
                double totalMB = totalSize / 1024.0 / 1024.0;
                statusLabel.setText(msg("status.deepScanDone", foundFiles.size(), String.format("%.2f", totalMB)));
                writeLog(msg("log.deepScanDone", foundFiles.size(), String.format("%.2f", totalMB)));
                updateProgress(100, bundle.getString("progress.done"));
            });
        }).start();
    }

    private void scanDirectory(File dir, List<File> tempList) {
        if (dir == null || !dir.exists()) return;
        String currentPath = dir.getAbsolutePath();
        if (isCriticalDir(currentPath)) return;
        if (isBlackListed(currentPath)) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanDirectory(f, tempList);
            else if (isWhiteListed(f.getName()) && f.length() > 1024 * 1024) tempList.add(f);
        }
    }

    private void deepScanDirectory(File dir, List<File> tempList) {
        if (dir == null || !dir.exists()) return;
        if (isCriticalDir(dir.getAbsolutePath())) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) deepScanDirectory(f, tempList);
            else {
                String lower = f.getName().toLowerCase();
                if (lower.endsWith(".exe") || lower.endsWith(".dll") || lower.endsWith(".sys")
                        || lower.endsWith(".ini") || lower.endsWith(".dat")) continue;
                if (f.length() > 0) tempList.add(f);
            }
        }
    }

    private void startClean() {
        boolean simulate = simulateModeCheckBox.isSelected();
        boolean forceDelete = forceDeleteCheckBox.isSelected();

        List<File> selectedFiles = new ArrayList<>();
        long selectedSize = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean checked = (Boolean) tableModel.getValueAt(i, 0);
            if (checked != null && checked) {
                File f = new File((String) tableModel.getValueAt(i, 3));
                if (isCriticalDir(f.getAbsolutePath())) { writeLog(msg("log.locked", f.getName())); continue; }
                selectedFiles.add(f);
                selectedSize += f.length();
            }
        }

        if (selectedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, bundle.getString("dialog.noSelection"));
            return;
        }

        if (!simulate) {
            double sizeMB = selectedSize / 1024.0 / 1024.0;
            String mode = (String) deleteModeComboBox.getSelectedItem();
            String msgText = msg("confirm.message", selectedFiles.size(), String.format("%.2f", sizeMB), mode)
                    + (forceDelete ? "\n⚠ " + bundle.getString("warning.riskyOperation") : "");
            int result = JOptionPane.showConfirmDialog(this, msgText,
                    bundle.getString("confirm.title"), JOptionPane.YES_NO_OPTION,
                    forceDelete ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.YES_OPTION) return;
        }

        if (!simulate && forceDelete) {
            int result2 = JOptionPane.showConfirmDialog(this,
                    bundle.getString("confirm.forceMessage"),
                    bundle.getString("confirm.forceTitle"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (result2 != JOptionPane.YES_OPTION) return;
        }

        writeLog(msg("log.startClean", selectedFiles.size()));
        updateProgress(0, bundle.getString("progress.cleaning"));

        final boolean finalSimulate = simulate;
        final boolean finalForceDelete = forceDelete;
        final long finalSelectedSize = selectedSize;

        new Thread(() -> {
            int deletedCount = 0; long deletedSize = 0;
            int total = selectedFiles.size();

            if (finalSimulate) {
                deletedCount = total; deletedSize = finalSelectedSize;
                updateProgress(100, bundle.getString("progress.done"));
            } else {
                String mode = (String) deleteModeComboBox.getSelectedItem();
                String recycleBinLabel = bundle.getString("mode.recycleBin");
                if (recycleBinLabel.equals(mode)) {
                    for (int i = 0; i < total; i++) {
                        File f = selectedFiles.get(i);
                        long fileSize = f.length();
                        int result = moveToRecycleBin(f);
                        if (result == 1) { deletedCount++; deletedSize += fileSize; }
                        else if (result == -1 && finalForceDelete) {
                            if (forceDelete(f)) { deletedCount++; deletedSize += fileSize; }
                        } else if (result == -1) { writeLog(msg("log.locked", f.getName())); }
                        if ((i + 1) % 50 == 0) writeLog(msg("log.processed", (i + 1), total));
                        updateProgress((i + 1) * 100 / total, (i + 1) + "/" + total);
                    }
                } else {
                    File backupDirFile = new File(backupDir);
                    if (!backupDirFile.exists()) backupDirFile.mkdirs();
                    String zipName = "backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".zip";
                    File zipFile = new File(backupDirFile, zipName);

                    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                        for (int i = 0; i < total; i++) {
                            File f = selectedFiles.get(i);
                            if (f.exists()) {
                                long fileSize = f.length();
                                try {
                                    String entryName = f.getAbsolutePath().replace("\\", "/");
                                    ZipEntry entry = new ZipEntry(entryName);
                                    zos.putNextEntry(entry);
                                    Files.copy(f.toPath(), zos);
                                    zos.closeEntry();
                                    if (f.delete()) { deletedCount++; deletedSize += fileSize; }
                                    else writeLog(msg("log.locked", f.getName()));
                                } catch (IOException e) { writeLog(msg("log.locked", f.getName())); }
                            }
                            if ((i + 1) % 50 == 0) writeLog(msg("log.processed", (i + 1), total));
                            updateProgress((i + 1) * 100 / total, (i + 1) + "/" + total);
                        }
                    } catch (IOException e) { writeLog(msg("log.error", e.getMessage())); }
                }
            }

            final int fc = deletedCount;
            final long fs = deletedSize;
            SwingUtilities.invokeLater(() -> {
                String title = finalSimulate ? bundle.getString("dialog.simulateResult")
                        : bundle.getString("dialog.cleanResult");
                String message = finalSimulate
                        ? msg("dialog.simulateResultMsg", fc, String.format("%.2f", fs / 1024.0 / 1024.0))
                        : msg("dialog.cleanResultMsg", fc, String.format("%.2f", fs / 1024.0 / 1024.0),
                        bundle.getString("info.recycleBin"));
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
                updateProgress(100, bundle.getString("progress.done"));
                if (!finalSimulate) startScan();
            });
        }).start();
    }

    private int moveToRecycleBin(File file) {
        try {
            String path = file.getAbsolutePath().replace("'", "''");
            String psCommand = "$ProgressPreference = 'SilentlyContinue'; " +
                    "Add-Type -AssemblyName Microsoft.VisualBasic; " +
                    "[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile('" + path + "', 'OnlyErrorDialogs', 'SendToRecycleBin')";
            byte[] bytes = psCommand.getBytes("UTF-16LE");
            String encodedCommand = java.util.Base64.getEncoder().encodeToString(bytes);

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encodedCommand
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
            int exitCode = process.waitFor();
            String out = output.toString();
            if (exitCode == 0) return 1;
            if (out.contains("拒绝访问") || out.contains("Access") || out.contains("Error")) return -1;
            return 0;
        } catch (Exception e) { return 0; }
    }

    private boolean forceDelete(File file) {
        try {
            String path = file.getAbsolutePath();
            if (isCriticalDir(path)) return false;
            if (file.delete()) return true;

            String psCommand = "$ProgressPreference = 'SilentlyContinue'; " +
                    "Remove-Item -LiteralPath '" + path.replace("'", "''") + "' -Force -ErrorAction SilentlyContinue";
            byte[] bytes = psCommand.getBytes("UTF-16LE");
            String encodedCommand = java.util.Base64.getEncoder().encodeToString(bytes);

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encodedCommand
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
            return !file.exists();
        } catch (Exception e) { return false; }
    }

    private void openFolder(String path) {
        try {
            File folder = new File(path);
            if (!folder.exists()) folder.mkdirs();
            Desktop.getDesktop().open(folder);
        } catch (IOException e) { writeLog(msg("log.error", e.getMessage())); }
    }

    private void openRecycleBin() {
        try { Runtime.getRuntime().exec("explorer.exe shell:RecycleBinFolder"); }
        catch (IOException e) { writeLog(msg("log.error", e.getMessage())); }
    }

    private void selectAll(boolean selected) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(selected, i, 0);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        final String folderArg = (args.length > 0) ? args[0] : null;

        SwingUtilities.invokeLater(() -> {
            CleanerUI ui = new CleanerUI();
            if (folderArg != null) {
                SwingUtilities.invokeLater(() -> ui.scanFolder(folderArg));
            }
        });
    }
}