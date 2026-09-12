import java.util.Timer;
import java.util.TimerTask;

public class AutoCleanScheduler {

    private final AppConfig config;
    private final Runnable cleanTask;
    private Timer timer;

    public AutoCleanScheduler(AppConfig config, Runnable cleanTask) {
        this.config = config;
        this.cleanTask = cleanTask;
    }

    /** 启动调度器 */
    public void start() {
        if (timer != null) timer.cancel();
        timer = new Timer("AutoCleanScheduler", true);
        System.out.println("[Scheduler] 启动，每小时检查一次");

        // 每 1 小时检查一次
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAndRun();
            }
        }, 0, 60 * 60 * 1000);
    }

    /** 停止调度器 */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /** 检查是否该清理 */
    private void checkAndRun() {
        boolean enabled = config.getAutoCleanEnabled();
        if (!enabled) return;

        long now = System.currentTimeMillis();
        long last = config.getAutoCleanLastTime();
        long intervalMs = getIntervalMs(config.getAutoCleanInterval());

        if (now - last >= intervalMs) {
            System.out.println("[Scheduler] 触发自动清理");
            config.setAutoCleanLastTime(now);
            config.save();
            try {
                cleanTask.run();
            } catch (Exception e) {
                System.err.println("自动清理失败：" + e.getMessage());
            }
        }
    }

    /** 间隔转毫秒 */
    private long getIntervalMs(String interval) {
        switch (interval) {
            case "every3days": return 3L * 24 * 60 * 60 * 1000;
            case "weekly":     return 7L * 24 * 60 * 60 * 1000;
            case "daily":
            default:           return 24L * 60 * 60 * 1000;
        }
    }
}