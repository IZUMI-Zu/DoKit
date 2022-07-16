package com.didichuxing.doraemonkit.kit.designcheck;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import com.didichuxing.doraemonkit.util.ToastUtils;
import com.didichuxing.doraemonkit.util.UIUtils;

public class ScreenCaptureUtils {

    public static Bitmap getScreenCapture(Activity activity, boolean isNormalMode) {
        View screenView;
        if (isNormalMode) {
            Drawable background = activity.getWindow().getDecorView().getBackground();
            screenView = ((ViewGroup) activity.getWindow().getDecorView()).getChildAt(0);
            screenView.setBackground(background);
        } else {
            screenView = activity.getWindow().getDecorView();
        }
        if (screenView != null) {
            getBitmapFromView(screenView, Color.WHITE);
            return getBitmapWithoutBar(getBitmapFromView(screenView, Color.WHITE)
                , UIUtils.getStatusBarHeight());
        } else {
            ToastUtils.showShort("Can't capture screen!");
            return null;
        }
    }

    public static Bitmap getBitmapFromView(View view, int defaultColor) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(defaultColor);
        view.draw(canvas);
        return bitmap;
    }

    public static Bitmap getBitmapWithoutBar(Bitmap bitmap, int height) {
        return Bitmap.createBitmap(bitmap, 0, height, bitmap.getWidth(), bitmap.getHeight() - height);
    }
}
