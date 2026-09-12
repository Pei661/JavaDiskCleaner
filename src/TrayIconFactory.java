import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 用 Java 代码画托盘图标，避免依赖外部图片文件。
 */
public class TrayIconFactory {

    /** 画一个 64x64 的图标：蓝色圆角方块 + 白色刷子 */
    public static Image createTrayIcon() {
        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // 抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 蓝色圆角背景
        g.setColor(new Color(30, 100, 200));
        g.fillRoundRect(0, 0, size, size, 16, 16);

        // 白色扫把/刷子形状（用简单图形代替）
        g.setColor(Color.WHITE);

        // 刷柄
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(22, 18, 40, 40);

        // 刷头
        g.fillRoundRect(36, 36, 18, 14, 4, 4);

        // 刷毛
        g.setColor(new Color(200, 220, 255));
        g.fillRoundRect(36, 48, 18, 6, 2, 2);

        g.dispose();
        return img;
    }

    /** 画一个 32x32 的窗口图标 */
    public static Image createWindowIcon() {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(30, 100, 200));
        g.fillRoundRect(0, 0, size, size, 8, 8);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(11, 9, 20, 20);
        g.fillRoundRect(18, 18, 9, 7, 2, 2);
        g.setColor(new Color(200, 220, 255));
        g.fillRoundRect(18, 24, 9, 3, 1, 1);

        g.dispose();
        return img;
    }
}