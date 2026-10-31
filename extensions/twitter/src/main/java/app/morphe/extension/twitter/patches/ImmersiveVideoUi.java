package app.morphe.extension.twitter.patches;

import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.twitter.patches.nativeFeatures.downloader.NativeDownloader;

/** Runtime for the opt-in portrait player prototype. All actions remain native X actions. */
@SuppressWarnings("unused")
public final class ImmersiveVideoUi {
    private static boolean enabled;
    private static final Map<Class<?>, Map<String, Field>> FIELDS = new ConcurrentHashMap<>();
    private static final Map<View, TouchState> TOUCH = new WeakHashMap<>();
    private static final String DOWNLOAD_TAG = "piko_video_rail_download";
    private static final String MORE_TAG = "piko_video_rail_more";

    // Rewritten from the target APK, rather than assuming obfuscated field names.
    private static String toggleFieldName() { return "pikoVideoToggleField"; }
    private static String tweetFieldName() { return "pikoVideoTweetField"; }

    public static void enable() { enabled = true; }

    public static boolean isRail(View view) {
        if (!enabled || view.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) return false;
        for (View current = view; current != null; ) {
            if ("chrome_portrait".equals(name(current))) return true;
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    /** Packed width/height for the injected onMeasure; -1 delegates to X unchanged. */
    public static long measure(ViewGroup bar, int widthSpec, int heightSpec) {
        if (!isRail(bar)) return -1;
        VideoChromePolicy.ensure(bar);
        VideoFollowControl.ensure(bar);
        ensureMoreButton(bar);
        VideoActionIcon.install(bar);
        int width = View.resolveSize(dp(bar, 56), widthSpec);
        int count = 0;
        for (int i = 0; i < bar.getChildCount(); i++) {
            if (bar.getChildAt(i).getVisibility() == View.VISIBLE) count++;
        }
        int desiredHeight = bar.getPaddingTop() + bar.getPaddingBottom() + count * dp(bar, 64);
        int height = View.resolveSize(desiredHeight, heightSpec);
        int cellHeight = count == 0 ? 0 : Math.max(0, height - bar.getPaddingTop() - bar.getPaddingBottom()) / count;
        int cellWidth = Math.max(0, width - bar.getPaddingLeft() - bar.getPaddingRight());
        for (int i = 0; i < bar.getChildCount(); i++) {
            View child = bar.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            child.measure(View.MeasureSpec.makeMeasureSpec(cellWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(cellHeight, View.MeasureSpec.EXACTLY));
        }
        return ((long) height << 32) | (width & 0xffffffffL);
    }

    public static boolean layout(ViewGroup bar) {
        if (!isRail(bar)) return false;
        int top = bar.getPaddingTop();
        List<View> items = new ArrayList<>();
        for (int i = 0; i < bar.getChildCount(); i++) {
            View child = bar.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) items.add(child);
            else child.layout(0, 0, 0, 0);
        }
        items.sort(Comparator.comparingInt(v -> actionOrder(v)));
        for (View child : items) {
            int left = bar.getPaddingLeft();
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
            top += child.getMeasuredHeight();
        }
        return true;
    }

    private static int actionOrder(View view) {
        if (VideoFollowControl.TAG.equals(view.getTag())) return -1;
        if (DOWNLOAD_TAG.equals(view.getTag())) return 9;
        if (MORE_TAG.equals(view.getTag())) return 10;
        switch (name(view)) {
            case "inline_like": return 0;
            case "inline_reply": return 1;
            case "inline_retweet": return 2;
            case "inline_bookmark": return 3;
            default: return 4;
        }
    }

    /** Native action views keep their listeners and checked state; only their geometry changes. */
    public static long measureAction(ViewGroup action, int widthSpec, int heightSpec) {
        if (!isRail(action)) return -1;
        try {
            ImageView icon = (ImageView) action.getClass().getMethod("getIconView").invoke(action);
            View count = (View) action.getClass().getMethod("getTextView").invoke(action);
            ViewGroup iconFrame = (ViewGroup) icon.getParent();
            int width = View.resolveSize(dp(action, 56), widthSpec);
            int height = View.resolveSize(dp(action, 64), heightSpec);
            int iconSize = dp(action, 34);
            ViewGroup.LayoutParams params = icon.getLayoutParams();
            if (params.width != iconSize || params.height != iconSize ||
                    !(params instanceof FrameLayout.LayoutParams) ||
                    ((FrameLayout.LayoutParams) params).gravity != Gravity.CENTER) {
                icon.setLayoutParams(new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
            }
            icon.setPadding(0, 0, 0, 0);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconFrame.setPadding(0, 0, 0, 0);
            iconFrame.setClipChildren(false);
            int frameSize = dp(action, 40);
            iconFrame.measure(exact(frameSize), exact(frameSize));
            styleCount(count);
            if (count.getVisibility() == View.VISIBLE) {
                count.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, width - dp(action, 4)), View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(dp(action, 20), View.MeasureSpec.AT_MOST));
            }
            for (int i = 0; i < action.getChildCount(); i++) {
                View child = action.getChildAt(i);
                if (child != count && child != iconFrame) child.measure(exact(frameSize), exact(frameSize));
            }
            return ((long) height << 32) | (width & 0xffffffffL);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    public static boolean layoutAction(ViewGroup action) {
        if (!isRail(action)) return false;
        try {
            View icon = (View) action.getClass().getMethod("getIconView").invoke(action);
            View count = (View) action.getClass().getMethod("getTextView").invoke(action);
            View frame = (View) icon.getParent();
            boolean hasCount = count.getVisibility() == View.VISIBLE && count.getMeasuredWidth() > 0;
            int total = frame.getMeasuredHeight() + (hasCount ? dp(action, 2) + count.getMeasuredHeight() : 0);
            int top = Math.max(0, (action.getHeight() - total) / 2);
            int left = (action.getWidth() - frame.getMeasuredWidth()) / 2;
            frame.layout(left, top, left + frame.getMeasuredWidth(), top + frame.getMeasuredHeight());
            for (int i = 0; i < action.getChildCount(); i++) {
                View child = action.getChildAt(i);
                if (child == count || child == frame) continue;
                child.layout(left, top, left + frame.getMeasuredWidth(), top + frame.getMeasuredHeight());
            }
            if (hasCount) {
                int countLeft = (action.getWidth() - count.getMeasuredWidth()) / 2;
                int countTop = top + frame.getMeasuredHeight() + dp(action, 2);
                count.layout(countLeft, countTop, countLeft + count.getMeasuredWidth(), countTop + count.getMeasuredHeight());
            } else count.layout(0, 0, 0, 0);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static int exact(int size) { return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY); }

    private static void styleCount(View count) {
        count.setPadding(0, 0, 0, 0);
        if (count instanceof TextView) {
            TextView text = (TextView) count;
            float targetSize = 12 * count.getResources().getDisplayMetrics().scaledDensity;
            if (Math.abs(text.getTextSize() - targetSize) > 0.5f) text.setTextSize(12);
            text.setTextColor(Color.WHITE);
            text.setIncludeFontPadding(false);
            text.setGravity(Gravity.CENTER);
            text.setShadowLayer(dp(count, 2), 0, dp(count, 1), Color.BLACK);
        } else if (count instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) count;
            for (int i = 0; i < group.getChildCount(); i++) styleCount(group.getChildAt(i));
        }
    }

    /** Called by the existing downloader only when that feature is enabled. */
    public static void addDownloadButton(ViewGroup bar) {
        if (bar.findViewWithTag(DOWNLOAD_TAG) != null) return;
        ImageView button = new ImageView(bar.getContext());
        button.setTag(DOWNLOAD_TAG);
        button.setContentDescription(NativeDownloader.downloadString());
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setPadding(dp(bar, 11), dp(bar, 15), dp(bar, 11), dp(bar, 15));
        int icon = bar.getResources().getIdentifier("ic_vector_incoming", "drawable", bar.getContext().getPackageName());
        button.setImageDrawable(VideoActionIcon.download());
        button.setColorFilter(Color.WHITE);
        button.setFocusable(true);
        button.setOnClickListener(v -> {
            try {
                Object tweet = readField(bar, tweetFieldName());
                if (tweet != null) NativeDownloader.downloader(bar.getContext(), tweet);
            } catch (Exception e) { Logger.printException(() -> "Video rail download failed", e); }
        });
        button.setOnLongClickListener(v -> {
            try {
                Object tweet = readField(bar, tweetFieldName());
                if (tweet != null) NativeDownloader.downloadAllFromTweet(bar.getContext(), tweet);
            } catch (Exception e) { Logger.printException(() -> "Video rail download-all failed", e); }
            return true;
        });
        bar.addView(button, new ViewGroup.LayoutParams(dp(bar, 56), dp(bar, 64)));
    }

    private static void ensureMoreButton(ViewGroup bar) {
        if (bar.findViewWithTag(MORE_TAG) != null) return;
        ImageView more = new ImageView(bar.getContext());
        more.setTag(MORE_TAG);
        more.setImageDrawable(VideoActionIcon.more());
        more.setScaleType(ImageView.ScaleType.FIT_CENTER);
        more.setPadding(dp(bar, 11), dp(bar, 15), dp(bar, 11), dp(bar, 15));
        more.setContentDescription("Altre opzioni del post");
        more.setFocusable(true);
        more.setOnClickListener(v -> {
            View chrome = bar;
            while (chrome.getParent() instanceof View && !"chrome_portrait".equals(name(chrome))) {
                chrome = (View) chrome.getParent();
            }
            View nativeMore = find(chrome, "tweet_curation_action");
            if (nativeMore != null && nativeMore.isEnabled()) nativeMore.performClick();
        });
        bar.addView(more, new ViewGroup.LayoutParams(dp(bar, 56), dp(bar, 64)));
    }

    /** 0: pass through; 1: consume; 2: consume and cancel the native gesture. */
    public static int onTouch(ViewGroup root, MotionEvent event) {
        if (!enabled) return 0;
        TouchState state = TOUCH.get(root);
        if (state != null && state.replaying) return 0;
        View video = find(root, "video_container");
        View chrome = find(root, "chrome_portrait");
        if (!(chrome instanceof ViewGroup) || video == null || !video.isShown() || !isRail(chrome)) {
            if (state != null) resetTouch(root, state);
            return 0;
        }
        if (state == null) { state = new TouchState(); TOUCH.put(root, state); }
        if (event.getPointerCount() != 1 || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            resetTouch(root, state);
            return 0;
        }
        View like = find(root, "inline_like");
        ViewGroup bar = findBar(chrome);
        if (like == null || bar == null || !like.isEnabled()) { resetTouch(root, state); return 0; }
        boolean nativeSuppressed = state.secondTap || state.horizontalLocked;
        try {
            Object tweet = readField(bar, tweetFieldName());
            if (tweet == null) { resetTouch(root, state); return 0; }
            Object key = postKey(tweet);
            if (!Objects.equals(state.post, key)) { resetTouch(root, state); state.post = key; }
            float x = event.getRawX(), y = event.getRawY();
            ViewConfiguration config = ViewConfiguration.get(root.getContext());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    Rect bounds = new Rect();
                    if (!video.getGlobalVisibleRect(bounds) || !bounds.contains((int) x, (int) y) ||
                            controlHit(root, root, video, (int) x, (int) y)) {
                        flushPending(root, state, true);
                        state.tracker.cancel();
                        state.surfaceDown = false;
                        recycleDown(state);
                        state.secondTap = false;
                        return 0;
                    }
                    state.surfaceDown = true;
                    state.horizontalLocked = false;
                    // Reserve the surface gesture before a parent can begin dismissing the player.
                    disallowInterception(root, true);
                    state.startX = x;
                    state.startY = y;
                    state.tracker.down(state.post, event.getEventTime(), x, y);
                    state.secondTap = state.pendingUp != null && state.tracker.isSecondDown(
                            config.getScaledDoubleTapSlop(), ViewConfiguration.getDoubleTapTimeout());
                    if (state.secondTap) {
                        // The first tap never reached native UP. Suppress the second native DOWN too,
                        // so native double-tap seeking cannot run before our like on UP.
                        flushPending(root, state, false);
                        return 2;
                    }
                    flushPending(root, state, true);
                    recycleDown(state);
                    state.downEvent = MotionEvent.obtain(event);
                    return 0;
                case MotionEvent.ACTION_MOVE:
                    if (state.horizontalLocked) {
                        if (state.scrubbing) scrub(state, event, MotionEvent.ACTION_MOVE);
                        return 1;
                    }
                    float dx = x - state.startX, dy = y - state.startY;
                    if (state.surfaceDown && !state.secondTap && VideoScrubMath.starts(dx, dy, config.getScaledTouchSlop())) {
                        state.horizontalLocked = true;
                        flushPending(root, state, false);
                        state.tracker.cancel();
                        android.widget.AbsSeekBar track = VideoChromePolicy.findTrack(find(chrome, "seekbar"));
                        if (track != null && track.isEnabled() && track.getMax() > 0 && track.getWidth() > 0) {
                            state.seekTrack = new java.lang.ref.WeakReference<>(track);
                            state.startProgress = track.getProgress();
                            state.scrubWidth = Math.max(1, video.getWidth());
                            state.scrubbing = true;
                            scrub(state, event, MotionEvent.ACTION_DOWN);
                            scrub(state, event, MotionEvent.ACTION_MOVE);
                        }
                        // Even an unavailable scrubber must never turn this drag into dismissal.
                        return 2;
                    }
                    if (state.surfaceDown && VideoScrubMath.vertical(dx, dy, config.getScaledTouchSlop())) {
                        state.surfaceDown = false;
                        disallowInterception(root, false);
                    }
                    state.tracker.move(x, y, config.getScaledTouchSlop());
                    // Do not expose tentative horizontal moves to X's surface-dismiss listener.
                    return state.secondTap || state.surfaceDown ? 1 : 0;
                case MotionEvent.ACTION_UP:
                    state.surfaceDown = false;
                    disallowInterception(root, false);
                    if (state.horizontalLocked) {
                        if (state.scrubbing) scrub(state, event, MotionEvent.ACTION_UP);
                        state.horizontalLocked = false;
                        state.scrubbing = false;
                        state.seekTrack = null;
                        recycleDown(state);
                        disallowInterception(root, false);
                        return 1;
                    }
                    boolean wasTracking = state.tracker.isTracking();
                    boolean doubleTap = state.tracker.up(event.getEventTime(), x, y,
                            config.getScaledTouchSlop(), config.getScaledDoubleTapSlop(),
                            ViewConfiguration.getDoubleTapTimeout(), ViewConfiguration.getLongPressTimeout());
                    if (state.secondTap) {
                        state.secondTap = false;
                        recycleDown(state);
                        if (!doubleTap) return 1;
                        // If this read fails, the native gesture remains consumed: never toggle off a like.
                        boolean liked = (Boolean) readField(like, toggleFieldName());
                        if (!liked) {
                            if (state.lastLikePost == state.post && event.getEventTime() - state.lastLikeTime < 1500) return 1;
                            if (!like.performClick()) return 1;
                            state.lastLikePost = state.post;
                            state.lastLikeTime = event.getEventTime();
                        }
                        showHeart(root, event.getX(), event.getY());
                        return 1;
                    }
                    if (wasTracking && state.tracker.hasPendingTap() && state.downEvent != null &&
                            event.getEventTime() - event.getDownTime() < ViewConfiguration.getLongPressTimeout()) {
                        state.pendingDown = state.downEvent;
                        state.downEvent = null;
                        state.pendingUp = MotionEvent.obtain(event);
                        state.pendingPost = state.post;
                        TouchState pendingState = state;
                        state.pendingReplay = () -> flushPending(root, pendingState, true);
                        root.postDelayed(state.pendingReplay, ViewConfiguration.getDoubleTapTimeout());
                        // Cancel native DOWN now. Confirm a single tap through X's actual play/pause
                        // button, rather than replaying X's video-surface gesture (which toggles chrome).
                        return 2;
                    }
                    recycleDown(state);
                    return 0;
                default:
                    return 0;
            }
        } catch (Exception e) {
            boolean consumedSequence = nativeSuppressed || state.secondTap || state.horizontalLocked;
            resetTouch(root, state);
            if (!state.reportedFailure) {
                state.reportedFailure = true;
                Logger.printException(() -> "Video double-tap disabled for this view: native state unavailable", e);
            }
            return consumedSequence ? 1 : 0;
        }
    }

    /** Use the native scrubber's touch callbacks so playback and time labels update together. */
    private static void scrub(TouchState state, MotionEvent source, int action) {
        android.widget.AbsSeekBar track = state.seekTrack == null ? null : state.seekTrack.get();
        if (track == null) return;
        float fraction = VideoScrubMath.fraction(state.startProgress, track.getMax(),
                action == MotionEvent.ACTION_DOWN ? 0 : source.getRawX() - state.startX, state.scrubWidth);
        float localX = track.getPaddingLeft() + fraction * (track.getWidth() - track.getPaddingLeft() - track.getPaddingRight());
        if (track.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) localX = track.getWidth() - localX;
        MotionEvent touch = MotionEvent.obtain(source.getDownTime(), source.getEventTime(), action,
                localX, track.getHeight() / 2f, 0);
        try { track.dispatchTouchEvent(touch); } finally { touch.recycle(); }
    }

    private static void recycleDown(TouchState state) {
        if (state.downEvent != null) { state.downEvent.recycle(); state.downEvent = null; }
    }

    /** Native CANCEL may release interception; restore ownership before the next MOVE. */
    public static void afterNativeCancel(ViewGroup root) {
        TouchState state = TOUCH.get(root);
        if (state != null && (state.horizontalLocked || state.secondTap || state.surfaceDown)) {
            disallowInterception(root, true);
        }
    }

    private static void disallowInterception(ViewGroup root, boolean disallow) {
        root.requestDisallowInterceptTouchEvent(disallow);
    }

    private static void resetTouch(ViewGroup root, TouchState state) {
        if (state.scrubbing) {
            MotionEvent cancel = MotionEvent.obtain(0, android.os.SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0);
            try { scrub(state, cancel, MotionEvent.ACTION_CANCEL); } finally { cancel.recycle(); }

        }
        disallowInterception(root, false);
        state.scrubbing = state.surfaceDown = state.horizontalLocked = false;
        state.seekTrack = null;
        flushPending(root, state, false);
        recycleDown(state);
        state.tracker.cancel();
        state.secondTap = false;
        state.post = null;
    }

    private static void flushPending(ViewGroup root, TouchState state, boolean replay) {
        if (state.pendingReplay != null) root.removeCallbacks(state.pendingReplay);
        MotionEvent down = state.pendingDown, up = state.pendingUp;
        Object post = state.pendingPost;
        state.pendingReplay = null;
        state.pendingDown = state.pendingUp = null;
        state.pendingPost = null;
        try {
            ViewGroup bar = findBar(root);
            if (replay && down != null && up != null && root.isAttachedToWindow() && root.isShown() &&
                    bar != null && isRail(bar) && Objects.equals(postKey(readField(bar, tweetFieldName())), post)) {
                View play = find(root, "video_play_toggle");
                if (play != null && play.isEnabled()) play.performClick();
            }
        } catch (Exception e) {
            Logger.printException(() -> "Video single-tap replay skipped", e);
        } finally {
            state.replaying = false;
            if (down != null) down.recycle();
            if (up != null) up.recycle();
        }
    }

    static Object currentTweet(ViewGroup bar) throws ReflectiveOperationException {
        return readField(bar, tweetFieldName());
    }

    private static Object postKey(Object tweet) throws ReflectiveOperationException {
        return tweet == null ? null : tweet.getClass().getMethod("getId").invoke(tweet);
    }

    private static Object readField(Object target, String name) throws ReflectiveOperationException {
        Map<String, Field> fields = FIELDS.computeIfAbsent(target.getClass(), ignored -> new ConcurrentHashMap<>());
        Field field = fields.get(name);
        if (field == null) {
            Class<?> type = target.getClass();
            while (type != null) {
                try { field = type.getDeclaredField(name); break; }
                catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
            }
            if (field == null) throw new NoSuchFieldException(name);
            field.setAccessible(true);
            fields.put(name, field);
        }
        return field.get(target);
    }

    static String name(View view) {
        try { return view.getId() == View.NO_ID ? "" : view.getResources().getResourceEntryName(view.getId()); }
        catch (Exception ignored) { return ""; }
    }

    static View find(View root, String resource) {
        if (resource.equals(name(root))) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), resource);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static ViewGroup findBar(View root) {
        if (root.getClass().getName().equals("com.twitter.ui.tweet.inlineactions.InlineActionBar")) return (ViewGroup) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                ViewGroup bar = findBar(group.getChildAt(i));
                if (bar != null) return bar;
            }
        }
        return null;
    }

    private static boolean controlHit(View root, View view, View video, int x, int y) {
        if (view == video || !view.isShown()) return false;
        Rect rect = new Rect();
        if (!view.getGlobalVisibleRect(rect) || !rect.contains(x, y)) return false;
        if (view != root && (view.isClickable() || view.isLongClickable())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                if (controlHit(root, group.getChildAt(i), video, x, y)) return true;
            }
        }
        return false;
    }

    private static int dp(View view, int value) { return Math.round(value * view.getResources().getDisplayMetrics().density); }

    private static void showHeart(ViewGroup root, float x, float y) {
        Heart heart = new Heart();
        int half = dp(root, 44);
        heart.setBounds((int) x - half, (int) y - half, (int) x + half, (int) y + half);
        root.getOverlay().add(heart);
        ValueAnimator animator = ValueAnimator.ofInt(255, 0);
        animator.setDuration(650);
        animator.addUpdateListener(a -> {
            heart.setAlpha((Integer) a.getAnimatedValue());
            if ((Integer) a.getAnimatedValue() == 0) root.getOverlay().remove(heart);
        });
        animator.start();
    }

    private static final class TouchState {
        final VideoTapTracker tracker = new VideoTapTracker();
        Object post, lastLikePost;
        long lastLikeTime;
        MotionEvent downEvent, pendingDown, pendingUp;
        Object pendingPost;
        Runnable pendingReplay;
        boolean secondTap, replaying, reportedFailure, surfaceDown, scrubbing, horizontalLocked;
        float startX, startY, scrubWidth;
        int startProgress;
        java.lang.ref.WeakReference<android.widget.AbsSeekBar> seekTrack;
    }

    private static final class Heart extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        Heart() {
            paint.setColor(Color.rgb(255, 55, 100));
            path.moveTo(50, 90);
            path.cubicTo(40, 80, 5, 55, 5, 30);
            path.cubicTo(5, 5, 38, 0, 50, 22);
            path.cubicTo(62, 0, 95, 5, 95, 30);
            path.cubicTo(95, 55, 60, 80, 50, 90);
            path.close();
        }
        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            int saved = canvas.save();
            canvas.translate(b.left, b.top);
            canvas.scale(b.width() / 100f, b.height() / 100f);
            canvas.drawPath(path, paint);
            canvas.restoreToCount(saved);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
