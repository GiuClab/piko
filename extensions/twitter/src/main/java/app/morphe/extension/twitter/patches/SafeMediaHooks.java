package app.morphe.extension.twitter.patches;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import java.util.List;

/** Small guarded entry points. No shared Piko context or optional module is initialized at startup. */
public final class SafeMediaHooks {
    private static void report(Throwable error) { android.util.Log.w("PikoMedia", "Optional media feature skipped", error); }
    private static boolean enabled(Context context, String key) {
        if (context == null) return false;
        return context.getSharedPreferences("piko_settings", Context.MODE_PRIVATE).getBoolean(key, false);
    }
    private static Object field(Object object, String name) throws ReflectiveOperationException {
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static Context contextOf(Object object) {
        if (object instanceof Context) return (Context) object;
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            for (String name : new String[]{"a", "b"}) {
                try {
                    Field f = c.getDeclaredField(name); f.setAccessible(true);
                    Object value = f.get(object);
                    if (value instanceof Context) return (Context) value;
                } catch (ReflectiveOperationException ignored) { }
            }
        }
        return null;
    }
    public static void sourceReady(Object source) {
        OptionalMediaGuard.run(() -> {
            if (enabled(contextOf(source), "media_continuous_gallery")) NativeMedia.onTimeline(source);
        }, SafeMediaHooks::report);
    }
    public static List<?> galleryItems(Object gallery, List<?> original) {
        return OptionalMediaGuard.value(() -> enabled(contextOf(gallery), "media_continuous_gallery")
                ? NativeMedia.galleryItems(gallery, original) : original, original, SafeMediaHooks::report);
    }
    public static void galleryPage(Object listener, int page) {
        OptionalMediaGuard.run(() -> {
            try {
                if (enabled(contextOf(field(listener, "a")), "media_continuous_gallery")) NativeMedia.onGalleryPage(listener, page);
            } catch (ReflectiveOperationException e) { report(e); }
        }, SafeMediaHooks::report);
    }
    static void installComments(ViewGroup bar) {
        OptionalMediaGuard.run(() -> {
            if (!enabled(bar.getContext(), "video_inline_comments")) return;
            NativeMedia.install(bar.getContext());
            NativeComments.install(bar);
        }, SafeMediaHooks::report);
    }
    static boolean closeComments(Activity activity) {
        return OptionalMediaGuard.value(() -> NativeComments.dismiss(activity), false, SafeMediaHooks::report);
    }
}
