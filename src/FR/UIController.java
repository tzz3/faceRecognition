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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
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
    @FXML
    private Button saveButton;

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
     * @param actionEvent ActionEvent
     */
    public void selectFile(ActionEvent actionEvent) {
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
                img1.setImage(image);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    /***
     * 鼠标选点
     * @param event ActionEvent
     */
    public void onMouseClick(MouseEvent event) {
        if (img1 != null && click) {
            clickCount++;
            Circle circle = new Circle(event.getX(), event.getY(), 3);
            circles.add(circle);
            circle.setFill(Color.RED);
            pane_img1.getChildren().add(circle);
            if (clickCount >= 2) {
                p2_x = event.getX();
                p2_y = event.getY();

                double distance = Math.sqrt(Math.pow((p1_x - p2_x), 2) + Math.pow((p1_y - p2_y), 2));
                double x, y;
                x = (p1_x + p2_x) / 2;
                y = (p1_y + p2_y) / 2;

                double imgHeight = image.getHeight();
                double imgWidth = image.getWidth();
                double pw = pane_img1.getWidth(), ph = pane_img1.getHeight();//pane height width
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
                    pane_img1.getChildren().remove(circle1);
                }

                FaceRecogintion fr = new FaceRecogintion();
                Image img = fr.normalize(image, begin, end, zoom_multiples);
                img2.setImage(img);
            } else {
                p1_x = event.getX();
                p1_y = event.getY();
            }
        }

    }

    /**
     * 允许点击
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
        FaceRecogintion FR = new FaceRecogintion();
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

}
