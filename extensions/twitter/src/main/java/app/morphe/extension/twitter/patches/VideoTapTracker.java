package app.morphe.extension.twitter.patches;

/** Gesture bookkeeping without Android or view references. One instance per visible tweet view. */
final class VideoTapTracker {
    private Object post;
    private boolean tracking;
    private long downTime;
    private float downX, downY;
    private long previousUp = -1;
    private float previousX, previousY;

    void down(Object currentPost, long time, float x, float y) {
        if (post != currentPost) cancel();
        post = currentPost;
        tracking = true;
        downTime = time;
        downX = x;
        downY = y;
    }

    void move(float x, float y, int slop) {
        if (distanceSquared(x, y, downX, downY) > (long) slop * slop) cancel();
    }

    boolean up(long time, float x, float y, int touchSlop, int doubleSlop, int doubleTimeout, int longTimeout) {
        if (!tracking) return false;
        tracking = false;
        if (time < downTime || time - downTime >= longTimeout ||
                distanceSquared(x, y, downX, downY) > (long) touchSlop * touchSlop) {
            cancel();
            return false;
        }
        boolean doubleTap = previousUp >= 0 && downTime >= previousUp &&
                downTime - previousUp <= doubleTimeout &&
                distanceSquared(downX, downY, previousX, previousY) <= (long) doubleSlop * doubleSlop;
        previousUp = doubleTap ? -1 : time;
        previousX = x;
        previousY = y;
        return doubleTap;
    }

    void cancel() {
        tracking = false;
        previousUp = -1;
        post = null;
    }

    private static float distanceSquared(float x, float y, float a, float b) {
        float dx = x - a, dy = y - b;
        return dx * dx + dy * dy;
    }
}
