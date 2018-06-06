package FR;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class UIController {
    @FXML
    private Pane AnchorPane;
    @FXML
    private Button bt1;
    @FXML
    private Button bt2;
    @FXML
    private ImageView img1;
    @FXML
    private ImageView img2;
    @FXML
    private Pane pane_img1;
    @FXML
    private Pane pane3_1;
    @FXML
    private ImageView imgView3_1;
    @FXML
    private ImageView imgView3_2;
    @FXML
    private ImageView imgView3_3;
    @FXML
    private Label label3_1;
    @FXML
    private Label label3_2;
    @FXML
    private Label label4_1;
    @FXML
    private ImageView imgView4_1;
    @FXML
    private ProgressBar pb2_1;

    @FXML
    private TextField text2_1_1;
    @FXML
    private Label label2_1_1;

    private Image image = null;
    private Image normalizeImg = null;
    private boolean click = false;
    private double p1_x;
    private double p1_y;
    private int clickCount = 0;
    private ArrayList<Circle> circles = new ArrayList<>();
    private ArrayList<File> imgList = new ArrayList<>();
    private String imgView = "";
    private int picNum = 9;//每组样本数量
    private String imgPath = "";


    public void setImg1() {
        selectPic();
        img1.setImage(image);

    }

    public void setImgView3_1() {
        selectPic();
        imgView3_1.setImage(image);
        String[] path = imgPath.split("/");
        label3_1.setText(path[path.length - 1]);
    }

    //选择图片
    public void selectPic() {
        clickCount = 0;
        final FileChooser filechooser = new FileChooser();
        String picDir = "C:\\Users\\tzz\\Desktop\\图像处理课程设计2018秋\\人脸测试库";
        filechooser.setInitialDirectory(new File(picDir));
        Stage stage = new Stage();
        File file = filechooser.showOpenDialog(stage);
        if (file != null) {
            System.out.println(file);
            try {
                imgPath = file.toURI().toURL().toString();
                System.out.println(imgPath);
                image = new Image(imgPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            imgPath = "";
        }
    }

    /***
     * 鼠标选点
     * @param event ActionEvent
     */
    public void onMouseClick(MouseEvent event) {
        if ((img1 != null || imgView3_1 != null) && click) {
            clickCount++;
            Circle circle = new Circle(event.getX(), event.getY(), 3);
            circles.add(circle);
            circle.setFill(Color.RED);
            if (imgView.equals("img2")) {
                pane_img1.getChildren().add(circle);
            } else if (imgView.equals("imgView3_2")) {
                pane3_1.getChildren().add(circle);
            }
            if (clickCount >= 2) {
                double p2_x = event.getX();
                double p2_y = event.getY();

                double distance = Math.sqrt(Math.pow((p1_x - p2_x), 2) + Math.pow((p1_y - p2_y), 2));
                double x, y;
                x = (p1_x + p2_x) / 2;
                y = (p1_y + p2_y) / 2;

                double imgHeight = image.getHeight();
                double imgWidth = image.getWidth();
                //double pw = pane_img1.getWidth(), ph = pane_img1.getHeight();//pane height width
                double pw = 0, ph = 0;
                if (imgView.equals("img2")) {
                    pw = pane_img1.getWidth();
                    ph = pane_img1.getHeight();//pane height width
                } else if (imgView.equals("imgView3_2")) {
                    pw = pane3_1.getWidth();
                    ph = pane3_1.getHeight();//pane height width
                }
                if (imgHeight > imgWidth) {
                    pw = imgWidth * ph / imgHeight;
                } else {
                    ph = pw * imgHeight / imgWidth;
                }

                double zoom_multiples = 48 / (Math.sqrt(Math.pow((p1_x - p2_x) * imgWidth / pw, 2) + Math.pow((p1_y - p2_y) * imgHeight / ph, 2)) * 2);

                Point begin = new Point();
                Point end = new Point();
                begin.x = (int) (x - distance);
                begin.x = (int) (begin.getX() * imgWidth / pw);
                begin.y = (int) (y - distance * 0.5);
                begin.y = (int) (begin.getY() * imgHeight / ph);
                end.x = (int) (x + distance);
                end.x = (int) (end.getX() * imgWidth / pw);
                end.y = (int) (y + distance * 1.5);
                end.y = (int) (end.getY() * imgHeight / ph);
                if (begin.x < 0) {
                    begin.x = 0;
                }
                if (begin.y < 0) {
                    begin.y = 0;
                }
                if (end.x > imgWidth) {
                    end.x = (int) imgWidth;
                }
                if (end.y > imgHeight) {
                    end.y = (int) imgHeight;
                }
                //从界面上删除选中点
                for (Circle circle1 : circles) {
                    if (imgView.equals("img2")) {
                        pane_img1.getChildren().remove(circle1);
                    } else if (imgView.equals("imgView3_2")) {
                        pane3_1.getChildren().remove(circle1);
                    }
                }
                FaceRecognition fr = new FaceRecognition();
                normalizeImg = fr.normalize(image, begin, end, zoom_multiples);
//                normalizeImg = fr.getImgFromMat(fr.equalization(fr.getGrayMatFromImg(normalizeImg)));
                if (imgView.equals("img2")) {
                    img2.setImage(normalizeImg);
                } else if (imgView.equals("imgView3_2")) {
                    imgView3_2.setImage(normalizeImg);
                }
            } else {
                p1_x = event.getX();
                p1_y = event.getY();
            }
        }
    }

    @FXML
    public void setImg2() {
        click = true;
        imgView = "img2";
    }

    @FXML
    public void setImgView3_2() {
        click = true;
        imgView = "imgView3_2";
    }


    /**
     * 允许点击选点
     *
     * @param event ActionEvent
     */
    public void normalize(ActionEvent event) {
        click = true;
    }

    /***
     * 保存图片
     * @param event ActionEvent
     */
    public void savePic(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        String picDir = "C:\\Users\\tzz\\Desktop\\图像处理课程设计2018秋";
        fileChooser.setInitialDirectory(new File(picDir));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("All Images", "*.*")
        );
        FaceRecognition FR = new FaceRecognition();
        Stage stage = new Stage();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(img2.getImage(),
                        null), "png", file);
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }

    }


    /***
     * 选择训练文件夹
     * @param actionEvent ActionEvent
     */
    public void selectDir(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("C:\\Users\\tzz\\Desktop\\图像处理课程设计2018秋"));
        Stage stage = new Stage();
        File dir = directoryChooser.showDialog(stage);
        System.out.println(picNum);
        if (dir != null) {
            FaceRecognition FR = new FaceRecognition();
            imgList = FR.getImages(dir, picNum);
            pb2_1.setProgress(1);
        }
    }

    public void openSecondStage() {
        try {
            Stage secondStage = new Stage();
            javafx.scene.layout.AnchorPane root1 = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("FR/UI2.fxml")));
            Scene secondScene = new Scene(root1);
            secondStage.setTitle("设置");
            secondStage.setScene(secondScene);
            secondStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPicNum() {
        try {
            picNum = Integer.parseInt(text2_1_1.getText());
            label2_1_1.setText("修改成功");
        } catch (Exception e) {
            label2_1_1.setText("输入错误");
        }
        System.out.println(picNum);
    }

    //样本训练
    public void training() throws InterruptedException {
        pb2_1.setProgress(0);
        TrainThread thread = new TrainThread("training", imgList);
        thread.start();
        pb2_1.setProgress(1);
    }

    //训练未处理图像集
    public void training2() throws InterruptedException {
        pb2_1.setProgress(0);
        TrainThread thread = new TrainThread("training2", imgList);
        thread.start();
        pb2_1.setProgress(1);
    }

    //人脸识别
    public void recognition() {
        //normalizeImg
        if (normalizeImg != null) {
            imgView3_2.setImage(normalizeImg);
//            imgView3_2.setImage(normalizeImg);
            FaceRecognition FR = new FaceRecognition();
//            String result = FR.calTestFaceMat(normalizeImg);
            String result = FR.calLDATestSample(normalizeImg);//normalize
            String[] path = result.split("\\\\");
            label3_2.setText(path[path.length - 1]);
            System.out.println(result);
            imgView3_3.setImage(new Image("file:/" + result));
        }
    }


    public void takePhoto() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        CameraBasic.photo();
        VideoCapture capture = new VideoCapture(0);
        Mat matrix = new Mat();
        capture.read(matrix);
        if (capture.isOpened()) {
            if (capture.read(matrix)) {
                BufferedImage bufferedImage = new BufferedImage(matrix.width(), matrix.height(), BufferedImage.TYPE_3BYTE_BGR);
                WritableRaster raster = bufferedImage.getRaster();
                DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
                byte[] data = dataBuffer.getData();
                matrix.get(0, 0, data);
                Image WritableImage = SwingFXUtils.toFXImage(bufferedImage, null);
                Imgcodecs.imwrite("photo.bmp", matrix);
                //图片处理-》 未处理-》 直方均衡化
                //图片 -》 直方均衡化
                FaceRecognition FR = new FaceRecognition();
                Mat mat = FR.getGrayMatFromImg(WritableImage);
                Mat eMat = FR.equalization(mat);
                Image img = FR.getImgFromMat(eMat);
                imgView3_1.setImage(img);//sanpshot.jpg
                image = img;
            }
        }
        capture.release();
    }

}
