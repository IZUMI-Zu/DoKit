package com.didichuxing.doraemonkit.kit.designcheck;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.didichuxing.doraemonkit.util.UIUtils;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ViewUtils {

    // TODO 处理最外层
    public static void drawView(ViewGroup viewGroup) {
        for (MatOfPoint matOfPoint : ImageCompareUtils.getDiffDot()) {
            Point point = ImageCompareUtils.getCenterPoint(Imgproc.boundingRect(matOfPoint));
            View view = traverseViews(viewGroup, (int) point.x, (int) point.y + UIUtils.getStatusBarHeight());
            replaceDrawable(view);
        }
    }

    private static void replaceDrawable(View view) {
        if (view instanceof TextureView) {
            // 过滤TextureView
            return;
        }
        LayerDrawable newDrawable;
        StrokeLineDrawable border = new StrokeLineDrawable();
        border.setStroke(3, Color.rgb(255, 0, 0)); // black border with full opacity
        if (view.getBackground() != null) {
            Drawable oldDrawable = view.getBackground();
            if (oldDrawable instanceof LayerDrawable) {
                for (int i = 0; i < ((LayerDrawable) oldDrawable).getNumberOfLayers(); i++) {
                    if (((LayerDrawable) oldDrawable).getDrawable(i) instanceof StrokeLineDrawable) {
                        // already replace
                        return;
                    }
                }
                newDrawable = new LayerDrawable(new Drawable[]{
                    oldDrawable,
                    border
                });
            } else {
                newDrawable = new LayerDrawable(new Drawable[]{
                    oldDrawable,
                    border
                });
            }
        } else {
            newDrawable = new LayerDrawable(new Drawable[]{
                border
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
        } else {
            clearDrawable(view);
        }
    }

    private static void clearDrawable(View view) {
        if (view.getBackground() == null) {
            return;
        }
        Drawable oldDrawable = view.getBackground();
        if (!(oldDrawable instanceof LayerDrawable)) {
            return;
        }
        LayerDrawable layerDrawable = (LayerDrawable) oldDrawable;
        List<Drawable> drawables = new ArrayList<>();
        for (int i = 0; i < layerDrawable.getNumberOfLayers(); i++) {
            if (layerDrawable.getDrawable(i) instanceof StrokeLineDrawable) {
                continue;
            }
            drawables.add(layerDrawable.getDrawable(i));
        }
        LayerDrawable newDrawable = new LayerDrawable(drawables.toArray(new Drawable[drawables.size()]));
        view.setBackground(newDrawable);
    }

    private static View traverseViews(View view, int x, int y) {
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
                    View v = traverseViews(((ViewGroup) view).getChildAt(index), x, y);
                    if (v != null) {
                        return v;
                    }
                }
            }
            if (left < x && x < right && top < y && y < bottom) {
                return view;
            } else {
                return null;
            }
        } else {
            if (left < x && x < right && top < y && y < bottom) {
                return view;
            } else {
                return null;
            }
        }
    }

    static class StrokeLineDrawable extends GradientDrawable {
    }
}
