package com.didichuxing.doraemonkit.kit.designcheck;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.didichuxing.doraemonkit.widget.dialog.DialogInfo;

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
    private EditText mEditText;

    private String filePath;
    private boolean isChoose = false;
    private boolean isLocal = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mText = findViewById(R.id.textView);
        mOptions = findViewById(R.id.options);
        mImageView = findViewById(R.id.imageView);
        mInternet = findViewById(R.id.internet);
        mLocal = findViewById(R.id.local);
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
                    ToastUtils.showShort("No File Manager");
                }
            } else {
                // TODO
                isLocal = true;
                filePath = this.getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath();
                ThreadUtils.executeByIo(new SaveBitmapTask());
            }
        });
        // TODO
        mInternet.setOnClickListener(v -> {
            Context context = this.getContext();
            mEditText = new EditText(context);
            if (!isChoose) {
                new AlertDialog.Builder(context)
                    .setTitle("设计稿获取").setMessage("请输入URL").setView(mEditText)
                    .setPositiveButton("确定", (dialogInterface, i) -> ThreadUtils.executeByIo(downloadBitmap))
                    .setNegativeButton("取消", null).create().show();
            } else {
                new AlertDialog.Builder(context)
                    .setTitle("比对结果上传").setMessage("请输入URL").setView(mEditText)
                    .setPositiveButton("确定", (dialogInterface, i) -> {
                        isLocal = false;
                        filePath = mEditText.getText().toString();
                        ThreadUtils.executeByIo(new SaveBitmapTask());
                    }).setNegativeButton("取消", null).create().show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            Uri mUri = data.getData();
            if (mUri != null) {
                isChoose = true;
                mImageView.setImageURI(mUri);
                mText.setText("设计稿");
                mOptions.setText("比对结果存储方式");
                ContentResolver resolver = this.getActivity().getContentResolver();
                ThreadUtils.executeByIo(new ThreadUtils.SimpleTask<Object>() {
                    @Override
                    public Object doInBackground() {
                        try {
                            ImageCompareUtils.srcScreen = BitmapFactory.decodeStream(
                                resolver.openInputStream(mUri));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        return ImageCompareUtils.srcScreen != null;
                    }

                    @Override
                    public void onSuccess(Object result) {
                    }
                });
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

    class SaveBitmapTask extends ThreadUtils.SimpleTask<Object> {
        @Override
        public Boolean doInBackground() {
            Bitmap bitmap = ImageCompareUtils.compareDraft();
            if (bitmap != null) return ImageCompareUtils.saveResult(bitmap, filePath, "res.png", isLocal);
            else return false;
        }

        @Override
        public void onSuccess(Object result) {
            finish();
        }

        @Override
        public void onFail(Throwable t) {
            super.onFail(t);
            ToastUtils.showShort("结果保存失败");
        }
    }

    ThreadUtils.SimpleTask<Object> downloadBitmap = new ThreadUtils.SimpleTask<Object>() {
        @Override
        public Boolean doInBackground() {
            ImageCompareUtils.srcScreen = getPic(mEditText.getText().toString());
            return ImageCompareUtils.srcScreen != null;
        }

        @Override
        public void onSuccess(Object result) {
            if (result != null) {
                isChoose = true;
                mImageView.setImageBitmap(ImageCompareUtils.srcScreen);
                mText.setText("设计稿");
                mOptions.setText("比对结果存储方式");
            }
        }

        @Override
        public void onFail(Throwable t) {
            super.onFail(t);
            ToastUtils.showShort("下载失败请重试");
        }
    };
}
