package com.didichuxing.doraemonkit.kit.designcheck;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.didichuxing.doraemonkit.util.FileIOUtils;
import com.didichuxing.doraemonkit.util.ImageUtils;
import com.didichuxing.doraemonkit.util.ToastUtils;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class ImageCompareUtils {
    private static final ArrayList<MatOfPoint> diffDot = new ArrayList<>();

    public static boolean isCompare = false;
    public static double similarity;

    public static Bitmap tarScreen; // 截图
    public static Bitmap srcScreen; // 设计稿

    public static Bitmap compareDraft() {
        if (!diffDot.isEmpty()) diffDot.clear();
        Mat tarScreenMat = new Mat(), srcScreenMat = new Mat();
        Utils.bitmapToMat(tarScreen, tarScreenMat);
        Utils.bitmapToMat(srcScreen, srcScreenMat);
        if (matSizeCompress(tarScreenMat, srcScreenMat)) {
            similarity = compareHist(tarScreenMat, srcScreenMat);
            Mat alignImageMat = new Mat();
            alignImages(tarScreenMat, srcScreenMat, alignImageMat);
            Imgproc.findContours(imageSubtract(tarScreenMat, alignImageMat), diffDot,
                new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            for (MatOfPoint matOfPoint : diffDot) {
                Imgproc.approxPolyDP(new MatOfPoint2f(matOfPoint.toArray()),
                    new MatOfPoint2f(matOfPoint.toArray()), 3, true);
                Imgproc.rectangle(tarScreenMat, Imgproc.boundingRect(matOfPoint), new Scalar(255, 0, 0, 128), 4);
            }
            isCompare = true;
            return generateReport(tarScreenMat, srcScreenMat);
        } else {
            isCompare = false;
            return null;
        }
    }

    public static ArrayList<MatOfPoint> getDiffDot() {
        return diffDot;
    }

    // todo
    public static boolean saveReport(Bitmap resReport, String resFilePath, String fileName, boolean isLocal){
        byte[] resByte = ImageUtils.bitmap2Bytes(resReport, Bitmap.CompressFormat.PNG, 100);
        if (isLocal){
            return FileIOUtils.writeFileFromBytesByStream(resFilePath + "/" + fileName, resByte);
        } else {
            // todo net
            final boolean[] status = {false};
            RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), resByte);
            MultipartBody multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", fileName, fileBody).build();
            Request request = new Request.Builder().url(resFilePath).post(multipartBody).build();
            OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    status[0] = false;
                }
                @Override
                public void onResponse(Call call, Response response) {
                    status[0] = true;
                }
            });
            return status[0];
        }
    }

    // 图片须保持同比例
    private static boolean matSizeCompress(Mat tarMat, Mat srcMat) {
        if (Math.abs(((double) tarMat.rows() / tarMat.cols()) - ((double) srcMat.rows() / srcMat.cols())) <= 10e-3) {
            if (tarMat.rows() != srcMat.rows())
                Imgproc.resize(srcMat, srcMat, tarMat.size(), 0, 0, Imgproc.INTER_LANCZOS4);
            return true;
        } else {
            ToastUtils.showShort("图片比例不同");
            return false;
        }
    }

    private static double compareHist(Mat tarMat, Mat srcMat) {
        Mat tar = tarMat.clone(), src = srcMat.clone();
        Imgproc.cvtColor(tar, tar, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        tar.convertTo(tar, CvType.CV_32F);
        src.convertTo(src, CvType.CV_32F);
        return Imgproc.compareHist(tar, src, Imgproc.CV_COMP_CORREL);
    }

    private static Bitmap generateReport(Mat tarMat, Mat srcMat) {
        Bitmap tarBitmap = Bitmap.createBitmap(tarMat.width(), tarMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tarMat, tarBitmap);
        Bitmap srcBitmap = Bitmap.createBitmap(srcMat.width(), srcMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(srcMat, srcBitmap);
        int width = tarBitmap.getWidth() + srcBitmap.getWidth();
        int height = srcBitmap.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height + 150, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(srcBitmap, 0, 0, new Paint());
        canvas.drawBitmap(tarBitmap, srcBitmap.getWidth(), 0, new Paint());
        Paint paint = new Paint();
        paint.setColor(Color.rgb(110, 110, 110));
        paint.setTextSize(60);
        paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY);
        canvas.drawText("设计稿", 0, height + 60, paint);
        canvas.drawText("屏幕截图", srcBitmap.getWidth(), height + 60, paint);
        canvas.drawText("相似度: " + similarity, 0, height + 130, paint);
        return bitmap;
    }


    private static Mat imageSubtract(Mat tarMat, Mat srcMat) {
        Mat image1Gray = new Mat(), image2Gray = new Mat();
        Imgproc.cvtColor(tarMat, image1Gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(srcMat, image2Gray, Imgproc.COLOR_BGR2GRAY);
        Mat absFrameDifference = new Mat(), segmentation = new Mat();
        Core.absdiff(image1Gray, image2Gray, absFrameDifference);
        absFrameDifference.convertTo(absFrameDifference, CvType.CV_8UC1, 1, 0);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7), new Point(-1, -1));
        Imgproc.threshold(absFrameDifference, segmentation, 65, 255, Imgproc.THRESH_BINARY);
        Imgproc.medianBlur(segmentation, segmentation, 3);
        Imgproc.morphologyEx(segmentation, segmentation, Imgproc.MORPH_CLOSE,
            element, new Point(-1, -1), 3, Core.BORDER_REPLICATE);
        return segmentation;
    }

    private static void alignImages(Mat tarMat, Mat srcMat, Mat alignMat) {
        Mat srcGrey = new Mat(), tarGrey = new Mat();
        Imgproc.cvtColor(tarMat, tarGrey, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(srcMat, srcGrey, Imgproc.COLOR_BGR2GRAY);
        MatOfKeyPoint keyPoints1 = new MatOfKeyPoint(), keyPoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat(), descriptors2 = new Mat();
        Feature2D orb = ORB.create(1000);
        orb.detectAndCompute(srcGrey, new Mat(), keyPoints1, descriptors1);
        orb.detectAndCompute(tarGrey, new Mat(), keyPoints2, descriptors2);
        MatOfDMatch matches = new MatOfDMatch();
        DescriptorMatcher matcher = DescriptorMatcher.create("BruteForce-Hamming");
        matcher.match(descriptors1, descriptors2, matches, new Mat());
        ArrayList<DMatch> matchesList = new ArrayList<>(matches.toList());
        Collections.sort(matchesList, (o1, o2) -> Float.compare(o2.distance, o1.distance));
        int numGoodMatches = (int) (matchesList.size() * 0.15);
        if (matchesList.size() - numGoodMatches > 0) matchesList.subList(0, matchesList.size() - numGoodMatches).clear();
        matches.fromList(matchesList);
        Mat imMatches = new Mat();
        Features2d.drawMatches(srcMat, keyPoints1, tarMat, keyPoints2, matches, imMatches);
        Point[] points1 = new Point[matchesList.size()], points2 = new Point[matchesList.size()];
        for (int i = 0; i < matchesList.size(); i++) {
            points1[i] = (keyPoints1.toArray()[matches.toArray()[i].queryIdx].pt);
            points2[i] = (keyPoints2.toArray()[matches.toArray()[i].trainIdx].pt);
        }
        Mat H = Calib3d.findHomography(new MatOfPoint2f(points1), new MatOfPoint2f(points2), Calib3d.RANSAC);
        Imgproc.warpPerspective(srcMat, alignMat, H, tarMat.size());
    }

}
