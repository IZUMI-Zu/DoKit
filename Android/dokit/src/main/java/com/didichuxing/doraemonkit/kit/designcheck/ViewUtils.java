package com.didichuxing.doraemonkit.kit.designcheck;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.didichuxing.doraemonkit.util.UIUtils;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public final class ViewUtils {

    public static void drawView(ViewGroup viewGroup) {
        for (MatOfPoint matOfPoint : ImageCompareUtils.getDiffDot()) {
            View view = traverseViews(viewGroup, getRectPoint(matOfPoint));
            if (view != null)
                replaceDrawable(view);
        }
    }

    private static void replaceDrawable(View view) {
        if (view instanceof TextureView) return;
        LayerDrawable newDrawable;
        if (view.getBackground() != null) {
            Drawable oldDrawable = view.getBackground();
            if (oldDrawable instanceof LayerDrawable) {
                for (int i = 0; i < ((LayerDrawable) oldDrawable).getNumberOfLayers(); i++) {
                    if (((LayerDrawable) oldDrawable).getDrawable(i) instanceof StrokeLineDrawable) return;
                }
            }
            newDrawable = new LayerDrawable(new Drawable[]{
                oldDrawable,
                new StrokeLineDrawable(view)
            });
        } else {
            newDrawable = new LayerDrawable(new Drawable[]{
                new StrokeLineDrawable(view)
            });
        }
        try {
            view.setBackground(newDrawable);
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    public static void clearChild(View view) {
        if (view instanceof ViewGroup) {
            clearDrawable(view);
            int childCount = ((ViewGroup) view).getChildCount();
            if (childCount != 0) {
                for (int index = 0; index < childCount; index++) {
                    clearChild(((ViewGroup) view).getChildAt(index));
                }
            }
        } else clearDrawable(view);
    }

    private static void clearDrawable(View view) {
        if (view.getBackground() == null)
            return;
        Drawable oldDrawable = view.getBackground();
        if (!(oldDrawable instanceof LayerDrawable))
            return;
        LayerDrawable layerDrawable = (LayerDrawable) oldDrawable;
        List<Drawable> drawables = new ArrayList<>();
        for (int i = 0; i < layerDrawable.getNumberOfLayers(); i++) {
            if (layerDrawable.getDrawable(i) instanceof StrokeLineDrawable) continue;
            drawables.add(layerDrawable.getDrawable(i));
        }
        LayerDrawable newDrawable = new LayerDrawable(drawables.toArray(new Drawable[drawables.size()]));
        view.setBackground(newDrawable);
    }

    private static View traverseViews(View view, int[][] points) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        if (view instanceof ViewGroup) {
            int childCount = ((ViewGroup) view).getChildCount();
            if (childCount != 0) {
                for (int index = childCount - 1; index >= 0; index--) {
                    View v = traverseViews(((ViewGroup) view).getChildAt(index), points);
                    if (v != null) return v;
                }
            }
        }
        if ((left < points[0][0] && points[0][0] < right && top < points[0][1] && points[0][1] < bottom) &&
            (left < points[1][0] && points[1][0] < right && top < points[1][1] && points[1][1] < bottom) &&
            (left < points[2][0] && points[2][0] < right && top < points[2][1] && points[2][1] < bottom) &&
            (left < points[3][0] && points[3][0] < right && top < points[3][1] && points[3][1] < bottom)) {
            return view;
        } else return null;
    }

    private static int[][] getRectPoint(MatOfPoint matOfPoint) {
        Rect rect = Imgproc.boundingRect(matOfPoint);
        int[][] point = new int[4][2];
        point[0][0] = rect.x + 10;
        point[0][1] = rect.y + UIUtils.getStatusBarHeight() + 10;
        point[1][0] = rect.x + rect.width - 10;
        point[1][1] = rect.y + UIUtils.getStatusBarHeight() + 10;
        point[2][0] = rect.x + 10;
        point[2][1] = rect.y + UIUtils.getStatusBarHeight() + rect.height - 10;
        point[3][0] = rect.x + rect.width - 10;
        point[3][1] = rect.y + UIUtils.getStatusBarHeight() + rect.height - 10;
        return point;
    }

}

class StrokeLineDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final android.graphics.Rect rect;

    private final Context context;

    public StrokeLineDrawable(View view) {
        rect = new android.graphics.Rect(0, 0, view.getWidth(), view.getHeight());
        context = view.getContext();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(4);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRect(rect, paint);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
