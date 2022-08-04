package com.didichuxing.doraemonkit.kit.designcheck;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.didichuxing.doraemonkit.DoKit;
import com.didichuxing.doraemonkit.R;
import com.didichuxing.doraemonkit.config.DesignCheckConfig;
import com.didichuxing.doraemonkit.kit.core.AbsDoKitView;
import com.didichuxing.doraemonkit.kit.core.DoKitViewLayoutParams;
import com.didichuxing.doraemonkit.util.FileIOUtils;
import com.didichuxing.doraemonkit.util.ImageUtils;
import com.didichuxing.doraemonkit.util.UIUtils;

public class DesignCheckInfoDoKitView extends AbsDoKitView {
    private TextView mStatus;
    private ImageView mClose;
    private Button mDoDesignCheck;
    private boolean toClear = false;

    private int mWindowWidth;
    private int mWindowHeight;

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (toClear) {
            ViewUtils.clearChild(getViewGroup());
            mStatus.clearComposingText();
        }
    }

    private void initView() {
        mStatus = findViewById(R.id.tv_status_bar_switch);
        mClose = findViewById(R.id.close);
        mDoDesignCheck = findViewById(R.id.btn_bottom);
        mDoDesignCheck.setText(R.string.dk_kit_design_check_compare);

        mClose.setOnClickListener(v -> {
            DesignCheckConfig.setDesignCheckOpen(false);
            DoKit.removeFloating(DesignCheckInfoDoKitView.class);
        });

        mDoDesignCheck.setOnClickListener(v -> {
            // TODO Better Way
            if (toClear) {
                ViewUtils.clearChild(getViewGroup());
                mStatus.clearComposingText();
            }

            byte[] screenByte = ImageUtils.bitmap2Bytes(ScreenCaptureUtils.getScreenCapture(this.getActivity(), isNormalMode()), Bitmap.CompressFormat.PNG, 100);
            FileIOUtils.writeFileFromBytesByStream(this.getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath() + "/a.png", screenByte);

            ImageCompareUtils.tarScreen = ScreenCaptureUtils.getScreenCapture(this.getActivity(), isNormalMode());
            DesignCheckConfig.setDesignCheckOpen(false);
            DoKit.removeFloating(DesignCheckInfoDoKitView.class);
            DoKit.launchFullScreen(UICheckView.class);
        });

        // TODO change logic
        if (ImageCompareUtils.isCompare) {
            mStatus.setText("相似度:\n" + ImageCompareUtils.similarity);
            ViewUtils.drawView(getViewGroup());
            ImageCompareUtils.isCompare = false;
            toClear = true;
        }
    }

    private ViewGroup getViewGroup() {
        ViewGroup viewGroup = (ViewGroup) this.getActivity().getWindow().getDecorView();
        if (isNormalMode()) {
            return (ViewGroup) viewGroup.getChildAt(0);
        } else {
            return viewGroup;
        }
    }


}

