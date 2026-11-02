package app.morphe.extension.twitter.patches;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import app.morphe.extension.shared.Logger;

/** X 12.19.1 native gallery bridge. Never substitutes a recommendation feed for a source list. */
public final class NativeMedia {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final List<WeakReference<Object>> SOURCES = new ArrayList<>();
    private static final Map<Object, Session> GALLERIES = new WeakHashMap<>();
    private static WeakReference<Activity> previous = new WeakReference<>(null);
    private static List<WeakReference<Object>> previousSources = Collections.emptyList();
    private static boolean installed;
    private static final String HOME = "com.twitter.app.home.HomeTimelineFragment";
    private static final String PROFILE = "com.twitter.app.profiles.timeline.ProfileMediaTimelineFragment";

    static Object field(Object object, String name) throws ReflectiveOperationException {
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    static void set(Object object, String name, Object value) throws ReflectiveOperationException {
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); f.set(object, value); return; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    static Object call(Object object, String name, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        Method m = object.getClass().getMethod(name, types); m.setAccessible(true); return m.invoke(object, args);
    }
    static Object call(Object object, String name) throws ReflectiveOperationException { return call(object, name, new Class<?>[0]); }
    static Object actor() throws ReflectiveOperationException {
        return Class.forName("com.twitter.util.user.UserIdentifier$Companion").getMethod("c").invoke(null);
    }
    static long id(Object object) throws ReflectiveOperationException { return ((Number) call(object, "getId")).longValue(); }
    private static long tweetId(Object tweet) throws ReflectiveOperationException {
        // Match Gallery's own core.a() identifier, including repost wrappers.
        return ((Number) call(field(tweet, "a"), "a")).longValue();
    }

    public static boolean closeComments(Activity activity) { return NativeComments.dismiss(activity); }

    public static void install() {
        if (installed) return;
        android.content.Context context = app.morphe.extension.shared.Utils.getContext();
        if (context == null || !(context.getApplicationContext() instanceof Application)) return;
        installed = true;
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            public void onActivityPaused(Activity a) {
                if (!a.getClass().getName().contains(".gallery.") && !a.getClass().getName().contains("immersivemediaplayer")) {
                    previous = new WeakReference<>(a);
                    List<WeakReference<Object>> visible = new ArrayList<>();
                    for (WeakReference<Object> ref : SOURCES) {
                        Object source = ref.get();
                        try {
                            if (source == null || field(source, "a") != a) continue;
                            Object fragment = field(source, "b");
                            android.view.View view = (android.view.View) call(fragment, "getView");
                            android.graphics.Rect rect = new android.graphics.Rect();
                            if (Boolean.TRUE.equals(call(fragment, "isVisible")) && view != null &&
                                    view.getGlobalVisibleRect(rect) && rect.width() > view.getWidth() / 2) visible.add(ref);
                        } catch (ReflectiveOperationException | RuntimeException ignored) { }
                    }
                    previousSources = visible;
                }
                NativeComments.dismiss(a);
            }
            public void onActivityDestroyed(Activity a) { NativeComments.dismiss(a); }
            public void onActivityCreated(Activity a, Bundle b) { }
            public void onActivityStarted(Activity a) { }
            public void onActivityResumed(Activity a) { }
            public void onActivityStopped(Activity a) { }
            public void onActivitySaveInstanceState(Activity a, Bundle b) { NativeComments.dismiss(a); }
        });
    }

    /** Called after X's source-list controller constructor, not from view binding history. */
    public static void onTimeline(Object controller) {
        install();
        try {
            String type = field(controller, "b").getClass().getName();
            if (!HOME.equals(type) && !PROFILE.equals(type)) return;
            SOURCES.removeIf(ref -> ref.get() == null);
            for (WeakReference<Object> ref : SOURCES) if (ref.get() == controller) return;
            SOURCES.add(new WeakReference<>(controller));
        } catch (ReflectiveOperationException ignored) { }
    }

    private static List<Object> tweets(Object source) throws ReflectiveOperationException {
        Object viewHost = field(source, "v");
        Object provider = call(viewHost, "X1");
        if (!Boolean.TRUE.equals(call(provider, "isInitialized"))) return Collections.emptyList();
        int count = ((Number) call(provider, "a")).intValue();
        LinkedHashMap<Long, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            Object row = call(provider, "getItem", new Class<?>[]{int.class}, i);
            collect(row, result, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        }
        return new ArrayList<>(result.values());
    }
    private static void collect(Object row, Map<Long, Object> tweets, Set<Object> seen, int depth) throws ReflectiveOperationException {
        if (row == null || depth > 5 || !seen.add(row)) return;
        if (row.getClass().getName().equals("com.twitter.model.core.e")) { tweets.putIfAbsent(tweetId(row), row); return; }
        if (row instanceof Iterable<?>) { for (Object child : (Iterable<?>) row) collect(child, tweets, seen, depth + 1); return; }
        if (!row.getClass().getName().startsWith("com.twitter.model.timeline.")) return;
        for (Class<?> c = row.getClass(); c != null && c.getName().startsWith("com.twitter.model.timeline."); c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                f.setAccessible(true); collect(f.get(row), tweets, seen, depth + 1);
            }
        }
    }
    private static List<Object> items(List<Object> tweets) throws ReflectiveOperationException {
        Class<?> tweetClass = Class.forName("com.twitter.model.core.e");
        Class<?> mediaClass = Class.forName("com.twitter.model.core.entity.c0");
        Class<?> requestClass = Class.forName("com.twitter.media.request.a$a");
        Class<?> utils = Class.forName("com.twitter.media.util.t");
        List<Object> items = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (Object tweet : tweets) {
            Object entities = field(call(tweet, "c"), "a");
            if (!(entities instanceof Iterable<?>)) continue;
            for (Object media : (Iterable<?>) entities) {
                if (!mediaClass.isInstance(media)) continue;
                String key = tweetId(tweet) + ":" + field(media, "j");
                if (!keys.add(key)) continue;
                String kind = ((Enum<?>) field(media, "p")).name();
                boolean video = "VIDEO".equals(kind) || "ANIMATED_GIF".equals(kind);
                if (!video && !"IMAGE".equals(kind)) continue;
                Object request = utils.getMethod(video ? "b" : "e", mediaClass).invoke(null, media);
                Method factory = Class.forName("com.twitter.app.gallery.item.a").getMethod("pikoCreate",
                        tweetClass, mediaClass, requestClass, String.class, boolean.class);
                items.add(factory.invoke(null, tweet, media, request, video ? null : field(media, "z"), video));
            }
        }
        return items;
    }

    /** Intercepts X's native gallery item list; failure leaves the original list intact. */
    public static List<?> galleryItems(Object gallery, List<?> original) {
        if (original == null || original.isEmpty()) return original;
        Session current = GALLERIES.get(gallery);
        if (current != null) {
            try { if (id(actor()) == current.owner) return current.updating ? original : current.items; }
            catch (ReflectiveOperationException ignored) { }
            GALLERIES.remove(gallery);
            return original;
        }
        try {
            long owner = id(actor());
            long selected = ((Number) call(field(gallery, "G"), "p")).longValue();
            Activity sourceActivity = previous.get();
            if (sourceActivity == null) return original;
            Object selectedMedia = field(gallery, "W");
            if (selectedMedia == null) {
                int nativeIndex = ((Number) call(field(gallery, "G"), "i")).intValue();
                nativeIndex = Math.max(0, Math.min(original.size() - 1, nativeIndex));
                selectedMedia = field(original.get(nativeIndex), "b");
            }
            if (selectedMedia == null) return original;
            long mediaId = ((Number) field(selectedMedia, "j")).longValue();
            for (int i = previousSources.size() - 1; i >= 0; i--) {
                Object source = previousSources.get(i).get();
                if (source == null || field(source, "a") != sourceActivity || id(field(source, "c")) != owner) continue;
                Object fragment = field(source, "b");
                if (!Boolean.TRUE.equals(call(fragment, "isVisible"))) continue;
                List<Object> posts = tweets(source);
                long matchedPost = -1;
                for (Object post : posts) {
                    if (selected > 0 && id(post) != selected && tweetId(post) != selected) continue;
                    Object entities = field(call(post, "c"), "a");
                    if (!(entities instanceof Iterable<?>)) continue;
                    for (Object media : (Iterable<?>) entities) {
                        if (((Number) field(media, "j")).longValue() == mediaId) { matchedPost = tweetId(post); break; }
                    }
                    if (matchedPost > 0) break;
                }
                if (matchedPost <= 0) continue;
                List<Object> expanded = items(posts);
                if (expanded.isEmpty() || expanded.size() < original.size()) return original;
                int index = -1;
                for (int j = 0; j < expanded.size(); j++) {
                    Object item = expanded.get(j);
                    if (tweetId(field(item, "a")) == matchedPost && ((Number) field(field(item, "b"), "j")).longValue() == mediaId) { index = j; break; }
                }
                if (index < 0) return original;
                set(gallery, "T", index);
                GALLERIES.put(gallery, new Session(gallery, source, owner, expanded));
                return expanded;
            }
        } catch (ReflectiveOperationException | RuntimeException e) { Logger.printException(() -> "Continuous gallery source unavailable", e); }
        return original;
    }

    /** Native gallery page-selected callback; ask the same source for its next page. */
    public static void onGalleryPage(Object listener, int page) {
        try {
            Object gallery = field(listener, "a");
            Session s = GALLERIES.get(gallery);
            if (s != null && page >= s.items.size() - 3) s.more();
        } catch (ReflectiveOperationException ignored) { }
    }
    private static final class Session {
        final WeakReference<Object> gallery, source;
        final long owner;
        List<Object> items;
        boolean loading, updating;
        long retryAfter;
        Session(Object gallery, Object source, long owner, List<Object> items) {
            this.gallery = new WeakReference<>(gallery); this.source = new WeakReference<>(source); this.owner = owner; this.items = items;
        }
        void more() {
            if (loading || android.os.SystemClock.uptimeMillis() < retryAfter) return;
            try {
                Object src = source.get();
                if (src == null || id(actor()) != owner || Boolean.TRUE.equals(call(src, "c"))) return;
                loading = true;
                call(src, "l0", new Class<?>[]{int.class}, 4);
                poll(0);
            } catch (ReflectiveOperationException | RuntimeException e) { loading = false; retryAfter = android.os.SystemClock.uptimeMillis() + 5000; }
        }
        void poll(int attempt) {
            MAIN.postDelayed(() -> {
                Object g = gallery.get(), src = source.get();
                try {
                    if (g == null || src == null || id(actor()) != owner || !Boolean.TRUE.equals(call(field(g, "Y"), "isAttachedToWindow"))) { loading = false; return; }
                    List<Object> fresh = NativeMedia.items(tweets(src));
                    List<Object> merged = MediaSequence.appendNew(items, fresh, item -> {
                        try { return key(item); }
                        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                    });
                    if (merged.size() > items.size()) {
                        items = merged;
                        set(g, "T", ((Number) call(field(g, "Y"), "getCurrentItem")).intValue());
                        updating = true;
                        try { call(g, "I3", new Class<?>[]{List.class, boolean.class}, items, false); }
                        finally { updating = false; loading = false; }
                    } else if (attempt < 30) poll(attempt + 1);
                    else { loading = false; retryAfter = android.os.SystemClock.uptimeMillis() + 5000; }
                } catch (ReflectiveOperationException | RuntimeException e) { loading = false; retryAfter = android.os.SystemClock.uptimeMillis() + 5000; }
            }, 300);
        }
        String key(Object item) throws ReflectiveOperationException { return tweetId(field(item, "a")) + ":" + field(field(item, "b"), "j"); }
    }
}
