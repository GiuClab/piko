package app.morphe.extension.twitter.patches;

public final class VideoTapTrackerTest {
    private static final Object POST = new Object();
    private static boolean tap(VideoTapTracker t, Object post, long down, long up, float x, float y) {
        t.down(post, down, x, y);
        return t.up(up, x, y, 8, 40, 300, 500);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        VideoTapTracker t = new VideoTapTracker();
        check(!tap(t, POST, 0, 50, 100, 100), "Single tap must not like");
        check(tap(t, POST, 150, 200, 105, 100), "Nearby second tap must like");
        check(!tap(t, POST, 250, 280, 100, 100), "Third tap must start a new pair");
        t.cancel();
        tap(t, POST, 0, 50, 100, 100);
        check(!tap(t, new Object(), 150, 200, 100, 100), "Changing posts must cancel a pair");
        t.cancel();
        tap(t, POST, 0, 50, 100, 100);
        t.down(POST, 150, 100, 100);
        t.move(100, 130, 8);
        check(!t.up(200, 100, 100, 8, 40, 300, 500), "Swipe returning to its origin is not a tap");
        check(!tap(t, POST, 250, 280, 100, 100), "Swipe must also clear the preceding tap");
        t.cancel();
        tap(t, POST, 0, 50, 100, 100);
        check(!tap(t, POST, 500, 550, 100, 100), "Expired pair must not like");
        t.cancel();
        tap(t, POST, 0, 50, 100, 100);
        check(!tap(t, POST, 150, 200, 200, 100), "Distant taps must not like");
        t.cancel();
        tap(t, POST, 0, 50, 100, 100);
        check(!tap(t, POST, 150, 700, 100, 100), "Long press must not like");
        check(!tap(t, POST, 750, 800, 100, 100), "Long press must clear the preceding tap");
        t.cancel();
        tap(t, POST, 0, 50, 100, 100);
        t.cancel();
        check(!tap(t, POST, 150, 200, 100, 100), "Cancel/multi-touch must clear a pending pair");
        t.cancel();
        tap(t, POST, 0, 50, 100, 100);
        t.down(POST, 150, 100, 100);
        check(!t.up(200, 120, 100, 8, 40, 300, 500), "Movement on up must be checked even without a move event");
        System.out.println("VideoTapTracker: gesture cases passed");
    }
}
