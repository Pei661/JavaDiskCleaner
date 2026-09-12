import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ResourceBundle;

public class SettingsDialog extends JDialog {

    private final AppConfig config;
    private final ResourceBundle bundle;
    private final JFrame parent;

    private JTextField backupDirField;
    private JTextField logDirField;
    private JComboBox<String> defaultModeCombo;
    private JComboBox<String> defaultLanguageCombo;
    private JCheckBox simulateDefaultCheckBox;
    private JCheckBox forceDeleteDefaultCheckBox;
    private JCheckBox autoStartCheckBox;
    private JCheckBox autoCleanEnabledCheckBox;
    private JComboBox<String> autoCleanIntervalCombo;
    private JCheckBox contextMenuCheckBox;

    public SettingsDialog(JFrame parent, ResourceBundle bundle, AppConfig config) {
        super(parent, bundle.getString("settings.title"), true);
        this.parent = parent;
        this.bundle = bundle;
        this.config = config;

        setSize(620, 600);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 25, 10, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // 备份目录
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel(bundle.getString("settings.backupDir")), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        backupDirField = new JTextField(config.getBackupDir(), 30);
        formPanel.add(backupDirField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseBackupBtn = new JButton("...");
        browseBackupBtn.setPreferredSize(new Dimension(40, 28));
        browseBackupBtn.addActionListener(e -> chooseDir(backupDirField));
        formPanel.add(browseBackupBtn, gbc);
        row++;

        // 日志目录
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel(bundle.getString("settings.logDir")), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        logDirField = new JTextField(config.getLogDir(), 30);
        formPanel.add(logDirField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseLogBtn = new JButton("...");
        browseLogBtn.setPreferredSize(new Dimension(40, 28));
        browseLogBtn.addActionListener(e -> chooseDir(logDirField));
        formPanel.add(browseLogBtn, gbc);
        row++;

        // 默认删除方式
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel(bundle.getString("settings.defaultMode")), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        defaultModeCombo = new JComboBox<>(new String[]{
                bundle.getString("mode.recycleBin"), bundle.getString("mode.zipBackup")
        });
        defaultModeCombo.setSelectedIndex("zipBackup".equals(config.getDefaultMode()) ? 1 : 0);
        formPanel.add(defaultModeCombo, gbc);
        row++;

        // 默认语言
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        formPanel.add(new JLabel(bundle.getString("settings.defaultLanguage")), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        defaultLanguageCombo = new JComboBox<>(new String[]{
                bundle.getString("settings.language.auto"),
                "中文（简体）", "中文（繁體）", "English", "日本語", "한국어", "Deutsch",
                "Français", "Español", "Português", "Русский", "العربية", "हिन्दी"
        });
        String lang = config.getDefaultLanguage();
        int langIndex = 0;
        if (!"auto".equals(lang)) {
            for (int i = 1; i < defaultLanguageCombo.getItemCount(); i++) {
                if (defaultLanguageCombo.getItemAt(i).equals(lang)) { langIndex = i; break; }
            }
        }
        defaultLanguageCombo.setSelectedIndex(langIndex);
        formPanel.add(defaultLanguageCombo, gbc);
        row++;

        // 模拟模式
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 3;
        simulateDefaultCheckBox = new JCheckBox(bundle.getString("settings.simulate"));
        simulateDefaultCheckBox.setSelected(config.getSimulateDefault());
        formPanel.add(simulateDefaultCheckBox, gbc);
        row++;

        // 强制删除
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3;
        forceDeleteDefaultCheckBox = new JCheckBox(bundle.getString("settings.forceDelete"));
        forceDeleteDefaultCheckBox.setSelected(config.getForceDeleteDefault());
        formPanel.add(forceDeleteDefaultCheckBox, gbc);
        row++;

        // 开机自启
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3;
        autoStartCheckBox = new JCheckBox(bundle.getString("settings.autoStart"));
        autoStartCheckBox.setSelected(config.getAutoStart());
        formPanel.add(autoStartCheckBox, gbc);
        row++;

        // 定时清理开关
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3;
        autoCleanEnabledCheckBox = new JCheckBox(bundle.getString("settings.autoClean"));
        autoCleanEnabledCheckBox.setSelected(config.getAutoCleanEnabled());
        formPanel.add(autoCleanEnabledCheckBox, gbc);
        row++;

        // 定时清理间隔
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        formPanel.add(new JLabel(bundle.getString("settings.autoCleanInterval")), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        autoCleanIntervalCombo = new JComboBox<>(new String[]{
                bundle.getString("settings.interval.daily"),
                bundle.getString("settings.interval.every3days"),
                bundle.getString("settings.interval.weekly")
        });
        String interval = config.getAutoCleanInterval();
        switch (interval) {
            case "every3days": autoCleanIntervalCombo.setSelectedIndex(1); break;
            case "weekly":     autoCleanIntervalCombo.setSelectedIndex(2); break;
            default:           autoCleanIntervalCombo.setSelectedIndex(0); break;
        }
        formPanel.add(autoCleanIntervalCombo, gbc);
        row++;

        // 右键菜单集成
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3; gbc.weightx = 0;
        contextMenuCheckBox = new JCheckBox(bundle.getString("settings.contextMenu"));
        contextMenuCheckBox.setSelected(config.getContextMenuEnabled());
        formPanel.add(contextMenuCheckBox, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton saveBtn = new JButton(bundle.getString("settings.save"));
        saveBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        saveBtn.setPreferredSize(new Dimension(110, 34));
        saveBtn.addActionListener(e -> {
            saveSettings();
            JOptionPane.showMessageDialog(this, bundle.getString("settings.saved"));
            dispose();
        });

        JButton cancelBtn = new JButton(bundle.getString("btn.cancel"));
        cancelBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        cancelBtn.setPreferredSize(new Dimension(110, 34));
        cancelBtn.addActionListener(e -> dispose());

        bottomPanel.add(saveBtn);
        bottomPanel.add(cancelBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void chooseDir(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(field.getText()));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void saveSettings() {
        config.setBackupDir(backupDirField.getText().trim());
        config.setLogDir(logDirField.getText().trim());
        config.setDefaultMode(defaultModeCombo.getSelectedIndex() == 1 ? "zipBackup" : "recycleBin");

        int langIdx = defaultLanguageCombo.getSelectedIndex();
        if (langIdx == 0) config.setDefaultLanguage("auto");
        else config.setDefaultLanguage(defaultLanguageCombo.getItemAt(langIdx));

        config.setSimulateDefault(simulateDefaultCheckBox.isSelected());
        config.setForceDeleteDefault(forceDeleteDefaultCheckBox.isSelected());

        boolean autoStart = autoStartCheckBox.isSelected();
        config.setAutoStart(autoStart);
        AutoStartManager.setAutoStart(autoStart);

        // 定时清理
        boolean wasEnabled = config.getAutoCleanEnabled();
        boolean nowEnabled = autoCleanEnabledCheckBox.isSelected();
        config.setAutoCleanEnabled(nowEnabled);
        switch (autoCleanIntervalCombo.getSelectedIndex()) {
            case 1: config.setAutoCleanInterval("every3days"); break;
            case 2: config.setAutoCleanInterval("weekly"); break;
            default: config.setAutoCleanInterval("daily"); break;
        }
        if (nowEnabled && !wasEnabled) {
            config.setAutoCleanLastTime(System.currentTimeMillis());
        }

        // 右键菜单
        boolean ctxMenu = contextMenuCheckBox.isSelected();
        config.setContextMenuEnabled(ctxMenu);
        ContextMenuManager.setEnabled(ctxMenu);

        config.save();
    }
}