package app.morphe.extension.twitter.patches;

public final class VideoScrubMathTest {
    public static void main(String[] args) {
        check(VideoScrubMath.starts(30, 5, 8));
        check(VideoScrubMath.starts(-30, 5, 8));
        check(!VideoScrubMath.starts(10, 2, 8));
        check(!VideoScrubMath.starts(30, 30, 8));
        check(!VideoScrubMath.starts(5, 40, 8));
        check(VideoScrubMath.vertical(5, 30, 8));
        check(VideoScrubMath.vertical(5, -30, 8));
        check(!VideoScrubMath.vertical(30, 5, 8));
        check(!VideoScrubMath.vertical(2, 4, 8));
        near(VideoScrubMath.fraction(50, 100, 25, 100), .75f);
        near(VideoScrubMath.fraction(50, 100, -25, 100), .25f);
        near(VideoScrubMath.fraction(90, 100, 25, 100), 1f);
        near(VideoScrubMath.fraction(10, 100, -25, 100), 0f);
        near(VideoScrubMath.fraction(10, 0, 25, 100), 0f);
        near(VideoScrubMath.fraction(10, 100, 25, 0), 0f);
    }
    private static void check(boolean pass) { if (!pass) throw new AssertionError(); }
    private static void near(float a, float b) { check(Math.abs(a - b) < .0001f); }
}
