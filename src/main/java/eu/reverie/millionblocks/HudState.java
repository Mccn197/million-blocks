package eu.reverie.millionblocks;

import java.util.Locale;

/** Immutable values safe to read from BetterHud's rendering threads. */
public record HudState(long count, long target, int multiplier, boolean visible,
                       boolean showGoal, boolean showPercent, boolean showMultiplier,
                       boolean showIcon, long lastMineNanos,
                       String nextItemId, String nextItemName, long stageProgress,
                       long stageGoal, int stagePercent) {
    public static final HudState EMPTY = new HudState(0, 1, 1, false, false, false, false, false, 0,
            "complete", "Tous les items débloqués", 0, 1, 100);
    // Preserve Java callers and old tests that construct the pre-unlock state.
    public HudState(long count, long target, int multiplier, boolean visible, boolean showGoal,
            boolean showPercent, boolean showMultiplier, boolean showIcon, long lastMineNanos) {
        this(count,target,multiplier,visible,showGoal,showPercent,showMultiplier,showIcon,lastMineNanos,
             "complete","Tous les items débloqués",0,1,100);
    }
    public HudState {
        count = Math.max(0, count);
        target = Math.max(1, target);
        multiplier = multiplier == 2 ? 2 : 1;
        stageGoal = Math.max(1,stageGoal);
        stageProgress = Math.max(0,Math.min(stageGoal,stageProgress));
        stagePercent = Math.max(0,Math.min(100,stagePercent));
    }
    public int percent() { return (int) Math.min(100, 100d * count / target); }
    public int progressFrame(){return (int)Math.min(20, Math.floor(20d*count/target));}
    public int unlockFrame(){return (int)Math.min(20,Math.floor(20d*stageProgress/stageGoal));}
    public String unlockText(){return number(stageProgress) + " / "+ number(stageGoal);}
    public String unlockGoalText(){return number(stageGoal);}
    public String nextItemText(){return nextItemName;}
    public String countText() { return number(count); }
    public String goalText() { return showGoal ? "/ " + number(target) : ""; }
    public String percentText() { return showPercent ? percent() + "%" : ""; }
    public String multiplierText() { return showMultiplier ? "x" + multiplier : ""; }
    public int crack(long now) {
        long elapsed = now - lastMineNanos;
        return lastMineNanos == 0 || elapsed < 0 || elapsed >= 600_000_000L ? 0
                : 1 + (int) (elapsed / 150_000_000L);
    }
    public static long animationStart(long previous, long now) {
        return previous == 0 || now - previous >= 600_000_000L ? now : previous;
    }
    public static long addSaturated(long before, long amount, int multiplier) {
        if (amount <= 0) return Math.max(0, before);
        int factor = multiplier == 2 ? 2 : 1;
        if (amount > (Long.MAX_VALUE - Math.max(0, before)) / factor) return Long.MAX_VALUE;
        return Math.max(0, before) + amount * factor;
    }
    private static String number(long n) { return String.format(Locale.US, "%,d", n).replace(',', ' '); }
}
