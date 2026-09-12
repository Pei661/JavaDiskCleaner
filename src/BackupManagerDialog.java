import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 备份管理窗口：查看 backup/ 里的 zip，还原文件。
 */
public class BackupManagerDialog extends JDialog {

    private final JFrame parent;
    private final ResourceBundle bundle;
    private final String backupDir;

    private DefaultTableModel tableModel;
    private JTable table;
    private List<File> zipFiles = new ArrayList<>();
    private JTextArea detailArea;

    public BackupManagerDialog(JFrame parent, ResourceBundle bundle, String backupDir) {
        super(parent, bundle.getString("backup.title"), true);
        this.parent = parent;
        this.bundle = bundle;
        this.backupDir = backupDir;

        setSize(900, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // 顶部：标题 + 刷新
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(new EmptyBorder(10, 10, 0, 10));

        JLabel titleLabel = new JLabel(bundle.getString("backup.title"));
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        titleLabel.setForeground(new Color(30, 100, 200));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JButton refreshBtn = new JButton(bundle.getString("backup.refresh"));
        refreshBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        refreshBtn.addActionListener(e -> loadBackups());
        topPanel.add(refreshBtn, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 中间：zip 列表 + 详情
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        // zip 列表
        String[] columns = {
                bundle.getString("backup.col.name"),
                bundle.getString("backup.col.size"),
                bundle.getString("backup.col.time")
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showZipContent();
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder(bundle.getString("backup.list")));
        splitPane.setTopComponent(tableScroll);

        // 详情
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        detailArea.setBackground(new Color(250, 250, 250));
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder(bundle.getString("backup.detail")));
        splitPane.setBottomComponent(detailScroll);

        add(splitPane, BorderLayout.CENTER);

        // 底部：还原 + 删除 + 关闭
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton restoreBtn = new JButton(bundle.getString("backup.restore"));
        restoreBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        restoreBtn.setPreferredSize(new Dimension(130, 34));
        restoreBtn.addActionListener(e -> restoreSelected());

        JButton deleteBtn = new JButton(bundle.getString("backup.delete"));
        deleteBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        deleteBtn.setPreferredSize(new Dimension(130, 34));
        deleteBtn.addActionListener(e -> deleteSelected());

        JButton closeBtn = new JButton(bundle.getString("btn.cancel"));
        closeBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        closeBtn.setPreferredSize(new Dimension(130, 34));
        closeBtn.addActionListener(e -> dispose());

        bottomPanel.add(restoreBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        loadBackups();
    }

    /** 加载 backup/ 下所有 zip */
    private void loadBackups() {
        tableModel.setRowCount(0);
        zipFiles.clear();
        detailArea.setText("");

        File dir = new File(backupDir);
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip"));
        if (files == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (File f : files) {
            zipFiles.add(f);
            tableModel.addRow(new Object[]{
                    f.getName(),
                    String.format("%.2f MB", f.length() / 1024.0 / 1024.0),
                    sdf.format(new Date(f.lastModified()))
            });
        }
    }

    /** 显示选中 zip 里的文件列表 */
    private void showZipContent() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= zipFiles.size()) {
            detailArea.setText("");
            return;
        }
        File zip = zipFiles.get(row);
        StringBuilder sb = new StringBuilder();
        sb.append(bundle.getString("backup.detail")).append(": ").append(zip.getName()).append("\n\n");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zis.getNextEntry()) != null) {
                sb.append(entry.getName()).append("\n");
                count++;
                if (count > 500) {
                    sb.append("... (more)\n");
                    break;
                }
            }
            sb.insert(sb.indexOf("\n\n") + 2,
                    bundle.getString("backup.fileCount").replace("{0}", String.valueOf(count)) + "\n\n");
        } catch (IOException e) {
            sb.append("Error: ").append(e.getMessage());
        }
        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    /** 还原选中的 zip */
    private void restoreSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= zipFiles.size()) {
            JOptionPane.showMessageDialog(this, bundle.getString("backup.noSelection"));
            return;
        }
        File zip = zipFiles.get(row);

        int confirm = JOptionPane.showConfirmDialog(this,
                bundle.getString("backup.restoreConfirm").replace("{0}", zip.getName()),
                bundle.getString("backup.restore"),
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        int ok = 0, fail = 0;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String entryName = entry.getName().replace("/", File.separator);
                File target = new File(entryName);

                File parentDir = target.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try (FileOutputStream fos = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    ok++;
                } catch (IOException ex) {
                    fail++;
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return;
        }

        String msg = bundle.getString("backup.restoreDone")
                .replace("{0}", String.valueOf(ok))
                .replace("{1}", String.valueOf(fail));
        JOptionPane.showMessageDialog(this, msg);
    }

    /** 删除选中的 zip */
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= zipFiles.size()) {
            JOptionPane.showMessageDialog(this, bundle.getString("backup.noSelection"));
            return;
        }
        File zip = zipFiles.get(row);

        int confirm = JOptionPane.showConfirmDialog(this,
                bundle.getString("backup.deleteConfirm").replace("{0}", zip.getName()),
                bundle.getString("backup.delete"),
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (zip.delete()) {
            loadBackups();
        } else {
            JOptionPane.showMessageDialog(this, "Delete failed: " + zip.getName());
        }
    }
}