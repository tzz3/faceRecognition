package FR;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.equalizeHist;

public class FaceRecognition {
    private Image image;
    private Mat mat;
    private Mat trainFaceMat = null;//样本训练矩阵
    private Mat meanFaceMat = null;//平均值矩阵
    private Mat normTrainFaceMat = null;//规格化训练样本矩阵
    private int matRows;//矩阵行 height y
    private int matCols = 48 * 48;//矩阵列 width x


    FaceRecognition() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /***
     * 图像转化为灰度Mat矩阵
     * @param image 源图像
     * @return 灰度Mat矩阵
     */
    public Mat getGrayMatFromImg(Image image) {
        if (image != null) {
            PixelReader pixelReader = image.getPixelReader();
            Mat matImage = new Mat((int) (image.getHeight()), (int) (image.getWidth()), CvType.CV_32F);
            //遍历源图像每个像素，将其写入到目标图像
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    Color color = pixelReader.getColor(x, y);
                    float gray = (float) ((color.getBlue() + color.getGreen() + color.getRed()) / 3.0);
                    matImage.put(y, x, gray);
                }
            }
            return matImage;
        }
        return null;
    }

    /***
     * Mat转化为Img
     * @param matImg Mat矩阵
     * @return Img图像
     */
    public Image getImgFromMat(Mat matImg) {
        if (matImg != null) {
            WritableImage wImage = new WritableImage(matImg.width(), matImg.height());
            //得到像素写入器
            PixelWriter pixelWriter = wImage.getPixelWriter();
            if (matImg.channels() == 1) {  //单通道图像
                float[] gray = new float[matImg.height() * matImg.width()];
                matImg.get(0, 0, gray);
                //遍历源图像每个像素，将其写入到目标图像
                for (int y = 0; y < matImg.height(); y++) {
                    for (int x = 0; x < matImg.width(); x++) {
                        int pixelIndex = y * matImg.width() + x;
                        Color color = Color.gray(gray[pixelIndex]);
                        pixelWriter.setColor(x, y, color);
                    }
                }
            } else if (matImg.channels() == 3) {    //3通道图像
                //遍历源图像每个像素，将其写入到目标图像
                for (int y = 0; y < matImg.height(); y++) {
                    for (int x = 0; x < matImg.width(); x++) {
                        int[] gray = new int[3];
                        matImg.get(y, x, gray);
                        Color color = Color.rgb(gray[2], gray[1], gray[0]);
                        pixelWriter.setColor(x, y, color);
                    }
                }
            }
            return wImage;
        }
        return null;
    }


    /***
     * 图像缩放
     * @param image 源图像
     * @param begin 起点坐标
     * @param end 终点坐标
     * @param zoom_multiples 缩放倍数
     * @return 图像Image
     */
    public Image getResizeImg(Image image, Point begin, Point end, double zoom_multiples) {

        if (image != null) {
            PixelReader pixelReader = image.getPixelReader();
//            Mat matImage = new Mat((int) (image.getHeight()), (int) (image.getWidth()), CvType.CV_32F);

            WritableImage writableImage = new WritableImage((int) (end.getX() - begin.getX()), (int) (end.getY() - begin.getY()));
            PixelWriter pixelWriter = writableImage.getPixelWriter();

            for (int y = (int) begin.getY(); y < end.getY(); y++) {
                for (int x = (int) begin.getX(); x < end.getX(); x++) {
                    Color color = pixelReader.getColor(x, y);
                    double x1 = color.getBlue();
                    double x2 = color.getRed();
                    double x3 = color.getGreen();
                    Color color2 = new Color(x2, x3, x1, 1);
                    pixelWriter.setColor((int) (x - begin.getX()), (int) (y - begin.getY()), color2);
                }
            }
            Mat src = getGrayMatFromImg(writableImage);
            Mat dst = new Mat();
            Imgproc.resize(src, dst, new Size(writableImage.getWidth() * zoom_multiples, writableImage.getHeight() * zoom_multiples), 0, 0, INTER_LINEAR);
            return getImgFromMat(dst);
        }
        return null;
    }


    /***
     * 直方图均衡化
     * @param mat 图像矩阵
     * @return 目标矩阵Mat
     */
    public Mat equalization(Mat mat) {
        mat.convertTo(mat, CvType.CV_8UC1, 255, 0);
        Mat dst = new Mat();
        List<Mat> mv = new ArrayList<>();
        Core.split(mat, mv);
        for (int i = 0; i < mat.channels(); i++) {
            equalizeHist(mv.get(i), mv.get(i));
        }
        Core.merge(mv, dst);
        dst.convertTo(dst, CV_32FC1, 1.0 / 255, 0);
        return dst;
    }

    /***
     * 直方图均衡化
     * @param image 源图像
     * @param begin 起点坐标
     * @param end 终点坐标
     * @param zoom_multiples 缩放率
     * @return 目标图像
     */
    public Image normalize(Image image, Point begin, Point end, double zoom_multiples) {
        Image resizeImg = getResizeImg(image, begin, end, zoom_multiples);
        Mat mat = getGrayMatFromImg(resizeImg);
        Image imgFromMat = getImgFromMat(mat);
        Mat cMat = getGrayMatFromImg(imgFromMat);
        Mat eMat = equalization(cMat);
        return getImgFromMat(eMat);
    }

    /***
     * 判断是否为图片
     * @param file 输入File
     * @return boolean
     */
    private static boolean isImage(File file) {
        boolean flag = false;
        try {
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
            if (imageInputStream == null) {
                return false;
            }
            imageInputStream.close();
            flag = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    /***
     * 获取所有图像文件名
     * @param dir 文件夹名
     * @return List
     */
    public static ArrayList<File> getImages(File dir) {
        File[] files = dir.listFiles();
        ArrayList<File> fileArrayList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isImage(file)) {
                    fileArrayList.add(file);
                }
            }
            System.out.println(fileArrayList.toString());
        }
        return fileArrayList;
    }

    /***
     * 获取TrainFaceMat
     * @param arrayList 所有图像文件
     */
    public void getTrainFaceMat(ArrayList<File> arrayList) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        matRows = arrayList.size();
        System.out.println(matRows);
        arrayList.sort(File::compareTo);
        trainFaceMat = new Mat(matRows, matCols, CV_32FC1);
        int countMat = 0;
        for (File file : arrayList) {
            try {
                Image image = new Image(file.toURI().toURL().toString());
                PixelReader pixelReader = image.getPixelReader();
                double[] pixelList = new double[(int) (image.getWidth() * image.getHeight())];
                int z = 0;
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        Color color = pixelReader.getColor(x, y);
                        float gray = (float) ((color.getBlue() + color.getGreen() + color.getRed()) / 3.0) * 255;
                        pixelList[z++] = gray;
                    }
                }
                trainFaceMat.put(countMat++, 0, pixelList);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
