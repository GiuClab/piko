package app.morphe.extension.twitter.patches;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.morphe.extension.shared.Logger;

/** Reuses X's activity-scoped relationship cache and its follow-only native request. */
final class VideoFollowControl extends LinearLayout {
    static final String TAG = "piko_video_follow";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, Pending> IN_FLIGHT = new ConcurrentHashMap<>();
    private final WeakReference<ViewGroup> barRef;
    private final TextView label;
    private final Glyph glyph = new Glyph();
    private final AvatarPhoto avatar;
    private final Badge badge;
    private Data shown;
    private boolean loggedFailure;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (!isAttachedToWindow()) return;
            update();
            postDelayed(this, 1000);
        }
    };

    static void ensure(ViewGroup bar) {
        View existing = bar.findViewWithTag(TAG);
        if (existing instanceof VideoFollowControl) return;
        bar.addView(new VideoFollowControl(bar), new ViewGroup.LayoutParams(dp(bar, 72), dp(bar, 64)));
    }

    private VideoFollowControl(ViewGroup bar) {
        super(bar.getContext());
        barRef = new WeakReference<>(bar);
        setTag(TAG);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setFocusable(true);
        FrameLayout profile = new FrameLayout(getContext());
        avatar = new AvatarPhoto(getContext());
        avatar.setContentDescription("Apri il profilo");
        avatar.setOnClickListener(v -> {
            View source = sourceAvatar();
            if (source != null) source.performClick();
        });
        profile.addView(avatar, new FrameLayout.LayoutParams(dp(this, 40), dp(this, 40), Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        badge = new Badge(getContext());
        badge.setOnClickListener(v -> follow());
        profile.addView(badge, new FrameLayout.LayoutParams(dp(this, 18), dp(this, 18), Gravity.BOTTOM | Gravity.END));
        addView(profile, new LinearLayout.LayoutParams(dp(this, 44), dp(this, 44)));
        label = new TextView(getContext());
        label.setTextColor(Color.WHITE);
        label.setTextSize(11);
        label.setIncludeFontPadding(false);
        label.setGravity(Gravity.CENTER);
        label.setShadowLayer(dp(this, 2), 0, dp(this, 1), Color.BLACK);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.topMargin = dp(this, 2);
        addView(label, textParams);
        setOnClickListener(v -> follow());
        update();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(refresh);
        post(refresh);
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(refresh);
        super.onDetachedFromWindow();
    }

    private void update() {
        ViewGroup bar = barRef.get();
        if (bar == null) return;
        int state = VideoFollowState.UNKNOWN;
        String text = "Stato…", handle = "";
        boolean canFollow = false;
        try {
            Data data = data(bar);
            shown = data;
            state = VideoFollowState.decode(data.flags, data.owner == data.author);
            handle = data.handle;
            if (IN_FLIGHT.containsKey(data.key())) text = "Invio…";
            else if (state == VideoFollowState.AVAILABLE) { text = "Segui"; canFollow = true; }
            else if (state == VideoFollowState.FOLLOWING) text = "Segui già";
            else if (state == VideoFollowState.REQUESTED) text = "Richiesta";
            else if (state == VideoFollowState.BLOCKED) text = "Bloccato";
            else if (state == VideoFollowState.SELF) text = "Tu";
        } catch (ReflectiveOperationException | IllegalStateException e) {
            shown = null;
            if (!loggedFailure) {
                loggedFailure = true;
                Logger.printException(() -> "Video follow state unavailable", e);
            }
        }
        if (!text.contentEquals(label.getText())) label.setText(text);
        setEnabled(canFollow);
        // Even a disabled status tile must not turn touches into video-surface gestures.
        setClickable(true);
        setContentDescription(text + (handle.isEmpty() ? "" : " @" + handle));
        glyph.state = state;
        glyph.invalidateSelf();
        avatar.invalidate();
        badge.setEnabled(canFollow);
        badge.setClickable(true);
        badge.setContentDescription(text + (handle.isEmpty() ? "" : " @" + handle));
        badge.invalidate();
    }

    private View sourceAvatar() {
        ViewGroup bar = barRef.get();
        if (bar == null) return null;
        View chrome = bar;
        while (chrome.getParent() instanceof View && !"chrome_portrait".equals(resourceName(chrome))) {
            chrome = (View) chrome.getParent();
        }
        return ImmersiveVideoUi.find(chrome, "tweet_profile_image");
    }

    private final class AvatarPhoto extends View {
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        AvatarPhoto(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            View source = sourceAvatar();
            boolean drawn = false;
            if (source != null && source.getWidth() > 0 && source.getHeight() > 0) {
                int save = canvas.save();
                try {
                    float pad = dp(this, 1);
                    Path circle = new Path();
                    circle.addCircle(getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()) / 2f - pad, Path.Direction.CW);
                    canvas.clipPath(circle);
                    canvas.translate(pad, pad);
                    canvas.scale((getWidth() - 2 * pad) / source.getWidth(), (getHeight() - 2 * pad) / source.getHeight());
                    // Reuse the already loaded native image. Its footer container is transparent,
                    // while this source child retains its normal size, data and profile listener.
                    source.draw(canvas);
                    drawn = true;
                } catch (RuntimeException ignored) { }
                finally { canvas.restoreToCount(save); }
            }
            if (!drawn) {
                glyph.setBounds(0, 0, getWidth(), getHeight());
                glyph.draw(canvas);
            }
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(dp(this, 1));
            border.setColor(Color.WHITE);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()) / 2f - dp(this, 1), border);
        }
    }

    private final class Badge extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Badge(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(glyph.state == VideoFollowState.FOLLOWING ? 0xff28ab80 : glyph.state == VideoFollowState.AVAILABLE ? 0xfffe2c55 : 0xff777777);
            canvas.drawCircle(cx, cy, Math.min(cx, cy), paint);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(dp(this, 2));
            if (glyph.state == VideoFollowState.FOLLOWING) {
                canvas.drawLine(cx - dp(this, 4), cy, cx - dp(this, 1), cy + dp(this, 3), paint);
                canvas.drawLine(cx - dp(this, 1), cy + dp(this, 3), cx + dp(this, 4), cy - dp(this, 3), paint);
            } else if (glyph.state == VideoFollowState.REQUESTED) {
                canvas.drawLine(cx, cy - dp(this, 4), cx, cy, paint);
                canvas.drawLine(cx, cy, cx + dp(this, 3), cy + dp(this, 2), paint);
            } else {
                canvas.drawLine(cx - dp(this, 4), cy, cx + dp(this, 4), cy, paint);
                canvas.drawLine(cx, cy - dp(this, 4), cx, cy + dp(this, 4), paint);
            }
        }
    }

    private void follow() {
        ViewGroup bar = barRef.get();
        if (bar == null || shown == null) return;
        try {
            Data fresh = data(bar);
            // The click must still refer to the author and account shown by this tile.
            if (fresh.owner != shown.owner || fresh.author != shown.author ||
                    VideoFollowState.decode(fresh.flags, fresh.owner == fresh.author) != VideoFollowState.AVAILABLE) {
                update();
                return;
            }
            Class<?> userIdentifier = Class.forName("com.twitter.util.user.UserIdentifier");
            Class<?> requestType = Class.forName("com.twitter.api.legacy.request.user.f");
            Object request = requestType.getConstructor(Context.class, userIdentifier, long.class,
                    Class.forName("com.twitter.model.core.entity.ad.h")).newInstance(
                    getContext().getApplicationContext(), fresh.actor, fresh.author, null);
            Pending pending = new Pending(fresh, request, this);
            if (IN_FLIGHT.putIfAbsent(fresh.key(), pending) != null) return;
            try {
                Class<?> clientType = Class.forName("com.twitter.async.http.g");
                Object client = clientType.getMethod("d").invoke(null);
                if (client == null) throw new IllegalStateException("Native request queue unavailable");
                clientType.getMethod("g", Class.forName("com.twitter.async.http.a")).invoke(client, request);
            } catch (ReflectiveOperationException | IllegalStateException e) {
                IN_FLIGHT.remove(fresh.key(), pending);
                throw e;
            }
            MAIN.postDelayed(pending, 100);
            update();
        } catch (ReflectiveOperationException | IllegalStateException e) {
            Logger.printException(() -> "Native video follow request could not be started", e);
            update();
        }
    }

    private static Data data(ViewGroup bar) throws ReflectiveOperationException {
        Activity activity = null;
        for (View cursor = bar; cursor != null; cursor = cursor.getParent() instanceof View ? (View) cursor.getParent() : null) {
            activity = activity(cursor.getContext());
            if (activity != null) break;
        }
        Class<?> activityType = Class.forName("com.twitter.app.common.inject.p");
        if (activity == null || !activityType.isInstance(activity)) throw new IllegalStateException("No native activity scope");
        Object actor = Class.forName("com.twitter.util.user.UserIdentifier$Companion").getMethod("c").invoke(null);
        if (actor == null) throw new IllegalStateException("No current native account");
        long owner = ((Number) actor.getClass().getMethod("getId").invoke(actor)).longValue();
        Object cache = Class.forName("com.twitter.cache.twitteruser.di.a").getMethod("a", activityType).invoke(null, activity);
        Object tweet = ImmersiveVideoUi.currentTweet(bar);
        if (tweet == null) throw new IllegalStateException("No bound tweet");
        long author = ((Number) tweet.getClass().getMethod("C").invoke(tweet)).longValue();
        String handle = (String) tweet.getClass().getMethod("D").invoke(tweet);
        if (author <= 0 || owner <= 0 || handle == null || handle.isEmpty()) throw new IllegalStateException("No verified author");
        View chrome = bar;
        while (chrome.getParent() instanceof View && !"chrome_portrait".equals(resourceName(chrome))) {
            chrome = (View) chrome.getParent();
        }
        View username = ImmersiveVideoUi.find(chrome, "username");
        if (!(username instanceof TextView) || !normalize(((TextView) username).getText().toString()).equalsIgnoreCase(normalize(handle))) {
            throw new IllegalStateException("Header author does not match follow target");
        }
        Integer flags = (Integer) cache.getClass().getMethod("c", long.class).invoke(cache, author);
        if (flags == null || (flags & 128) != 0) {
            // Read only this account's cached author record; never infer "not followed" from a cache miss.
            Class<?> dbType = Class.forName("com.twitter.database.legacy.tdbh.b0");
            Object db = dbType.getMethod("f1", Class.forName("com.twitter.util.user.UserIdentifier")).invoke(null, actor);
            Object user = dbType.getMethod("d1", long.class).invoke(db, author);
            if (user != null && ((Number) user.getClass().getMethod("getId").invoke(user)).longValue() == author) {
                cache.getClass().getMethod("j", Class.forName("com.twitter.model.core.entity.k1")).invoke(cache, user);
                flags = (Integer) cache.getClass().getMethod("c", long.class).invoke(cache, author);
            }
        }
        return new Data(actor, cache, owner, author, handle, flags);
    }

    private static String normalize(String value) {
        String v = value.replace("\u200e", "").replace("\u200f", "").trim();
        return v.startsWith("@") ? v.substring(1) : v;
    }

    private static Activity activity(Context context) {
        for (int i = 0; i < 20; i++) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) return null;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) return null;
            context = next;
        }
        return null;
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        while (type != null) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
            catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static String resourceName(View view) {
        try { return view.getResources().getResourceEntryName(view.getId()); }
        catch (Exception ignored) { return ""; }
    }

    private static int dp(View view, int value) { return Math.round(value * view.getResources().getDisplayMetrics().density); }

    private static final class Data {
        final Object actor, cache;
        final long owner, author;
        final String handle;
        final Integer flags;
        Data(Object actor, Object cache, long owner, long author, String handle, Integer flags) {
            this.actor = actor; this.cache = cache; this.owner = owner; this.author = author;
            this.handle = handle; this.flags = flags;
        }
        String key() { return owner + ":" + author; }
    }

    /** Publish a followed state only after X provides an actual successful user response. */
    private static final class Pending implements Runnable {
        final Data data;
        final Object request;
        final long deadline = SystemClock.uptimeMillis() + 30000;
        final java.lang.ref.WeakReference<VideoFollowControl> control;
        Pending(Data data, Object request, VideoFollowControl view) {
            this.data = data; this.request = request;
            control = new java.lang.ref.WeakReference<>(view);
        }
        @Override public void run() {
            boolean done = false;
            try {
                Object user = field(request, "M");
                if (user != null) {
                    long returnedId = ((Number) user.getClass().getMethod("getId").invoke(user)).longValue();
                    if (returnedId == data.author) {
                        data.cache.getClass().getMethod("j", Class.forName("com.twitter.model.core.entity.k1")).invoke(data.cache, user);
                    }
                    done = true;
                } else if (field(request, "K") != null) done = true;
            } catch (ReflectiveOperationException e) {
                done = true;
                Logger.printException(() -> "Video follow confirmation unavailable", e);
            }
            if (done || SystemClock.uptimeMillis() >= deadline) {
                IN_FLIGHT.remove(data.key(), this);
                VideoFollowControl view = control.get();
                if (view != null && view.isAttachedToWindow()) view.update();
            }
            else MAIN.postDelayed(this, 100);
        }
    }

    private static final class Glyph extends Drawable {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int state = VideoFollowState.UNKNOWN;
        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            int save = canvas.save();
            canvas.translate(b.left, b.top);
            canvas.scale(b.width() / 100f, b.height() / 100f);
            Path body = new Path();
            body.moveTo(12, 87); body.cubicTo(12, 59, 63, 59, 63, 87);
            for (int pass = 0; pass < 2; pass++) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(pass == 0 ? 10 : 5);
                paint.setColor(pass == 0 ? 0x99000000 : Color.WHITE);
                canvas.drawCircle(38, 28, 16, paint);
                canvas.drawPath(body, paint);
            }
            paint.setStrokeWidth(7);
            paint.setColor(state == VideoFollowState.FOLLOWING ? 0xff5bdfa5 : 0xffff476f);
            if (state == VideoFollowState.FOLLOWING) {
                canvas.drawLine(69, 70, 78, 80, paint);
                canvas.drawLine(78, 80, 94, 58, paint);
            } else {
                canvas.drawLine(70, 70, 94, 70, paint);
                canvas.drawLine(82, 58, 82, 82, paint);
            }
            canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
