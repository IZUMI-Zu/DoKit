# DoKit Design Check Android 使用文档

## 功能说明

该功能通过，调用`OpenCV 4.6.0`相关库函数，实现了对`Android`测试机当前屏幕内容，与设计团队设计稿之间的还原度的比对，目前可以做到覆盖大部分`Android`原生组件与第三方组件。并根据设计稿与屏幕内容生成相关比对结果的效果不同，该报告包含设计稿与屏幕截图（使用红色方框对不用区域进行标记）与二者通过`OpenCV`函数`Imgproc.compareHist(tar, src, Imgproc.CV_COMP_CORREL)`通过直方图比较二者灰度图得出的相似度。

## 代码原理与接口说明

核心功能涉及文件如下：

### `ScreenCaptureUtils.java` 实现了对测试机屏幕的捕捉与获取

`getScreenCapture`函数负责获取屏幕`bitmap`：
屏幕内容的获取的方式在常规模式与系统模式之间有所不同，处于系统模式时直接通过调用getDecorView即可获取目标屏幕的view，而在普通模式，需通过((ViewGroup) activity.getWindow().getDecorView()).getChildAt(0)与background拼接的方式来过滤掉浮窗从而获得目标屏幕。

```java
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
```

`getBitmapWithoutBar`函数负责去除`bitmap`的状态栏：
由于通过上述方式获取到的，`bitmap`均含有状态栏，而目标设计稿并不包含，通过`UIUtils.getStatusBarHeight()`获得测试机的状态栏高度，进行裁切从而获得需要的屏幕内容部分。

```java
    public static Bitmap getBitmapWithoutBar(Bitmap bitmap, int height) {
        return Bitmap.createBitmap(bitmap, 0, height, bitmap.getWidth(), bitmap.getHeight() - height);
    }
```

### `ImageCompareUtils.java` 实现了进行图片比对所需的主要算法

`compareDraft()`函数是主要比较算法的入口
其核心是调用`OpenCV`的库函数`Imgproc.findContours`与`imageSubtract`来实现对图像不同获取其轮廓并获得轮廓相对坐标的功能。首先将获得的`bitmap`转换成`OpenCV`操作所用的矩阵，通过调用`matSizeCompress`函数对二者的大小进行统一（为了后期进行测试机标注时所需坐标的准确性，该函数只在大小不统一时对设计稿进行缩放）。
接着调用`compareHist`函数获得二者的相似度，`Imgproc.findContours`返回了一个包含所有不同矩形的数组，同过遍历次数组并利用`Imgproc.rectangle`函数对图片对应不同区域进行标注，即可获得不同的比较图。

```java
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
```

`generateReport()`函数用于生成比较报告
该函数通过目标设计稿与屏幕的矩阵来生成`bitmap`格式的报告。

```java
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
```

`alignImages()`函数用于对不同大小的图片进行对齐
由于测试机的尺寸单一，而设计稿的分辨率往往不同于测试机屏幕分辨率，为了实现二者的比较则需要对图片进行配准来使能图片比较功能，该函数实现了图片配准的功能，通过`Feature2D`对图片的特征进行提取，并提取85%相似点进行图片匹配于对齐，来获取配准后的图片。

```java
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
```

### `ViewUtils.java` 实现了在测试机获得绘制不用的功能

`drawView`函数实现了对屏幕不同`view`的获取以及替换
因为`View`是以`Tree`的结构组织的，所以通过遍历当前`Activity`的`ViewTree`并结合相关坐标就可以获取到目标`View`。通过遍历`viewTree`来获取到目标`view`，目标`view`是否取到的判断方式为，通过判断坐标是否落到不同区域的四角之内。而由于`OpenCV`与`Android`坐标系相似但不同，所以通过编写`getRectPoint`来将`OpenCV`的`MatOfPoint`坐标转换为`Android`坐标系来进行`view`的判断。

```java
    public static void drawView(ViewGroup viewGroup) {
        for (MatOfPoint matOfPoint : ImageCompareUtils.getDiffDot()) {
            View view = traverseViews(viewGroup, getRectPoint(matOfPoint));
            if (view != null)
                replaceDrawable(view);
        }
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
```

`replaceDrawable`函数实现了对屏幕不同`view`的替换（参考DoKit布局边界功能）
该函数使用了替换`View`的`Background`的方式来为不用的`view`添加边框。`View`的`Background`是`Drawable`类型的，而`LayerDrawable`这种`Drawable`是可以包含一组`Drawable`的，所以取出`View`的原始`Background`后与绘制边框的`Drawable`放进同一个`LayerDrawable`中，就可以实现带边框的背景。从而实现对不同`View`标注的功能。
`clearChild`函数同样，通过去除添加的`StrokeLineDrawable`边框来实现对，不同标注的去除。

```java
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
                new StrokeLineDrawable(view),
                oldDrawable,
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
```

`StrokeLineDrawable`类是继承自Drawable的边框类
它实现了一个简单的为`view`添加红线边框的功能。

```java
class StrokeLineDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final android.graphics.Rect rect;

    private final Context context;

    public StrokeLineDrawable(View view) {
        rect = new android.graphics.Rect(0, 0, view.getWidth(), view.getHeight());
        context = view.getContext();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8);
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
```
