package app.morphe.extension.twitter.patches;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import app.morphe.extension.shared.Logger;

/** Mount X's own conversation fragment over the player; never launches the post activity. */
final class NativeComments {
    private static final Map<Activity, Panel> PANELS = new WeakHashMap<>();
    private static final Map<View, View.OnClickListener> LISTENERS = new WeakHashMap<>();

    static void install(ViewGroup bar) {
        View reply = ImmersiveVideoUi.find(bar, "inline_reply");
        if (reply == null) return;
        View.OnClickListener listener = LISTENERS.get(reply);
        if (listener == null) {
            WeakReference<ViewGroup> ref = new WeakReference<>(bar);
            listener = v -> {
                ViewGroup current = ref.get();
                if (current != null) open(current);
            };
            LISTENERS.put(reply, listener);
        }
        reply.setOnClickListener(listener);
        try {
            ((View) NativeMedia.call(reply, "getIconView")).setOnClickListener(listener);
            ((View) NativeMedia.call(reply, "getTextView")).setOnClickListener(listener);
        } catch (ReflectiveOperationException ignored) { }
    }
    private static Activity activity(Context context) {
        for (int i = 0; context != null && i < 20; i++) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) break;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
    static boolean dismiss(Activity activity) {
        Panel panel = PANELS.remove(activity);
        if (panel == null) return false;
        panel.close();
        return true;
    }
    private static void open(ViewGroup bar) {
        Activity activity = activity(bar.getContext());
        if (activity == null || activity.isFinishing()) return;
        dismiss(activity);
        Panel panel = null;
        String stage = "resolve-current-post";
        try {
            Object tweet = ImmersiveVideoUi.currentTweet(bar);
            if (!(tweet instanceof Parcelable) || NativeMedia.id(tweet) <= 0) return;
            panel = new Panel(activity);
            PANELS.put(activity, panel);
            panel.show(tweet);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            String failedStage = panel == null ? stage : panel.stage;
            dismiss(activity);
            showFailure(activity, failedStage, e);
        }
    }
    private static void showFailure(Activity activity, String stage, Throwable failure) {
        Throwable root = failure;
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        while (seen.add(root)) {
            Throwable next = root instanceof java.lang.reflect.InvocationTargetException
                    ? ((java.lang.reflect.InvocationTargetException) root).getTargetException() : root.getCause();
            if (next == null || seen.contains(next)) break;
            root = next;
        }
        String version;
        try { version = String.valueOf(app.morphe.extension.shared.Utils.getPatchesReleaseVersion()); }
        catch (RuntimeException | LinkageError ignored) { version = "unknown"; }
        final String details = "Piko in-player comments diagnostic\n"
                + "Bundle: " + version + "\nStage: " + stage
                + "\nAndroid: " + android.os.Build.VERSION.RELEASE
                + " / API " + android.os.Build.VERSION.SDK_INT
                + "\nCause: " + root.getClass().getName() + ": " + String.valueOf(root.getMessage())
                + "\n\n" + android.util.Log.getStackTraceString(failure);
        android.util.Log.e("PikoMedia", details);
        if (activity.isFinishing() || activity.isDestroyed()) return;
        try {
            new android.app.AlertDialog.Builder(activity)
                    .setTitle(isItalian() ? "Impossibile aprire i commenti" : "Unable to open comments")
                    .setMessage(details)
                    .setPositiveButton(isItalian() ? "Copia dettagli" : "Copy details", (dialog, which) -> {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                                activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard != null) clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Piko comments diagnostic", details));
                    })
                    .setNegativeButton(isItalian() ? "Chiudi" : "Close", null)
                    .show();
        } catch (RuntimeException | LinkageError error) {
            android.util.Log.e("PikoMedia", "Unable to display comments diagnostic", error);
        }
    }

    private static final class Panel {
        final WeakReference<Activity> activity;
        FrameLayout overlay;
        String stage = "create-overlay";
        Object manager, fragment, backDispatcher, backCallback;
        Panel(Activity activity) { this.activity = new WeakReference<>(activity); }
        void show(Object tweet) throws ReflectiveOperationException {
            Activity a = activity.get();
            if (a == null) return;
            ViewGroup content = a.findViewById(android.R.id.content);
            if (content == null) throw new IllegalStateException("No activity content for comments");
            overlay = new FrameLayout(a);
            overlay.setBackgroundColor(0x66000000);
            overlay.setFocusableInTouchMode(true);
            overlay.setOnKeyListener((view, key, event) -> {
                if (key != android.view.KeyEvent.KEYCODE_BACK) return false;
                if (event.getAction() == android.view.KeyEvent.ACTION_UP) dismiss(a);
                return true;
            });
            overlay.setOnClickListener(v -> dismiss(a));
            LinearLayout sheet = new LinearLayout(a);
            sheet.setOrientation(LinearLayout.VERTICAL);
            sheet.setBackgroundColor(Color.BLACK);
            sheet.setClickable(true);
            LinearLayout header = new LinearLayout(a);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(a, 16), 0, 0, 0);
            TextView title = new TextView(a);
            title.setText(isItalian() ? "Commenti" : "Comments");
            title.setTextSize(18); title.setTextColor(Color.WHITE); title.setTypeface(null, Typeface.BOLD);
            header.addView(title, new LinearLayout.LayoutParams(0, dp(a, 52), 1f));
            title.setGravity(Gravity.CENTER_VERTICAL);
            TextView close = new TextView(a);
            close.setText("\u00d7"); close.setTextSize(28); close.setTextColor(Color.WHITE); close.setGravity(Gravity.CENTER);
            close.setContentDescription(isItalian() ? "Chiudi commenti" : "Close comments");
            close.setOnClickListener(v -> dismiss(a));
            header.addView(close, new LinearLayout.LayoutParams(dp(a, 52), dp(a, 52)));
            sheet.addView(header);
            FrameLayout fragmentHost = new FrameLayout(a);
            fragmentHost.setId(View.generateViewId());
            sheet.addView(fragmentHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            int height = Math.round(a.getResources().getDisplayMetrics().heightPixels * 0.66f);
            overlay.addView(sheet, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height, Gravity.BOTTOM));
            content.addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            // Match X's native TweetDetail timeline argument builder, including its account scope.
            stage = "create-native-args-builder";
            Object builder = Class.forName("com.twitter.tweetdetail.g0$a").getConstructor(Bundle.class).newInstance((Object) null);
            stage = "set-native-account";
            NativeMedia.call(builder, "s", new Class<?>[]{Class.forName("com.twitter.util.user.UserIdentifier")}, NativeMedia.actor());
            stage = "set-native-post-id";
            NativeMedia.call(builder, "t", new Class<?>[]{String.class}, String.valueOf(NativeMedia.id(tweet)));
            stage = "build-native-args";
            Bundle args = new Bundle((Bundle) NativeMedia.field(NativeMedia.call(builder, "j"), "a"));
            args.putParcelable("timeline_arg_contextual_tweet", (Parcelable) tweet);
            args.putLong("timeline_arg_tweet_id", NativeMedia.id(tweet));
            args.putInt("arg_timeline_type", 14);
            stage = "create-conversation-fragment";
            fragment = Class.forName("com.twitter.tweetdetail.TweetDetailTimelineFragment").getConstructor().newInstance();
            stage = "set-fragment-arguments";
            NativeMedia.call(fragment, "setArguments", new Class<?>[]{Bundle.class}, args);
            stage = "get-fragment-manager";
            manager = NativeMedia.call(a, "getSupportFragmentManager");
            stage = "create-fragment-transaction";
            Object transaction = transaction();
            stage = "attach-conversation-fragment";
            NativeMedia.call(transaction, "e", new Class<?>[]{int.class, Class.forName("androidx.fragment.app.Fragment"), String.class, int.class},
                    fragmentHost.getId(), fragment, "piko_video_comments", 1);
            // Synchronous commit lets an incompatible native fragment fail without an uncaught async crash.
            stage = "commit-conversation-fragment";
            NativeMedia.call(transaction, "l");
            stage = "conversation-attached";
            overlay.requestFocus();
            registerBack(a);
        }
        Object transaction() throws ReflectiveOperationException {
            return Class.forName("androidx.fragment.app.b").getConstructor(Class.forName("androidx.fragment.app.FragmentManager")).newInstance(manager);
        }
        void registerBack(Activity a) {
            if (android.os.Build.VERSION.SDK_INT < 33) return;
            try {
                Class<?> callbackType = Class.forName("android.window.OnBackInvokedCallback");
                Class<?> dispatcherType = Class.forName("android.window.OnBackInvokedDispatcher");
                backDispatcher = Activity.class.getMethod("getOnBackInvokedDispatcher").invoke(a);
                backCallback = java.lang.reflect.Proxy.newProxyInstance(callbackType.getClassLoader(),
                        new Class<?>[]{callbackType}, (proxy, method, args) -> {
                            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                            if (method.getName().equals("equals")) return proxy == args[0];
                            if (method.getName().equals("toString")) return "PikoCommentsBack";
                            if (method.getName().equals("onBackInvoked")) dismiss(a);
                            return null;
                        });
                dispatcherType.getMethod("registerOnBackInvokedCallback", int.class, callbackType)
                        .invoke(backDispatcher, 0, backCallback);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { }
        }
        void close() {
            try {
                if (backDispatcher != null && backCallback != null) {
                    Class.forName("android.window.OnBackInvokedDispatcher")
                            .getMethod("unregisterOnBackInvokedCallback", Class.forName("android.window.OnBackInvokedCallback"))
                            .invoke(backDispatcher, backCallback);
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { }
            backDispatcher = backCallback = null;
            try {
                if (fragment != null && manager != null) {
                    Object transaction = transaction();
                    NativeMedia.call(transaction, "p", new Class<?>[]{Class.forName("androidx.fragment.app.Fragment")}, fragment);
                    NativeMedia.call(transaction, "m");
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) { Logger.printException(() -> "Comments fragment cleanup failed", e); }
            finally {
                if (overlay != null && overlay.getParent() instanceof ViewGroup) ((ViewGroup) overlay.getParent()).removeView(overlay);
                fragment = manager = null;
            }
        }
    }
    private static boolean isItalian() { return java.util.Locale.getDefault().getLanguage().equals("it"); }
    private static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
}

