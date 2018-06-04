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
    private Image image;
    private Mat mat = null;
    private Mat eigenMat = null;
    private ArrayList<File> imgList = null;
    private ArrayList<String> list = null;
    private Mat trainFaceMat = null;//样本训练矩阵
    private Mat meanFaceMat = null;//平均值矩阵
    private int MFMRow;
    private int MFMCol;
    private String MFMFileName = "meanFaceMat";
    private Mat normTrainFaceMat = null;//规格化训练样本矩阵
    private int matRows;//矩阵行 height y
    private int matCols = 48 * 48;//矩阵列 width x
    private Mat eigenvalues = null;//特征值
    private Mat eigenvectors = null;//特征向量
    private Mat eigenVectors = null;//降维后特征向量
    private int eigenRow;//特征向量行数
    private int eigenCol;//特征向量列数
    private String vectorsFile = "eigenVectors";//特征向量保存文件地址
    private String eigenFile = "eigenFace";
    private Mat eigenFace = null;//样本的特征脸空间
    private int eigenFaceRow;//行数
    private int eigenFaceCol;//列数
    private Mat eigenTrainSample = null;//投影样本矩阵
    private String eigenTrainSampleFile = "eigenTrainSample";


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
//            Imgproc.resize(src, dst, new Size(writableImage.getWidth() * zoom_multiples, writableImage.getWidth() * zoom_multiples), 0, 0, INTER_LINEAR);
            Imgproc.resize(src, dst, new Size(48, 48), 0, 0, INTER_LINEAR);
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
     * 图像灰度归一化
     * @param image 源图像
     * @param begin 起点坐标
     * @param end 终点坐标
     * @param zoom_multiples 缩放率
     * @return 目标图像
     */
    public Image normalize(Image image, Point begin, Point end, double zoom_multiples) {
        Image resizeImg = getResizeImg(image, begin, end, zoom_multiples);
        Mat mat = getGrayMatFromImg(resizeImg);
//        Image imgFromMat = getImgFromMat(mat);
//        Mat cMat = getGrayMatFromImg(imgFromMat);
        Mat eMat = equalization(mat);
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
//            System.out.println(fileArrayList.toString());
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
        arrayList.sort(File::compareTo);
        imgList = arrayList;
//        System.out.println(imgList.toString());
        saveImgListToFile();
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
                        float gray = (float) ((color.getBlue() + color.getGreen() + color.getRed()) / 3.0);
                        pixelList[z++] = gray;
                    }
                }
                trainFaceMat.put(countMat++, 0, pixelList);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    //计算平均矩阵
    public void calMeanFaceMat() {
//        outputMat(trainFaceMat);
        meanFaceMat = new Mat(1, matCols, CV_32FC1);
        double sum = 0;
        for (int c = 0; c < matCols; c++) {
            sum = 0;
            for (int r = 0; r < matRows; r++) {
                double tf = trainFaceMat.get(r, c)[0];
                sum += tf;
            }
            double avg = sum / matRows;
//            System.out.println("sum:" + sum + " avg:" + avg);
            meanFaceMat.put(0, c, avg);
        }
//        outputMat(meanFaceMat);
        saveMatToFile(meanFaceMat, MFMFileName);
    }

    //计算规格化样本矩阵
    public void calNormTrainFaceMat() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        normTrainFaceMat = new Mat(matRows, matCols, CV_32FC1);//规格化样本矩阵