//        System.out.println(trainFaceMat.toString());
//        for (int i = 0; i < matRows; i++) {
//            for (int j = 0; j < matCols; j++) {
//                System.out.print(Arrays.toString(trainFaceMat.get(i, j)) + " ");
//            }
//            System.out.println();
//        }
    }

    /**
     * 计算平均矩阵
     */
    public void cirMeanFaceMat() {
        meanFaceMat = new Mat(1, matCols, CV_32FC1);
        int sum = 0, count = 0;
        for (int c = 0; c < matCols; c++) {
            sum = 0;
            count = 0;
            for (int r = 0; r < matRows; r++) {
                int tf = (int) trainFaceMat.get(r, c)[0];
                if (tf > 0) {
                    sum += tf;
                    count++;
                }
            }
            int avg;
            if (count > 0) {
                avg = sum / count;
            } else {
                avg = 0;
            }
//            System.out.println("sum:" + sum + " count:" + count + " avg:" + avg);
            meanFaceMat.put(0, c, avg);
        }
        for (int i = 0; i < matCols; i++) {
            System.out.print(Arrays.toString(meanFaceMat.get(0, i)) + " ");
        }
    }

    public void cirNormTrainFaceMat() {
        // TODO: 2018/6/1 计算规格化训练矩阵
        normTrainFaceMat = new Mat(matRows, matCols, CV_32FC1);
        for (int x = 0; x < matRows; x++) {
            for (int y = 0; y < matCols; y++) {
                normTrainFaceMat.put(x, y, trainFaceMat.get(x, y)[0] - meanFaceMat.get(0, y)[0]);
            }
        }
//        for (int i = 0; i < matRows; i++) {
//            for (int j = 0; j < matCols; j++) {
//                System.out.print(Arrays.toString(normTrainFaceMat.get(i, j)) + " ");
//            }
//            System.out.println();
//        }
    }
}
