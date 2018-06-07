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
import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.equalizeHist;

public class FaceRecognition {
    private ArrayList<File> imgList = null;
    private ArrayList<String> list = null;
    private Mat trainFaceMat = null;//样本训练矩阵
    private Mat meanFaceMat = null;//平均值矩阵
    private String MFMFileName = "meanFaceMat";
    private Mat normTrainFaceMat = null;//规格化训练样本矩阵
    private int matRows;//矩阵行 height y
    private int matCols = 48 * 48;//矩阵列 width x
    private Mat eigenVectors = null;//降维后特征向量
    private String vectorsFile = "eigenVectors";//特征向量保存文件地址
    private String eigenFile = "eigenFace";
    private Mat eigenFace = null;//样本的特征脸空间
    private Mat eigenTrainSample = null;//投影样本矩阵
    private String eigenTrainSampleFile = "eigenTrainSample";
    private Mat classMat = null;
    private Mat ldaTrainSample = null;


    FaceRecognition() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /***
     * 图像转化为灰度Mat矩阵
     * @param image 源图像
     * @return 灰度Mat矩阵
     */
    Mat getGrayMatFromImg(Image image) {
        if (image != null) {
            PixelReader pixelReader = image.getPixelReader();
            Mat matImage = new Mat((int) (image.getHeight()), (int) (image.getWidth()), CV_32F);
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
    Image getImgFromMat(Mat matImg) {
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
    private Image getResizeImg(Image image, Point begin, Point end, double zoom_multiples) {

        if (image != null) {
            PixelReader pixelReader = image.getPixelReader();
            //Mat matImage = new Mat((int) (image.getHeight()), (int) (image.getWidth()), CvType.CV_32F);

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
            //Imgproc.resize(src, dst, new Size(writableImage.getWidth() * zoom_multiples, writableImage.getWidth() * zoom_multiples), 0, 0, INTER_LINEAR);
            Imgproc.resize(src, dst, new Size(48, 48), 0, 0, INTER_LINEAR);
            return getImgFromMat(dst);
        }
        return null;
    }

    //直方图均衡化
    Mat equalization(Mat mat) {
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

    //图像灰度归一化
    Image normalize(Image image, Point begin, Point end, double zoom_multiples) {
        Image resizeImg = getResizeImg(image, begin, end, zoom_multiples);
        Mat mat = getGrayMatFromImg(resizeImg);
        //Mat eMat = equalization(mat);
        //return getImgFromMat(eMat);
        return getImgFromMat(mat);
        // return resizeImg;
    }

    //判断是否为图片
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

    //获取所有图像文件名
    ArrayList<File> getImages(File dir, int picNum) {
        File[] files = dir.listFiles();
        String dirPath = dir.getPath();
        ArrayList<File> fileArrayList = new ArrayList<>();
        //System.out.println(files[0]);
        if (files != null) {
            System.out.println("图片共 " + files.length + " 张!");
            classMat = new Mat(files.length, 1, CV_32FC1);
            for (int i = 1; i <= files.length / picNum; i++) {
                for (int j = 1; j <= picNum; j++) {
                    fileArrayList.add(new File(dirPath + "/s" + i + "_" + j + ".bmp"));
                    classMat.put((i - 1) * picNum + j - 1, 0, i);
                }
            }
            saveMatToFile(classMat, "classMat");
        }
        return fileArrayList;
    }

    void getTrainFace(ArrayList<File> arrayList) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        matRows = arrayList.size();
        imgList = arrayList;
        saveImgListToFile();
        trainFaceMat = new Mat(matRows, matCols, CV_32FC1);
        int countMat = 0;
        for (File file : arrayList) {
            try {
                Image image = new Image(file.toURI().toURL().toString());
                Mat mat = getGrayMatFromImg(image);
                //System.out.println(file);
                Mat eMat = equalization(mat);

                int z = 0;
                double[] gray = new double[(int) (image.getWidth() * image.getWidth())];
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        gray[z++] = eMat.get(x, y)[0];
                    }
                }
                trainFaceMat.put(countMat++, 0, gray);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        //outputMat(trainFaceMat);
    }

    //获取TrainFaceMat
    void getTrainFaceMat(ArrayList<File> arrayList) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        matRows = arrayList.size();
        imgList = arrayList;
        //System.out.println(imgList.toString());
        saveImgListToFile();
        trainFaceMat = new Mat(matRows, matCols, CV_32FC1);
        int countMat = 0;
        for (File file : arrayList) {
            try {
                Image image = new Image(file.toURI().toURL().toString());
                //System.out.println(file);
                PixelReader pixelReader = image.getPixelReader();
                double[] pixelList = new double[(int) (image.getWidth() * image.getHeight())];
                int z = 0;
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        Color color = pixelReader.getColor(x, y);
                        float gray = (float) ((color.getBlue() + color.getGreen() + color.getRed()) / 3.0);
                        pixelList[z++] = gray;
                    }
                }
                trainFaceMat.put(countMat++, 0, pixelList);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        //outputMat(trainFaceMat);
    }

    //计算平均矩阵
    void calMeanFaceMat() {
        //outputMat(trainFaceMat);
        meanFaceMat = new Mat(1, matCols, CV_32FC1);
        double sum = 0;
        for (int c = 0; c < matCols; c++) {
            sum = 0;
            for (int r = 0; r < matRows; r++) {
                double tf = trainFaceMat.get(r, c)[0];
                sum += tf;
            }
            double avg = sum / matRows;
            //System.out.println("sum:" + sum + " avg:" + avg);
            meanFaceMat.put(0, c, avg);
        }
        //outputMat(meanFaceMat);
        saveMatToFile(meanFaceMat, MFMFileName);
    }

    //计算规格化样本矩阵
    void calNormTrainFaceMat() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        normTrainFaceMat = new Mat(matRows, matCols, CV_32FC1);//规格化样本矩阵
        //outputMat(meanFaceMat);
        for (int x = 0; x < matRows; x++) {
            for (int y = 0; y < matCols; y++) {
                normTrainFaceMat.put(x, y, trainFaceMat.get(x, y)[0] - meanFaceMat.get(0, y)[0]);
            }
        }
    }

    //PCA识别比对
    String calTestFaceMat(Image normalizeImg) {
        Mat mat = getGrayMatFromImg(normalizeImg);
        System.out.println("mat:" + mat.height() + "*" + mat.width());
        Mat testFaceMat = new Mat(1, mat.height() * mat.width(), CV_32FC1);
        for (int i = 0; i < mat.height(); i++) {
            for (int j = 0; j < mat.width(); j++) {
                testFaceMat.put(0, i * mat.width() + j, mat.get(i, j));
            }
        }
        System.out.println("testFaceMat:" + testFaceMat.height() + "*" + testFaceMat.width());
        readMeanFaceMat();
        System.out.println("meanFaceMat:" + meanFaceMat.height() + "*" + meanFaceMat.width());
        Mat normTestFaceMat = new Mat();
        subtract(testFaceMat, meanFaceMat, normTestFaceMat);
        System.out.println("normTestFaceMat:" + normTestFaceMat.height() + "*" + normTestFaceMat.width());
        Mat eigenTestSample = new Mat();
        readEigenFace();
        gemm(normTestFaceMat, eigenFace, 1, new Mat(), 0, eigenTestSample);
        System.out.println("eigenTestSample:" + eigenTestSample.height() + "*" + eigenTestSample.width());
        //outputMat(eigenTestSample);
        double threshold = 0.7;
        double min = 0;
        int index = 0;
        readEigenTrainSample();
        System.out.println("eigenTrainSample:" + eigenTrainSample.height() + "*" + eigenTrainSample.width());
        double distance = 0;
        for (int i = 0; i < eigenTrainSample.height(); i++) {
            distance = 0;
            for (int j = 0; j < eigenTrainSample.width(); j++) {
                distance += Math.pow(eigenTrainSample.get(i, j)[0] - eigenTestSample.get(0, j)[0], 2);
            }
            distance = Math.sqrt(distance);
            if (i == 0) {
                min = distance;
                index = 0;
            } else {
                if (min > distance) {
                    min = distance;
                    index = i;
                }
            }
        }
        System.out.println("index:" + index + " min:" + min);
        readImgList();
        System.out.println(list.size());
        System.out.println(list.get(index));
        return list.get(index);
    }

    //PCA训练
    void calculateEigenTrain() {
        //计算特征值和特征向量
        //特征值
        Mat eigenvalues = new Mat();
        //特征向量
        Mat eigenvectors = new Mat();
        Mat dst = new Mat();//转置矩阵
        mulTransposed(normTrainFaceMat, dst, false);//计算矩阵与转置矩阵点乘
        eigen(dst, eigenvalues, eigenvectors);//
        //outputMat(eigenvectors);
        //outputMat(eigenvalues);
        //特征向量行数
        int eigenRow = eigenvectors.height();
        //特征向量列数
        int eigenCol = eigenvectors.width();
        //eigenvalues.height()*eigenvalues.width()  400*1
        //特征值求和
        double ValuesSum = 0;
        for (int i = 0; i < eigenvalues.height(); i++) {
            ValuesSum += eigenvalues.get(i, 0)[0];
        }
        //System.out.println("ValuesSum:" + ValuesSum);
        // 前m个特征值大于90%
        double sum = 0;
        int m = 0;
        for (int i = 0; i < eigenvalues.height(); i++) {
            sum += eigenvalues.get(i, 0)[0];
            if (sum >= ValuesSum * 0.9) {
                m = i;
                break;
            }
        }
        //System.out.println("Sum:" + sum);
        System.out.println("m:" + m);
        //特征向量降维
        eigenVectors = new Mat(eigenRow, m, CV_32FC1);
        for (int i = 0; i < eigenRow; i++) {
            for (int j = 0; j < m; j++) {
                eigenVectors.put(i, j, eigenvectors.get(i, j)[0] / Math.sqrt(eigenvalues.get(j, 0)[0]));
            }
        }
        //outputMat(eigenVectors);
        saveMatToFile(eigenVectors, vectorsFile);
        //获得训练样本的特征脸空间
        Mat TnormTrainFaceMat = new Mat();
        transpose(normTrainFaceMat, TnormTrainFaceMat);
        eigenFace = new Mat();
        gemm(TnormTrainFaceMat, eigenVectors, 1, new Mat(), 0, eigenFace);//乘
        //行数
        int eigenFaceRow = eigenFace.height();
        //列数
        int eigenFaceCol = eigenFace.width();
        saveMatToFile(eigenFace, eigenFile);
        //训练样本在特征脸空间的投影
        eigenTrainSample = new Mat();
        System.out.println("normTrainFaceMat:" + normTrainFaceMat.height() + "*" + normTrainFaceMat.width() + "\neigenFaceMat:" + eigenFace.height() + "*" + eigenFace.width());
        gemm(normTrainFaceMat, eigenFace, 1, new Mat(), 0, eigenTrainSample);//M*N * N*m -> M*m
        System.out.println("eigenTrainSample:" + eigenTrainSample.height() + "*" + eigenTrainSample.width());
        //outputMat(eigenTrainSample);
        saveMatToTXTFile(eigenTrainSample, "eigenTrainSampleTXT.txt");
        saveMatToFile(eigenTrainSample, eigenTrainSampleFile);
    }

    //LDA识别
    String calLDATestSample(Image normalizeImg) {
        Mat mat = getGrayMatFromImg(normalizeImg);
        System.out.println("mat:" + mat.height() + "*" + mat.width());
        Mat testFaceMat = new Mat(1, mat.height() * mat.width(), CV_32FC1);
        for (int i = 0; i < mat.height(); i++) {
            for (int j = 0; j < mat.width(); j++) {
                testFaceMat.put(0, i * mat.width() + j, mat.get(i, j));
            }
        }
        System.out.println("testFaceMat:" + testFaceMat.height() + "*" + testFaceMat.width());
        readMeanFaceMat();
        System.out.println("meanFaceMat:" + meanFaceMat.height() + "*" + meanFaceMat.width());
        Mat normTestFaceMat = new Mat();
        subtract(testFaceMat, meanFaceMat, normTestFaceMat);
        System.out.println("normTestFaceMat:" + normTestFaceMat.height() + "*" + normTestFaceMat.width());
        Mat eigenTestSample = new Mat();
        readEigenFace();
        gemm(normTestFaceMat, eigenFace, 1, new Mat(), 0, eigenTestSample);
        System.out.println("eigenTestSample:" + eigenTestSample.height() + "*" + eigenTestSample.width());
        //outputMat(eigenTestSample);
        //测试人脸在LDA投影空间上进行投影
        Mat ldaTestSample = new Mat();
        Mat ldaeigenVectors = readMatFromFile("ldaeigenVectors");
        System.out.println("ldaeigenVectors:" + ldaeigenVectors.height() + "*" + ldaeigenVectors.width());
        //outputMat(ldaeigenVectors);
        gemm(eigenTestSample, ldaeigenVectors, 1, new Mat(), 0, ldaTestSample);
        System.out.println("ldaTestSample:" + ldaTestSample.height() + "*" + ldaTestSample.width());
        //计算欧式距离
        double threshold = 0.7;
        readLDATrainSample();
        //outputMat(ldaTestSample);
        System.out.println("ldaTrainSample:" + ldaTrainSample.height() + "*" + ldaTrainSample.width());
        double min = 0;
        int index = 0;
        double distance = 0;
        for (int i = 0; i < ldaTrainSample.height(); i++) {
            distance = 0;
            for (int j = 0; j < ldaTrainSample.width(); j++) {
                distance += Math.pow(ldaTrainSample.get(i, j)[0] - ldaTestSample.get(0, j)[0], 2);
            }
            distance = Math.sqrt(distance);
            if (i == 0) {
                min = distance;
                index = 0;
            } else {
                if (min > distance) {
                    min = distance;
                    index = i;
                }
            }
        }
        System.out.println("index:" + index + " min:" + min);
        readImgList();
        System.out.println(list.size());
        System.out.println(list.get(index));
        return list.get(index);
    }

    //LDA训练
    void LDA() {
        //计算eigenTrainSample 均值
        int t = 0;
        do {

            int etsHeight = eigenTrainSample.height();
            int etsWidth = eigenTrainSample.width();
            int m = etsWidth;
            Mat u = new Mat(1, etsWidth, CV_32FC1);//1*m
            double avg = 0;
            for (int j = 0; j < etsWidth; j++) {
                avg = 0;
                for (int i = 0; i < etsHeight; i++) {
                    avg += eigenTrainSample.get(i, j)[0];
                }
                avg = avg / etsHeight;
                u.put(0, j, avg);
            }
            //System.out.println("U:");
            //outputMat(u);
            //计算类均值人脸
            readClassMat();
            //outputMat(classMat);
            int c = (int) classMat.get(classMat.height() - 1, 0)[0];
            System.out.println("c:" + c);
            Mat ui = new Mat(c, m, CV_32FC1);
            int now = (int) classMat.get(0, 0)[0];
            double[] sum = new double[etsWidth];
            int num = 1;
            for (int i = 0; i < etsHeight; i++) {
                int cn = (int) classMat.get(i, 0)[0];
                if (cn == now) {
                    num++;
                    for (int j = 0; j < etsWidth; j++) {
                        sum[j] += eigenTrainSample.get(i, j)[0];
                    }
                } else {
                    for (int j = 0; j < etsWidth; j++) {
                        sum[j] /= num;
                    }
                    num = 1;
                    ui.put(now - 1, 0, sum);
                    for (int j = 0; j < etsWidth; j++) {
                        sum[j] = 0;
                    }
                    now = cn;
                    for (int j = 0; j < etsWidth; j++) {
                        sum[j] += eigenTrainSample.get(i, j)[0];
                    }
                }
                if (i == etsHeight - 1) {
                    for (int j = 0; j < etsWidth; j++) {
                        sum[j] /= num;
                    }
                    ui.put(now - 1, 0, sum);
                }
            }
            System.out.println("num:" + num);
            //System.out.println("ui:");
            //outputMat(ui);
            //计算类间离散度矩阵
            Mat Sb = new Mat(m, m, CV_32FC1);
            for (int i = 0; i < c; i++) {
                Mat dst = new Mat();
                subtract(u, ui.row(i), dst);
                // outputMat(dst);
                Mat m2 = new Mat();
                transpose(dst, dst);
                mulTransposed(dst, m2, false);
                add(Sb, m2, Sb);
            }
            //outputMat(Sb);//√
            //System.out.println("\n\n");
            //计算类内离散度矩阵
            Mat Sw = new Mat(m, m, CV_32FC1);
            for (int i = 0; i < c; i++) {
                Mat w = new Mat(m, m, CV_32FC1);
                for (int j = 0; j < num; j++) {
                    Mat dst = new Mat();
                    subtract(eigenTrainSample.row(i * num + j), ui.row(i), dst);
                    //outputMat(dst);
                    transpose(dst, dst);
                    Mat m2 = new Mat();
                    mulTransposed(dst, m2, false);
                    add(w, m2, w);
                }
                add(Sw, w, Sw);
            }
            //outputMat(Sw);//√
            //计算Sw-1Sb 矩阵的特征值和特征向量
            Mat Sw1 = new Mat(Sw.width(), Sw.height(), Sw.type());//Sw.width(), Sw.height(), Sw.type()
            invert(Sw, Sw1, DECOMP_EIG);
            // FIXME: 2018/6/8 逆矩阵计算有时有问题
            //outputMat(Sw1);//√
            System.out.println("Sw1:" + Sw1.height() + "*" + Sw1.width());
            Mat ldaEigenValues = new Mat();
            Mat ldaEigenVectors = new Mat();
            Mat dst = new Mat();
            gemm(Sw1, Sb, 1, new Mat(), 0, dst);
            //outputMat(dst);//√
            eigen(dst, ldaEigenValues, ldaEigenVectors);
            System.out.println("ldaEigenVectors:" + ldaEigenVectors.height() + "*" + ldaEigenVectors.width());
            System.out.println("ldaEigenValues:" + ldaEigenValues.height() + "*" + ldaEigenValues.width());
            //outputMat(ldaEigenVectors);
            //outputMat(ldaEigenValues);//√
            //对特征空间降维

            for (int i = 0; i < ldaEigenValues.height(); i++) {
                if (ldaEigenValues.get(i, 0)[0] > 0) {
                    t = i;
                } else {
                    break;
                }
            }
            Mat ldaeigenVectors = new Mat(ldaEigenVectors.height(), t, CV_32FC1);
            for (int i = 0; i < ldaEigenVectors.height(); i++) {
                for (int j = 0; j < t; j++) {
                    ldaeigenVectors.put(i, j, ldaEigenVectors.get(i, j)[0] / Math.sqrt(ldaEigenValues.get(j, 0)[0]));
                }
            }
            System.out.println("t:" + t);

            saveMatToFile(ldaeigenVectors, "ldaeigenVectors");
            //outputMat(ldaeigenVectors);
            ldaTrainSample = new Mat();
            //readEigenTrainSample();
            gemm(eigenTrainSample, ldaeigenVectors, 1, new Mat(), 0, ldaTrainSample);
            //outputMat(ldaTrainSample);
            saveMatToFile(ldaTrainSample, "ldaTrainSample");

        } while (t < 20);
    }

    private void saveMatToFile(Mat mat, String filename) {
        try {
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(filename));
            outputStream.writeInt(mat.height());
            outputStream.writeInt(mat.width());
            for (int i = 0; i < mat.height(); i++) {
                for (int j = 0; j < mat.width(); j++) {
                    outputStream.writeDouble(mat.get(i, j)[0]);
                }
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveMatToTXTFile(Mat mat, String fileName) {
        try {
            FileWriter fileWriter = new FileWriter(fileName);
            BufferedWriter bw = new BufferedWriter(fileWriter);
            for (int i = 0; i < mat.height(); i++) {
                for (int j = 0; j < mat.width(); j++) {
                    bw.write(String.valueOf(mat.get(i, j)[0]) + " ");
                }
                bw.write("\n");
            }
            bw.close();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int readPicNumFromFile() {
        try {
            FileReader fileReader = new FileReader("picNum.txt");
            BufferedReader br = new BufferedReader(fileReader);
            int picNum = br.read();
            br.close();
            fileReader.close();
            return picNum;
        } catch (IOException e) {
            return 9;
        }
    }

    static void savePicNumToFile(int picnum) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("picNum.txt");
            fileOutputStream.write(picnum);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveImgListToFile() {
        try {
            FileWriter fileWriter = new FileWriter("imgList");
            BufferedWriter bw = new BufferedWriter(fileWriter);
            for (File file : imgList) {
                bw.write(file.getPath() + "\n");
            }
            bw.close();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Mat readMatFromFile(String fileName) {
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(fileName));
            int row = inputStream.readInt();
            int col = inputStream.readInt();
            Mat mat = new Mat(row, col, CV_32FC1);
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < col; j++) {
                    mat.put(i, j, inputStream.readDouble());
                }
            }
            return mat;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void readImgList() {
        try {
            InputStream is = new FileInputStream(new File("imgList"));
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            line = br.readLine();
            list = new ArrayList<>();
            while (line != null) {
                list.add(line);
                line = br.readLine();
            }
            br.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readClassMat() {
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream("classMat"));
            int row = inputStream.readInt();
            int col = inputStream.readInt();
            classMat = new Mat(row, col, CV_32FC1);
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < col; j++) {
                    classMat.put(i, j, inputStream.readDouble());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readLDATrainSample() {
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream("ldaTrainSample"));
            int row = inputStream.readInt();
            int col = inputStream.readInt();
            ldaTrainSample = new Mat(row, col, CV_32FC1);
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < col; j++) {
                    ldaTrainSample.put(i, j, inputStream.readDouble());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readEigenTrainSample() {
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(eigenTrainSampleFile));
            int row = inputStream.readInt();
            int col = inputStream.readInt();
            eigenTrainSample = new Mat(row, col, CV_32FC1);
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < col; j++) {
                    eigenTrainSample.put(i, j, inputStream.readDouble());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMeanFaceMat() {
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(MFMFileName));
            int row = inputStream.readInt();
            int col = inputStream.readInt();
            meanFaceMat = new Mat(row, col, CV_32FC1);
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < col; j++) {
                    meanFaceMat.put(i, j, inputStream.readDouble());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void readEigenFace() {
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(eigenFile));
            int row = inputStream.readInt();
            int col = inputStream.readInt();
            eigenFace = new Mat(row, col, CV_32FC1);
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < col; j++) {
                    eigenFace.put(i, j, inputStream.readDouble());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //从文件读取特征向量
    void readEigenVectors() {
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(vectorsFile));
            int row = inputStream.readInt();
            int col = inputStream.readInt();
            eigenVectors = new Mat(row, col, CV_32FC1);
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < col; j++) {
                    double d = inputStream.readDouble();
                    eigenVectors.put(i, j, inputStream.readDouble());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //输出Mat矩阵
    public void outputMat(Mat mat) {
        for (int i = 0; i < mat.height(); i++) {
            for (int j = 0; j < mat.width(); j++) {
                System.out.print(Arrays.toString(mat.get(i, j)) + " ");
            }
            System.out.println();
        }
    }
}
