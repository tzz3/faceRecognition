package FR;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class UIController {
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


    private File file = null;
    private Image image = null;
    private Image normalizeImg = null;
    private boolean click = false;
    private double p1_x;
    private double p1_y;
    private double p2_x;
    private double p2_y;
    private int clickCount = 0;
    private ArrayList<Circle> circles = new ArrayList<>();
    ArrayList<File> imgList = new ArrayList<>();
    private String imgView;


    public void setImg1() {
        selectPic();
        img1.setImage(image);
    }

    public void setImgView3_1() {
        selectPic();
        imgView3_1.setImage(image);
    }

    //选择图片
    public void selectPic() {
        clickCount = 0;
        final FileChooser filechooser = new FileChooser();
        String picDir = "C:\\Users\\tzz\\Desktop\\图像处理课程设计2018秋\\人脸测试库";
        filechooser.setInitialDirectory(new File(picDir));
        Stage stage = new Stage();
        file = filechooser.showOpenDialog(stage);
        if (file != null) {
            System.out.println(file);
            try {
                String path = file.toURI().toURL().toString();
                System.out.println(path);
                image = new Image(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                p2_x = event.getX();
                p2_y = event.getY();

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

                double zoom_multiples = Math.sqrt(Math.pow((p1_x - p2_x) * imgWidth / pw, 2) + Math.pow((p1_y - p2_y) * imgHeight / ph, 2)) * 2 / 128;

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
        if (dir != null) {
            imgList = FaceRecognition.getImages(dir);
        }
    }

    /**
     * 样本训练
     */
    public void training() {
        NewThread thread = new NewThread("training", imgList);
        thread.start();
    }

}