//        outputMat(meanFaceMat);
        for (int x = 0; x < matRows; x++) {
            for (int y = 0; y < matCols; y++) {
                normTrainFaceMat.put(x, y, trainFaceMat.get(x, y)[0] - meanFaceMat.get(0, y)[0]);
            }
        }
    }

    //识别比对
    public void calTestFaceMat(Image normalizeImg) {
        Mat mat = getGrayMatFromImg(normalizeImg);
        System.out.println("mat:" + mat.height() + "*" + mat.width());
        Mat testFaceMat = new Mat(1, mat.height() * mat.width(), CV_32FC1);
        for (int i = 0; i < mat.height(); i++) {
            for (int j = 0; j < mat.width(); j++) {
                testFaceMat.put(0, i * mat.width() + j, mat.get(i, j));
            }
        }
        outputMat(testFaceMat);
        System.out.println("testFaceMat:" + testFaceMat.height() + "*" + testFaceMat.width());
        readMeanFaceMat();
//        outputMat(meanFaceMat);
        System.out.println("meanFaceMat:" + meanFaceMat.height() + "*" + meanFaceMat.width());
        Mat normTestFaceMat = new Mat();
        subtract(testFaceMat, meanFaceMat, normTestFaceMat);
//        System.out.println("normTestFaceMat:");
////        outputMat(normTestFaceMat);
        Mat eigenTestSample = new Mat();
        readEigenFace();
        gemm(normTestFaceMat, eigenFace, 1, new Mat(), 0, eigenTestSample);
        outputMat(eigenTestSample);
        double threshold = 0.7;
        double min = 0;
        int index = 0;
        readEigenTrainSample();
        System.out.println("eigenTrainSample:" + eigenTrainSample.height() + "*" + eigenTrainSample.width());
        outputMat(eigenTrainSample);
        for (int i = 0; i < eigenTrainSample.height(); i++) {
            double distance = Math.abs(eigenTrainSample.get(i, 0)[0] - eigenTestSample.get(0, 0)[0]);
            if (i == 0) {
                min = distance;
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
    }

    //计算投影样本矩阵
    public void calculateEigenTrain() {
        //计算特征值和特征向量
        eigenvalues = new Mat();//特征值
        eigenvectors = new Mat();//特征向量
        Mat dst = new Mat();//转置矩阵
        mulTransposed(normTrainFaceMat, dst, false);//计算矩阵与转置矩阵点乘
        eigen(dst, eigenvalues, eigenvectors);//√
//        outputMat(eigenvectors);
//        outputMat(eigenvalues);
        eigenRow = eigenvectors.height();//400
        eigenCol = eigenvectors.width();//400
        //eigenvalues.height()*eigenvalues.width()  400*1
        //特征值求和
        double ValuesSum = 0;
        for (int i = 0; i < eigenvalues.height(); i++) {
            ValuesSum += eigenvalues.get(i, 0)[0];
        }
        System.out.println("ValuesSum:" + ValuesSum);
        // 前m个特征值大于90%
        double sum = 0;
        int m = 0;
        for (int i = 0; i < eigenvalues.height(); i++) {
            sum += eigenvalues.get(i, 0)[0];
            if (sum > ValuesSum * 0.9) {
                m = i;
                break;
            }
        }
        System.out.println("Sum:" + sum);
        System.out.println("m:" + m);
        //特征向量降维
        eigenVectors = new Mat(eigenRow, m, CV_32FC1);
        for (int i = 0; i < eigenRow; i++) {
            for (int j = 0; j < m; j++) {
                eigenVectors.put(i, j, eigenvectors.get(i, j)[0] / Math.sqrt(eigenvalues.get(j, 0)[0]));
            }
        }
//        outputMat(eigenVectors);
        saveMatToFile(eigenVectors, vectorsFile);
        //获得训练样本的特征脸空间
        Mat TnormTrainFaceMat = new Mat();
        transpose(normTrainFaceMat, TnormTrainFaceMat);
        eigenFace = new Mat();
        gemm(TnormTrainFaceMat, eigenVectors, 1, new Mat(), 0, eigenFace);//乘
        eigenFaceRow = eigenFace.height();//N
        eigenFaceCol = eigenFace.width();//m
        saveMatToFile(eigenFace, eigenFile);
        //训练样本在特征脸空间的投影
        eigenTrainSample = new Mat();
        System.out.println("normTrainFaceMat:" + normTrainFaceMat.height() + "*" + normTrainFaceMat.width() + "\neigenFaceMat:" + eigenFace.height() + "*" + eigenFace.width());
        gemm(normTrainFaceMat, eigenFace, 1, new Mat(), 0, eigenTrainSample);//M*N * N*m -> M*m
        System.out.println("eigenTrainSample:" + eigenTrainSample.height() + "*" + eigenTrainSample.width());
        outputMat(eigenTrainSample);
        saveMatToFile(eigenTrainSample, eigenTrainSampleFile);
    }


    public void saveMatToFile(Mat mat, String filename) {
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

    public void saveImgListToFile() {
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

    public void readImgList() {
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

    public void readEigenTrainSample() {
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

    public void readMeanFaceMat() {
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

    public void readEigenFace() {
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
    public void readEigenVectors() {
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
