package com.didichuxing.doraemonkit.kit.designcheck;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.didichuxing.doraemonkit.DoKit;
import com.didichuxing.doraemonkit.R;
import com.didichuxing.doraemonkit.config.DesignCheckConfig;
import com.didichuxing.doraemonkit.kit.core.AbsDoKitView;
import com.didichuxing.doraemonkit.kit.core.DoKitViewLayoutParams;
import com.didichuxing.doraemonkit.util.ActivityUtils;
import com.didichuxing.doraemonkit.util.FileIOUtils;
import com.didichuxing.doraemonkit.util.ImageUtils;
import com.didichuxing.doraemonkit.util.LifecycleListenerUtil;
import com.didichuxing.doraemonkit.util.UIUtils;

import java.io.File;

public class DesignCheckInfoDoKitView extends AbsDoKitView {
    private TextView mCheckHex;
    private ImageView mClose;
    private Button mDoDesignCheck;
    private View mFloatView;

    private int mWindowWidth;
    private int mWindowHeight;
    private int left, right, top, bottom;
    private boolean mCapture = false;

    private Runnable mRunnable;

    @Override
    public void onCreate(Context context) {
        mWindowWidth = UIUtils.getWidthPixels();
        mWindowHeight = UIUtils.getHeightPixels();
    }

    @Override
    public View onCreateView(Context context, FrameLayout rootView) {
        return LayoutInflater.from(context).inflate(R.layout.dk_float_desgin_check, null);
    }

    @Override
    public void initDokitViewLayoutParams(DoKitViewLayoutParams params) {
        params.width = DoKitViewLayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.x = 0;
        params.y = UIUtils.getHeightPixels() - UIUtils.dp2px(150);
    }

    @Override
    public void onViewCreated(FrameLayout rootView) {
        initView();
    }


    private void initView() {
        mCheckHex = findViewById(R.id.align_hex);
        mClose = findViewById(R.id.close);
        mDoDesignCheck = findViewById(R.id.btn_bottom);
        mDoDesignCheck.setText(R.string.dk_kit_design_check_compare);
        mFloatView = this.getActivity().getCurrentFocus();

        mClose.setOnClickListener(v -> {
            DesignCheckConfig.setDesignCheckOpen(false);
            DoKit.removeFloating(DesignCheckInfoDoKitView.class);
        });

        mDoDesignCheck.setOnClickListener(v -> {
            // TODO Better Way
            if (!mCapture) {
                Bitmap bitmapColored = ScreenCapture.getBitmapFromView(
                    this.getActivity().getWindow().getDecorView(), Color.WHITE);
                File file = this.getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                mCapture = FileIOUtils.writeFileFromBytesByStream(file.getPath() + "/target.png",
                    ImageUtils.bitmap2Bytes(bitmapColored, Bitmap.CompressFormat.PNG, 100));
                if (mCapture) mDoDesignCheck.setText(R.string.dk_kit_design_check_choose);
            } else {
                // TODO 在本地与网络进行选择
                mDoDesignCheck.setText(R.string.dk_kit_design_check);
                DoKit.launchFullScreen(ChooseFile.class);
                DesignCheckConfig.setDesignCheckOpen(false);
                DoKit.removeFloating(DesignCheckInfoDoKitView.class);
            }
        });
    }
}
