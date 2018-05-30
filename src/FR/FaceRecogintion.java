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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.equalizeHist;

public class FaceRecogintion {
    private Image image;
    private Mat mat;

    FaceRecogintion() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

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
//            return writableImage;
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
        dst.convertTo(dst, CvType.CV_32FC1, 1.0 / 255, 0);
        return dst;
    }

    public Image normalize(Image image, Point begin, Point end, double zoom_multiples) {
//        FaceRecogintion fr = new FaceRecogintion();
        Image resizeImg = getResizeImg(image, begin, end, zoom_multiples);
        Mat mat = getGrayMatFromImg(resizeImg);
        Image imgFromMat = getImgFromMat(mat);
        Mat cMat = getGrayMatFromImg(imgFromMat);
        Mat eMat = equalization(cMat);
        return getImgFromMat(eMat);
    }

}
