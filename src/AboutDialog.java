import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.util.ResourceBundle;

/**
 * 关于窗口（模态对话框）
 */
public class AboutDialog extends JDialog {

    private static final String REPO_URL = "https://github.com/Pei661/JavaDiskCleaner";
    private static final String VERSION = "1.0.0";

    public AboutDialog(JFrame parent, ResourceBundle bundle) {
        super(parent, bundle.getString("about.title"), true);
        setSize(520, 400);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout(10, 10));

        // 顶部：图标 + 软件名 + 版本
        JPanel headerPanel = new JPanel(new BorderLayout(15, 10));
        headerPanel.setBorder(new EmptyBorder(20, 25, 10, 25));

        JLabel iconLabel = new JLabel("🧹", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
        headerPanel.add(iconLabel, BorderLayout.WEST);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel nameLabel = new JLabel("JDClear");
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        nameLabel.setForeground(new Color(30, 100, 200));

        JLabel versionLabel = new JLabel(
                bundle.getString("about.version").replace("{0}", VERSION));
        versionLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        versionLabel.setForeground(new Color(120, 120, 120));

        titlePanel.add(nameLabel);
        titlePanel.add(versionLabel);
        headerPanel.add(titlePanel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // 中间：描述 + 作者 + 协议 + 主页
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(new EmptyBorder(10, 40, 10, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 描述
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel descLabel = new JLabel(bundle.getString("about.description"));
        descLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        infoPanel.add(descLabel, gbc);

        // 作者
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        JLabel authorKeyLabel = new JLabel(getAuthorLabel(bundle));
        authorKeyLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        infoPanel.add(authorKeyLabel, gbc);

        gbc.gridx = 1;
        JLabel authorValueLabel = new JLabel("Pei");
        authorValueLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        infoPanel.add(authorValueLabel, gbc);

        // 协议
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel licenseKeyLabel = new JLabel(getLicenseLabel(bundle));
        licenseKeyLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        infoPanel.add(licenseKeyLabel, gbc);

        gbc.gridx = 1;
        JLabel licenseValueLabel = new JLabel("MIT");
        licenseValueLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        infoPanel.add(licenseValueLabel, gbc);

        // 项目主页（可点击）
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel webKeyLabel = new JLabel(getWebsiteLabel(bundle));
        webKeyLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        infoPanel.add(webKeyLabel, gbc);

        gbc.gridx = 1;
        JLabel webValueLabel = new JLabel("<html><a href=''>" + REPO_URL + "</a></html>");
        webValueLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        webValueLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        webValueLabel.setForeground(new Color(30, 100, 200));
        webValueLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(REPO_URL));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AboutDialog.this,
                            "无法打开浏览器：" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        infoPanel.add(webValueLabel, gbc);

        add(infoPanel, BorderLayout.CENTER);

        // 底部：关闭按钮
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(new EmptyBorder(5, 10, 15, 10));
        JButton closeBtn = new JButton(bundle.getString("btn.cancel"));
        closeBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        closeBtn.setPreferredSize(new Dimension(100, 32));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /** 从 bundle 里取“作者：”标签，没有就用默认 */
    private String getAuthorLabel(ResourceBundle bundle) {
        try {
            return bundle.getString("label.author") + "：";
        } catch (Exception e) {
            return "作者：";
        }
    }

    /** 从 bundle 里取“开源协议：”标签 */
    private String getLicenseLabel(ResourceBundle bundle) {
        try {
            return bundle.getString("about.license").replace("{0}", "") + "：";
        } catch (Exception e) {
            return "开源协议：";
        }
    }

    /** 从 bundle 里取“项目主页：”标签 */
    private String getWebsiteLabel(ResourceBundle bundle) {
        try {
            return bundle.getString("about.website").replace("{0}", "") + "：";
        } catch (Exception e) {
            return "项目主页：";
        }
    }
}