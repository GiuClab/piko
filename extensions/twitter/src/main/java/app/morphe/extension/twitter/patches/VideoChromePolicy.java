package app.morphe.extension.twitter.patches;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Reassert portrait-only visibility after native binders update a recycled video view. */
final class VideoChromePolicy implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
    private static final Map<ViewGroup, VideoChromePolicy> INSTALLED = new WeakHashMap<>();
    private final WeakReference<ViewGroup> barRef;
    private WeakReference<ViewTreeObserver> observer = new WeakReference<>(null);

    static void ensure(ViewGroup bar) {
        if (INSTALLED.containsKey(bar)) return;
        VideoChromePolicy policy = new VideoChromePolicy(bar);
        INSTALLED.put(bar, policy);
        bar.addOnAttachStateChangeListener(policy);
        if (bar.isAttachedToWindow()) policy.onViewAttachedToWindow(bar);
        policy.apply();
    }

    private VideoChromePolicy(ViewGroup bar) { barRef = new WeakReference<>(bar); }

    private boolean apply() {
        ViewGroup bar = barRef.get();
        if (bar == null || !ImmersiveVideoUi.isRail(bar)) return false;
        View chrome = bar;
        while (chrome.getParent() instanceof View && !"chrome_portrait".equals(ImmersiveVideoUi.name(chrome))) {
            chrome = (View) chrome.getParent();
        }
        // Position is independent of the TweetView visibility guard used for auto-hide.
        positionRail(bar);
        SafeMediaHooks.installComments(bar);
        // X fades/hides the chrome after inactivity. Restore only this visible portrait
        // player's groups, leaving per-action availability and other screens untouched.
        View owner = chrome;
        while (owner.getParent() instanceof View &&
                !isTweetOwner(owner)) {
            owner = (View) owner.getParent();
        }
        android.graphics.Rect bounds = new android.graphics.Rect();
        if (!isTweetOwner(owner) || !owner.isShown() || !owner.getGlobalVisibleRect(bounds)) return false;
        pin(chrome);
        for (View current = bar; current != chrome && current != null;
                current = current.getParent() instanceof View ? (View) current.getParent() : null) pin(current);
        for (String id : new String[]{"tweet_header", "name", "seekbar", "video_play_toggle", "playback_speed_button", "video_mute_toggle", "video_pip_button"}) {
            pin(ImmersiveVideoUi.find(chrome, id));
        }
        pin(ImmersiveVideoUi.find(owner, "chrome_top"));
        boolean changed = false;
        for (String id : new String[]{"tweet_content_text", "text_separator", "promoted_badge", "tweet_edit_badge"}) {
            View view = ImmersiveVideoUi.find(chrome, id);
            if (view != null && view.getVisibility() != View.GONE) {
                view.setVisibility(View.GONE);
                changed = true;
            }
        }
        View username = ImmersiveVideoUi.find(chrome, "username");
        if (username != null && username.getAlpha() != 0) username.setAlpha(0);
        View avatar = ImmersiveVideoUi.find(chrome, "tweet_profile_image_container");
        if (avatar != null && avatar.getAlpha() != 0) avatar.setAlpha(0);
        View more = ImmersiveVideoUi.find(chrome, "tweet_curation_action");
        if (more != null && more.getAlpha() != 0) more.setAlpha(0);
        View play = ImmersiveVideoUi.find(chrome, "video_play_toggle");
        if (play != null && !play.isClickable()) play.setClickable(true);
        alignPlayback(chrome);
        VideoActionIcon.install(bar);
        return changed;
    }

    /** Align with the actual track, not the wrapper that also contains the time label. */
    private static void alignPlayback(View chrome) {
        View wrapper = ImmersiveVideoUi.find(chrome, "seekbar");
        View play = ImmersiveVideoUi.find(chrome, "video_play_toggle");
        android.widget.AbsSeekBar track = findTrack(wrapper);
        if (track == null || play == null || wrapper.getWidth() == 0 || track.getHeight() == 0) return;
        track.setPadding(0, track.getPaddingTop(), 0, track.getPaddingBottom());
        if (track.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) track.getParent();
            // Native internal margins must not shorten the visible progress line.
            int width = parent.getWidth();
            if (width > 0 && (track.getLeft() != 0 || track.getWidth() != width)) {
                track.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(track.getHeight(), View.MeasureSpec.EXACTLY));
                track.layout(0, track.getTop(), width, track.getBottom());
            }
        }
        int[] trackPosition = new int[2];
        int[] playPosition = new int[2];
        track.getLocationInWindow(trackPosition);
        play.getLocationInWindow(playPosition);
        float trackCenter = trackPosition[1] + track.getPaddingTop()
                + (track.getHeight() - track.getPaddingTop() - track.getPaddingBottom()) / 2f;
        float delta = trackCenter - (playPosition[1] + play.getHeight() / 2f);
        if (Math.abs(delta) > 0.5f) play.setTranslationY(play.getTranslationY() + delta);
    }

    static android.widget.AbsSeekBar findTrack(View view) {
        if (view instanceof android.widget.AbsSeekBar) return (android.widget.AbsSeekBar) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.widget.AbsSeekBar track = findTrack(group.getChildAt(i));
                if (track != null) return track;
            }
        }
        return null;
    }

    /** Position the XML host's direct child, which can wrap the hooked InlineActionBar. */
    static void positionRail(ViewGroup bar) {
        if (!ImmersiveVideoUi.isRail(bar)) return;
        View chrome = bar;
        while (chrome.getParent() instanceof View &&
                !"chrome_portrait".equals(ImmersiveVideoUi.name(chrome))) {
            chrome = (View) chrome.getParent();
        }
        View found = ImmersiveVideoUi.find(chrome, "piko_video_action_container");
        if (!(found instanceof android.widget.FrameLayout)) return;
        android.widget.FrameLayout host = (android.widget.FrameLayout) found;
        if (host.getChildCount() != 1) return;
        View railRoot = host.getChildAt(0);
        if (!(railRoot.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams)) return;
        boolean left = "left".equals(app.morphe.extension.twitter.Utils.getStringPref(
                app.morphe.extension.twitter.settings.Settings.VIDEO_ACTION_SIDE));
        android.widget.FrameLayout.LayoutParams params =
                (android.widget.FrameLayout.LayoutParams) railRoot.getLayoutParams();
        int gravity = android.view.Gravity.TOP | (left ? android.view.Gravity.LEFT : android.view.Gravity.RIGHT);
        if (bar.getTranslationX() != 0f) bar.setTranslationX(0f);
        if (railRoot.getTranslationX() != 0f) railRoot.setTranslationX(0f);
        if (params.gravity != gravity) {
            params.gravity = gravity;
            railRoot.setLayoutParams(params);
            host.requestLayout();
        }
    }

    private static boolean isTweetOwner(View view) {
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass()) {
            if ("com.twitter.tweetview.core.TweetView".equals(type.getName())) return true;
        }
        return false;
    }

    private static void pin(View view) {
        if (view == null) return;
        if (view.getVisibility() != View.VISIBLE || view.getAlpha() != 1f || view.getAnimation() != null) {
            view.animate().cancel();
            view.clearAnimation();
            view.setVisibility(View.VISIBLE);
            view.setAlpha(1f);
        }
    }

    @Override public boolean onPreDraw() {
        apply();
        // Never veto the entire frame while native binders are updating controls.
        return true;
    }
    @Override public void onViewAttachedToWindow(View view) {
        ViewTreeObserver tree = view.getViewTreeObserver();
        tree.addOnPreDrawListener(this);
        observer = new WeakReference<>(tree);
    }
    @Override public void onViewDetachedFromWindow(View view) {
        ViewTreeObserver tree = observer.get();
        if (tree != null && tree.isAlive()) tree.removeOnPreDrawListener(this);
        observer.clear();
    }
}
