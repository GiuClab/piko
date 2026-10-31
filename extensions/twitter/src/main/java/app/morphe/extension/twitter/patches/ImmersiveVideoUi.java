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
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import java.lang.reflect.Field;
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
        int width = View.resolveSize(dp(bar, 72), widthSpec);
        int count = 0;
        for (int i = 0; i < bar.getChildCount(); i++) {
            if (bar.getChildAt(i).getVisibility() == View.VISIBLE) count++;
        }
        int desiredHeight = bar.getPaddingTop() + bar.getPaddingBottom() + count * dp(bar, 56);
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
        for (int i = 0; i < bar.getChildCount(); i++) {
            View child = bar.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) {
                child.layout(0, 0, 0, 0);
                continue;
            }
            int left = bar.getPaddingLeft();
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
            top += child.getMeasuredHeight();
        }
        return true;
    }

    /** Called by the existing downloader only when that feature is enabled. */
    public static void addDownloadButton(ViewGroup bar) {
        if (bar.findViewWithTag(DOWNLOAD_TAG) != null) return;
        ImageView button = new ImageView(bar.getContext());
        button.setTag(DOWNLOAD_TAG);
        button.setContentDescription(NativeDownloader.downloadString());
        button.setScaleType(ImageView.ScaleType.CENTER);
        int icon = bar.getResources().getIdentifier("ic_vector_incoming", "drawable", bar.getContext().getPackageName());
        if (icon != 0) button.setImageResource(icon);
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
        bar.addView(button, new ViewGroup.LayoutParams(dp(bar, 72), dp(bar, 56)));
    }

    /** Observe ordinary gestures; consume only the completed second tap of a native like. */
    public static boolean onTouch(ViewGroup root, MotionEvent event) {
        if (!enabled) return false;
        View video = find(root, "video_container");
        View chrome = find(root, "chrome_portrait");
        if (!(chrome instanceof ViewGroup) || video == null || !video.isShown() || !isRail(chrome)) return false;
        TouchState state = TOUCH.get(root);
        if (state == null) { state = new TouchState(); TOUCH.put(root, state); }
        if (event.getPointerCount() != 1 || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            state.tracker.cancel();
            return false;
        }
        View like = find(root, "inline_like");
        ViewGroup bar = findBar(chrome);
        if (like == null || bar == null || !like.isEnabled()) { state.tracker.cancel(); return false; }
        try {
            Object tweet = readField(bar, tweetFieldName());
            if (tweet == null) { state.tracker.cancel(); return false; }
            if (state.post != tweet) { state.tracker.cancel(); state.post = tweet; }
            float x = event.getRawX(), y = event.getRawY();
            ViewConfiguration config = ViewConfiguration.get(root.getContext());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    Rect bounds = new Rect();
                    if (!video.getGlobalVisibleRect(bounds) || !bounds.contains((int) x, (int) y) ||
                            controlHit(root, root, video, (int) x, (int) y)) {
                        state.tracker.cancel();
                    } else {
                        state.tracker.down(tweet, event.getEventTime(), x, y);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    state.tracker.move(x, y, config.getScaledTouchSlop());
                    break;
                case MotionEvent.ACTION_UP:
                    if (!state.tracker.up(event.getEventTime(), x, y, config.getScaledTouchSlop(),
                            config.getScaledDoubleTapSlop(), ViewConfiguration.getDoubleTapTimeout(),
                            ViewConfiguration.getLongPressTimeout())) break;
                    // If the native state cannot be read, do nothing: never risk toggling off a like.
                    boolean liked = (Boolean) readField(like, toggleFieldName());
                    if (!liked) {
                        if (state.lastLikePost == tweet && event.getEventTime() - state.lastLikeTime < 1500) return true;
                        if (!like.performClick()) return false;
                        state.lastLikePost = tweet;
                        state.lastLikeTime = event.getEventTime();
                    }
                    showHeart(root, event.getX(), event.getY());
                    return true;
                default:
                    break;
            }
        } catch (Exception e) {
            state.tracker.cancel();
            if (!state.reportedFailure) {
                state.reportedFailure = true;
                Logger.printException(() -> "Video double-tap disabled for this view: native state unavailable", e);
            }
        }
        return false;
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

    private static String name(View view) {
        try { return view.getId() == View.NO_ID ? "" : view.getResources().getResourceEntryName(view.getId()); }
        catch (Exception ignored) { return ""; }
    }

    private static View find(View root, String resource) {
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
        boolean reportedFailure;
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
