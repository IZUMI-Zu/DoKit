package com.didichuxing.doraemonkit.kit.designcheck;


import android.graphics.Bitmap;

import com.didichuxing.doraemonkit.util.FileIOUtils;
import com.didichuxing.doraemonkit.util.ImageUtils;
import com.didichuxing.doraemonkit.util.ToastUtils;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public final class ImageCompareUtils {
    private static final ArrayList<MatOfPoint> diffDot = new ArrayList<>();

    public static String resFilePath;
    public static boolean isLocal;
    public static boolean isCompare = false;
    public static double similarity;

    public static Bitmap tarScreen;
    public static Bitmap srcScreen;

    private static Mat tarScreenMat = new Mat();
    private static Mat srcScreenMat = new Mat();
    private static Mat result;
    private static Mat src;

    // TODO 直接截图获取不同，生成报告
    // TODO 不同格式
    public static void compareDraft() {
        Utils.bitmapToMat(tarScreen, tarScreenMat);
        Utils.bitmapToMat(srcScreen, srcScreenMat);
        result = tarScreenMat.clone();
        src = srcScreenMat.clone();
        Imgproc.cvtColor(srcScreenMat.clone(), srcScreenMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(tarScreenMat.clone(), tarScreenMat, Imgproc.COLOR_BGR2GRAY);
        matSizeCompress(srcScreenMat, tarScreenMat);
        compareHist(tarScreenMat, srcScreenMat);
        Mat matDiff = new Mat();
        Core.absdiff(tarScreenMat, srcScreenMat, matDiff);
        matDiff.convertTo(matDiff, CvType.CV_8UC1);
        Imgproc.findContours(matDiff, diffDot, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint matOfPoint : diffDot) {
            Rect rect = Imgproc.boundingRect(matOfPoint);
            Imgproc.rectangle(result, rect.tl(), rect.br(), new Scalar(255, 0, 0, 128), 2);
        }
        // for test
        isCompare = true;
        byte[] screenByte = ImageUtils.bitmap2Bytes(generateReport(src, result), Bitmap.CompressFormat.PNG, 100);
        FileIOUtils.writeFileFromBytesByStream(ImageCompareUtils.resFilePath + "/tar.png", screenByte);
    }

    public static ArrayList<MatOfPoint> getDiffDot() {
        return diffDot;
    }

    private static Bitmap generateReport(Mat srcMat, Mat tarMat) {
        int w1 = srcMat.cols(), h1 = srcMat.rows();
        int w2 = tarMat.cols(), h2 = tarMat.rows();
        int width = w1 + w2, height = h1 + 400;
        Mat des = Mat.zeros(height, width, srcMat.type());
        Mat rectForM1 = des.colRange(new Range(0, srcMat.cols()));
        int rowOffset1 = (int) (rectForM1.size().height - srcMat.rows()) / 2;
        rectForM1 = rectForM1.rowRange(rowOffset1, rowOffset1 + srcMat.rows());
        Mat rectForM2 = des.colRange(new Range(srcMat.cols(), des.cols()));
        int rowOffset2 = (int) (rectForM2.size().height - tarMat.rows()) / 2;
        rectForM2 = rectForM2.rowRange(rowOffset2, rowOffset2 + tarMat.rows());
        srcMat.copyTo(rectForM1);
        tarMat.copyTo(rectForM2);
        Imgproc.putText(des, "UI Draft", new Point(0, h1 + 250),
            Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(0, 255, 255, 128), 5);
        Imgproc.putText(des, "Captured Screen", new Point(w1, h2 + 250),
            Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(0, 255, 255, 128), 5);
        Imgproc.putText(des, "Similarity: " + similarity, new Point(2, h2 + 350),
            Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(0, 255, 255, 128), 5);
        Bitmap bitmap = Bitmap.createBitmap(des.cols(), des.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(des, bitmap);
        return bitmap;
    }

    // todo different size
    private static void matSizeCompress(Mat srcMat, Mat tarMat) {
        if (((double) srcMat.rows() / srcMat.cols()) == ((double) tarMat.rows() / tarMat.cols())) {
            if (srcMat.rows() > tarMat.rows()) {
                Imgproc.resize(tarMat.clone(), tarMat, srcMat.size());
            } else if (srcMat.rows() < tarMat.rows()) {
                Imgproc.resize(srcMat.clone(), srcMat, tarMat.size());
            }
        } else {
            ToastUtils.showShort("图片比例不同");
        }
    }

    private static void compareHist(Mat srcMat, Mat desMat) {
        srcMat.convertTo(srcMat, CvType.CV_32F);
        desMat.convertTo(desMat, CvType.CV_32F);
        similarity = Imgproc.compareHist(srcMat, desMat, Imgproc.CV_COMP_CORREL);
    }

    public static Point getCenterPoint(Rect rect) {
        Point cpt = new Point();
        cpt.x = rect.x + Math.round(rect.width / 2.0);
        cpt.y = rect.y + Math.round(rect.height / 2.0);
        return cpt;
    }

}
