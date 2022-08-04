package com.didichuxing.doraemonkit.kit.designcheck;


import android.graphics.Bitmap;

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
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;

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

    // TODO 直接截图获取不同，生成报告
    // TODO 不同格式
    public static void compareDraft() {
        if (!diffDot.isEmpty()) diffDot.clear();
        Utils.bitmapToMat(tarScreen, tarScreenMat);
        Utils.bitmapToMat(srcScreen, srcScreenMat);
        if (matSizeCompress(tarScreenMat, srcScreenMat)) {
            compareHist(tarScreenMat, srcScreenMat);
            Mat alignImageMat = new Mat();
            alignImages(srcScreenMat, tarScreenMat, alignImageMat);
            Imgproc.findContours(imageSubtract(alignImageMat, tarScreenMat), diffDot,
                new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            for (MatOfPoint matOfPoint : diffDot) {
                Imgproc.approxPolyDP(new MatOfPoint2f(matOfPoint.toArray()),
                    new MatOfPoint2f(matOfPoint.toArray()), 3, true);
                Imgproc.rectangle(tarScreenMat,  Imgproc.boundingRect(matOfPoint), new Scalar(255, 0, 0, 128), 2);
            }
            isCompare = true;
        } else
            isCompare = false;
        // for test
        byte[] screenByte = ImageUtils.bitmap2Bytes(
            generateReport(srcScreenMat, tarScreenMat), Bitmap.CompressFormat.PNG, 100);
        FileIOUtils.writeFileFromBytesByStream(ImageCompareUtils.resFilePath + "/tar.png", screenByte);
    }

    public static ArrayList<MatOfPoint> getDiffDot() {
        return diffDot;
    }

    public static int[] getCenterPoint(MatOfPoint matOfPoint) {
        Rect rect = Imgproc.boundingRect(matOfPoint);
        int[] point = new int[2];
        point[0] = (int) (rect.x + Math.round(rect.width / 2.0));
        point[1] = (int) (rect.y + Math.round(rect.height / 2.0));
        return point;
    }

    private static boolean matSizeCompress(Mat srcMat, Mat tarMat) {
        if (((double) srcMat.rows() / srcMat.cols()) == ((double) tarMat.rows() / tarMat.cols())) {
            if (srcMat.rows() != tarMat.rows())
                Imgproc.resize(tarMat, tarMat, srcMat.size(), 0, 0, Imgproc.INTER_LANCZOS4);
            return true;
        } else {
            ToastUtils.showShort("图片比例不同");
            return false;
        }
    }

    private static void compareHist(Mat srcMat, Mat desMat) {
        Mat src = srcMat.clone();
        Mat des = desMat.clone();
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(des, des, Imgproc.COLOR_BGR2GRAY);
        src.convertTo(src, CvType.CV_32F);
        des.convertTo(des, CvType.CV_32F);
        similarity = Imgproc.compareHist(src, des, Imgproc.CV_COMP_CORREL);
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
            Imgproc.FONT_HERSHEY_SIMPLEX, 2, new Scalar(0, 255, 255, 128), 5);
        Imgproc.putText(des, "Captured Screen", new Point(w1, h2 + 250),
            Imgproc.FONT_HERSHEY_SIMPLEX, 2, new Scalar(0, 255, 255, 128), 5);
        Imgproc.putText(des, "Similarity: " + similarity, new Point(2, h2 + 350),
            Imgproc.FONT_HERSHEY_SIMPLEX, 2, new Scalar(0, 255, 255, 128), 5);
        Bitmap bitmap = Bitmap.createBitmap(des.cols(), des.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(des, bitmap);
        return bitmap;
    }


    private static Mat imageSubtract(Mat src, Mat tar) {
        Mat image1Gray = new Mat(), image2Gray = new Mat();
        Imgproc.cvtColor(src, image1Gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(tar, image2Gray, Imgproc.COLOR_BGR2GRAY);
        Mat absFrameDifference = new Mat(), frameDifference = new Mat();
        Core.subtract(image1Gray, image2Gray, frameDifference);
        Core.absdiff(frameDifference, new Scalar(0), absFrameDifference);
        absFrameDifference.convertTo(absFrameDifference, CvType.CV_8UC1, 1, 0);
        Mat segmentation = new Mat();
        Imgproc.threshold(absFrameDifference, segmentation, 100, 255, Imgproc.THRESH_BINARY);
        Imgproc.medianBlur(segmentation, segmentation, 3);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3), new Point(-1, -1));
        Imgproc.morphologyEx(segmentation, segmentation, Imgproc.MORPH_CLOSE,
            element, new Point(-1, -1), 2, Core.BORDER_REPLICATE);
        return segmentation;
    }

    private static void alignImages(Mat img1, Mat img2, Mat img1Reg) {
        Mat img1Grey = new Mat(), img2Grey = new Mat();
        Imgproc.cvtColor(img1, img1Grey, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(img2, img2Grey, Imgproc.COLOR_BGR2GRAY);
        MatOfKeyPoint keyPoints1 = new MatOfKeyPoint(), keyPoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat(), descriptors2 = new Mat();
        Feature2D orb = ORB.create(1000);
        orb.detectAndCompute(img1Grey, new Mat(), keyPoints1, descriptors1);
        orb.detectAndCompute(img2Grey, new Mat(), keyPoints2, descriptors2);
        MatOfDMatch matches = new MatOfDMatch();
        DescriptorMatcher matcher = DescriptorMatcher.create("BruteForce-Hamming");
        matcher.match(descriptors1, descriptors2, matches, new Mat());
        ArrayList<DMatch> matchesList = new ArrayList<>(matches.toList());
        Collections.sort(matchesList, (o1, o2) -> Float.compare(o2.distance, o1.distance));
        int numGoodMatches = (int) (matchesList.size() * 0.15);
        if (matchesList.size() - numGoodMatches > 0) matchesList.subList(0, matchesList.size() - numGoodMatches).clear();
        matches.fromList(matchesList);
        Mat imMatches = new Mat();
        Features2d.drawMatches(img1, keyPoints1, img2, keyPoints2, matches, imMatches);
        Point[] points1 = new Point[matchesList.size()], points2 = new Point[matchesList.size()];
        for (int i = 0; i < matchesList.size(); i++) {
            points1[i] = (keyPoints1.toArray()[matches.toArray()[i].queryIdx].pt);
            points2[i] = (keyPoints2.toArray()[matches.toArray()[i].trainIdx].pt);
        }
        Mat H = Calib3d.findHomography(new MatOfPoint2f(points1), new MatOfPoint2f(points2), Calib3d.RANSAC);
        Imgproc.warpPerspective(img1, img1Reg, H, img2.size());
    }

}
