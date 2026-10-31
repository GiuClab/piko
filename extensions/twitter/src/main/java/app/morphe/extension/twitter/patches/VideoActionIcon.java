package app.morphe.extension.twitter.patches;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Filled video icons. Native action views still own click listeners, counts and checked states. */
final class VideoActionIcon extends Drawable {
    private static final int HEART = 0, COMMENT = 1, REPOST = 2, BOOKMARK = 3, SHARE = 4, DOWNLOAD = 5, MORE = 6;
    private static final Map<Class<?>, Method> GETTERS = new ConcurrentHashMap<>();
    private final int kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private int alpha = 255;
    private boolean checked;

    static void install(ViewGroup bar) {
        for (int i = 0; i < bar.getChildCount(); i++) {
            View action = bar.getChildAt(i);
            if (!(action instanceof ViewGroup)) continue;
            int kind = kind(ImmersiveVideoUi.name(action));
            if (kind < 0) continue;
            try {
                Method getter = GETTERS.get(action.getClass());
                if (getter == null) {
                    getter = action.getClass().getMethod("getIconView");
                    GETTERS.put(action.getClass(), getter);
                }
                ImageView icon = (ImageView) getter.invoke(action);
                if (!(icon.getDrawable() instanceof VideoActionIcon)) icon.setImageDrawable(new VideoActionIcon(kind));
            } catch (ReflectiveOperationException ignored) { }
        }
    }

    static Drawable download() { return new VideoActionIcon(DOWNLOAD); }
    static Drawable more() { return new VideoActionIcon(MORE); }

    private static int kind(String name) {
        switch (name) {
            case "inline_like": return HEART;
            case "inline_reply": return COMMENT;
            case "inline_retweet": return REPOST;
            case "inline_bookmark": return BOOKMARK;
            case "inline_twitter_share": case "tweet_share": return SHARE;
            default: return name.contains("share") ? SHARE : -1;
        }
    }

    private VideoActionIcon(int kind) {
        this.kind = kind;
        if (kind == HEART) {
            shape.moveTo(50, 90);
            shape.cubicTo(35, 77, 7, 57, 7, 32);
            shape.cubicTo(7, 8, 36, 3, 50, 24);
            shape.cubicTo(64, 3, 93, 8, 93, 32);
            shape.cubicTo(93, 57, 65, 77, 50, 90);
            shape.close();
        } else if (kind == COMMENT) {
            shape.moveTo(50, 9);
            shape.cubicTo(75, 9, 94, 25, 94, 47);
            shape.cubicTo(94, 70, 76, 84, 53, 85);
            shape.lineTo(27, 98); shape.lineTo(30, 81);
            shape.cubicTo(14, 75, 6, 63, 6, 47);
            shape.cubicTo(6, 25, 25, 9, 50, 9); shape.close();
        } else if (kind == BOOKMARK) {
            shape.moveTo(20, 8); shape.lineTo(80, 8); shape.lineTo(80, 94);
            shape.lineTo(50, 73); shape.lineTo(20, 94); shape.close();
        } else if (kind == SHARE) {
            shape.moveTo(10, 84); shape.cubicTo(12, 48, 29, 33, 59, 31);
            shape.lineTo(59, 9); shape.lineTo(96, 46); shape.lineTo(59, 80);
            shape.lineTo(59, 59); shape.cubicTo(38, 58, 23, 66, 10, 84); shape.close();
        }
    }

    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] state) {
        boolean next = false;
        for (int value : state) if (value == android.R.attr.state_checked) next = true;
        if (next == checked) return false;
        checked = next;
        invalidateSelf();
        return true;
    }
    @Override public int getIntrinsicWidth() { return 100; }
    @Override public int getIntrinsicHeight() { return 100; }
    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        int save = canvas.save();
        canvas.translate(b.left, b.top);
        canvas.scale(b.width() / 100f, b.height() / 100f);
        paint.setColor(checked && kind == HEART ? 0xfffe2c55 : checked && kind == BOOKMARK ? 0xffffd34d : Color.WHITE);
        paint.setAlpha(alpha);
        paint.setStyle(Paint.Style.FILL);
        if (kind <= SHARE && kind != REPOST) {
            canvas.drawPath(shape, paint);
            if (kind == COMMENT) {
                paint.setColor(0xff4a403c); paint.setAlpha(alpha);
                canvas.drawCircle(30, 47, 6, paint);
                canvas.drawCircle(50, 47, 6, paint);
                canvas.drawCircle(70, 47, 6, paint);
            }
        } else if (kind == MORE) {
            canvas.drawCircle(50, 20, 6, paint);
            canvas.drawCircle(50, 50, 6, paint);
            canvas.drawCircle(50, 80, 6, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            if (kind == REPOST) {
                canvas.drawLine(22, 73, 22, 23, paint); canvas.drawLine(22, 23, 66, 23, paint);
                canvas.drawLine(78, 27, 78, 77, paint); canvas.drawLine(78, 77, 34, 77, paint);
                canvas.drawLine(8, 39, 22, 23, paint); canvas.drawLine(22, 23, 36, 39, paint);
                canvas.drawLine(64, 61, 78, 77, paint); canvas.drawLine(78, 77, 92, 61, paint);
            } else {
                canvas.drawLine(50, 10, 50, 64, paint);
                canvas.drawLine(30, 44, 50, 64, paint); canvas.drawLine(50, 64, 70, 44, paint);
                Path tray = new Path(); tray.moveTo(16, 72); tray.lineTo(16, 88); tray.lineTo(84, 88); tray.lineTo(84, 72);
                canvas.drawPath(tray, paint);
            }
        }
        canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha) { this.alpha = alpha; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
