package com.didichuxing.doraemonkit.kit.designcheck;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.didichuxing.doraemonkit.DoKit;
import com.didichuxing.doraemonkit.R;
import com.didichuxing.doraemonkit.config.DesignCheckConfig;
import com.didichuxing.doraemonkit.kit.core.AbsDoKitFragment;
import com.didichuxing.doraemonkit.util.ThreadUtils;
import com.didichuxing.doraemonkit.util.ToastUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class UICheckView extends AbsDoKitFragment {
    private TextView mText;
    private TextView mOptions;
    private ImageView mImageView;
    private Button mInternet;
    private Button mLocal;

    private String FilePath;
    private Uri mUri;
    private boolean isChoose = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mText = findViewById(R.id.textView);
        mOptions = findViewById(R.id.options);
        mImageView = findViewById(R.id.imageView);
        mInternet = findViewById(R.id.internet);
        mLocal = findViewById(R.id.local);
        FilePath = this.getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath();
        initView();
    }

    @Override
    public int layoutId() {
        return R.layout.dk_fragment_desgin_check;
    }

    @Override
    public String initTitle() {
        return "图像选择与处理";
    }

    private void initView() {
        mText.setText("截取设备屏幕");
        mOptions.setText("设计稿获取方式");
        mImageView.setImageBitmap(ImageCompareUtils.tarScreen);
        mLocal.setOnClickListener(v -> {
            if (!isChoose) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(intent, 1);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    ToastUtils.showShort("请安装文件管理器");
                }
            } else {
                // TODO
                ImageCompareUtils.isLocal = true;
                ThreadUtils.executeByIo(compareThread);
            }
        });
        // TODO
        mInternet.setOnClickListener(v -> {
            if (!isChoose) {
//                ThreadUtils.executeByIo(new ThreadUtils.SimpleTask<Object>() {
//                    @Override
//                    public Object doInBackground() {
//                        String url = "";
//                        ImageCompareUtils.srcScreen = getPic(url);
//                        return ImageCompareUtils.srcScreen != null;
//                    }
//
//                    @Override
//                    public void onSuccess(Object result) {
//                    }
//                });
                ToastUtils.showShort("目前尚未实现");
                isChoose = true;
            } else {
                ToastUtils.showShort("目前尚未实现");
                ImageCompareUtils.isLocal = false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            mUri = data.getData();
            if (null != mUri) {
                isChoose = true;
                mImageView.setImageURI(mUri);
                mText.setText("设计稿");
                mOptions.setText("比对结果存储方式");
                try {
                    ImageCompareUtils.srcScreen = BitmapFactory.decodeStream(
                        this.getActivity().getBaseContext().getContentResolver().openInputStream(mUri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DesignCheckConfig.setDesignCheckOpen(true);
        DoKit.launchFloating(DesignCheckInfoDoKitView.class);
    }

    public Bitmap getPic(String url) {
        try {
            return BitmapFactory.decodeStream(new OkHttpClient().newCall(
                new Request.Builder().url(url).build()).execute().body().byteStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    ThreadUtils.SimpleTask<Object> compareThread = new ThreadUtils.SimpleTask<Object>() {
        @Override
        public Object doInBackground() {
            if (ImageCompareUtils.isLocal) {
                ImageCompareUtils.resFilePath = FilePath;
                ImageCompareUtils.compareDraft();
            } else {
                // TODO
            }
            return null;
        }

        @Override
        public void onSuccess(Object result) {
            finish();
        }
    };
}
