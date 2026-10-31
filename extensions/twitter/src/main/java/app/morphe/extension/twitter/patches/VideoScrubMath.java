package app.morphe.extension.twitter.patches;

final class VideoScrubMath {
    static boolean starts(float dx, float dy, int slop) {
        return Math.abs(dx) > slop * 2 && Math.abs(dx) > Math.abs(dy) * 1.5f;
    }
    static float fraction(int progress, int max, float dx, float width) {
        if (max <= 0 || width <= 0) return 0;
        return Math.max(0f, Math.min(1f, progress / (float) max + dx / width));
    }
}
