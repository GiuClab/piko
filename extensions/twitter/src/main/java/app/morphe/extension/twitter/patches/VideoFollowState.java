package app.morphe.extension.twitter.patches;

/** Relationship flags verified against X 12.19.1's native Friendship implementation. */
final class VideoFollowState {
    static final int UNKNOWN = 0, AVAILABLE = 1, FOLLOWING = 2, REQUESTED = 3, BLOCKED = 4, SELF = 5;
    static int decode(Integer flags, boolean self) {
        if (self) return SELF;
        if (flags == null) return UNKNOWN;
        if ((flags & (4 | 32768)) != 0) return BLOCKED;
        if ((flags & 1) != 0) return FOLLOWING;
        if ((flags & 16384) != 0) return REQUESTED;
        if ((flags & 128) != 0) return UNKNOWN;
        return AVAILABLE;
    }
}
