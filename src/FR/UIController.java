package FR;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.MalformedURLException;

public class UIController {
    @FXML
    private Button bt1;
    @FXML
    private Button bt2;
    @FXML
    private ImageView img1;

    File file = null;

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
                Image image = new Image(path);
                img1.setImage(image);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public void normalize() {

    }
}
