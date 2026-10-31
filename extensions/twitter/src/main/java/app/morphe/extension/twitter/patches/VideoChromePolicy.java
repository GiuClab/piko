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
        boolean changed = false;
        for (String id : new String[]{"tweet_content_text", "username", "text_separator", "promoted_badge", "tweet_edit_badge"}) {
            View view = ImmersiveVideoUi.find(chrome, id);
            if (view != null && view.getVisibility() != View.GONE) {
                view.setVisibility(View.GONE);
                changed = true;
            }
        }
        View avatar = ImmersiveVideoUi.find(chrome, "tweet_profile_image_container");
        if (avatar != null && avatar.getAlpha() != 0) avatar.setAlpha(0);
        View play = ImmersiveVideoUi.find(chrome, "video_play_toggle");
        if (play != null && play.isClickable()) play.setClickable(false);
        VideoActionIcon.install(bar);
        return changed;
    }

    @Override public boolean onPreDraw() { return !apply(); }
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
