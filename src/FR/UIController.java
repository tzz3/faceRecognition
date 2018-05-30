package FR;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Mat;

import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
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
    private Pane pane1;
    @FXML
    private Pane pane_img1;

    private File file = null;
    private Image image = null;
    private boolean click = false;
    private double p1_x;
    private double p1_y;
    private double p2_x;
    private double p2_y;
    private int clickCount = 0;
    private ArrayList<Circle> circles = new ArrayList<>();


    /***
     * 选择图片
     * @param actionEvent
     */
    public void selectFile(ActionEvent actionEvent) {
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
                img1.setImage(image);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public void displaynormalize(Point begin, Point end, double zoom_multiples) {
        if (image != null) {
            FaceRecogintion fr = new FaceRecogintion();
            Image resizeImg = fr.getResizeImg(image, begin, end, zoom_multiples);
            Mat mat = fr.getGrayMatFromImg(resizeImg);
            Image imgFromMat = fr.getImgFromMat(mat);

            img2.setImage(imgFromMat);
        }
    }

    public void onMouseClick(MouseEvent event) {
        if (img1 != null && click) {
            clickCount++;
            Circle circle = new Circle(event.getX(), event.getY(), 3);
            circles.add(circle);
            circle.setFill(Color.RED);
            pane_img1.getChildren().add(circle);
            if (clickCount == 2) {
                p2_x = event.getX();
                p2_y = event.getY();

                double distance = Math.sqrt(Math.pow((p1_x - p2_x), 2) + Math.pow((p1_y - p2_y), 2));
                double zoom_multiples = distance * 2 / 128;
                double x, y;
                Point begin = new Point();
                Point end = new Point();
                x = (p1_x + p2_x) / 2;
                y = (p1_y + p2_y) / 2;


                begin.x = (int) (x - distance);
                begin.y = (int) (y - distance * 0.5);
                end.x = (int) (x + distance);
                end.y = (int) (y + distance * 1.5);




                displaynormalize(begin, end, zoom_multiples);

                for (Circle circle1 : circles) {
                    pane_img1.getChildren().remove(circle1);
                }
            } else {
                p1_x = event.getX();
                p1_y = event.getY();
            }
        }

    }

    public void normalize(ActionEvent event) {
        click = true;
    }
}
