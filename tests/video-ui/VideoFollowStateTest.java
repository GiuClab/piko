package app.morphe.extension.twitter.patches;

public final class VideoFollowStateTest {
    private static void expect(int flags, int expected, String message) {
        if (VideoFollowState.decode(flags, false) != expected) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        if (VideoFollowState.decode(null, false) != VideoFollowState.UNKNOWN)
            throw new AssertionError("Cache miss must not enable follow or claim the profile is unfollowed");
        expect(128, VideoFollowState.UNKNOWN, "Unknown native following state must remain unknown");
        expect(0, VideoFollowState.AVAILABLE, "Confirmed not-followed state permits a follow request");
        expect(2, VideoFollowState.AVAILABLE, "A profile following us does not mean we follow it");
        expect(1, VideoFollowState.FOLLOWING, "Already followed profiles must not get a toggle action");
        expect(16384, VideoFollowState.REQUESTED, "A protected-profile request is not a completed follow");
        expect(16384 | 128, VideoFollowState.REQUESTED, "A confirmed pending request blocks duplicate requests");
        expect(4 | 1, VideoFollowState.BLOCKED, "A blocked profile must not be unblocked or toggled");
        expect(32768, VideoFollowState.BLOCKED, "Blocked-by state must not permit follow");
        if (VideoFollowState.decode(0, true) != VideoFollowState.SELF)
            throw new AssertionError("Do not offer follow on the current account");
        System.out.println("VideoFollowState: relationship cases passed");
    }
}
