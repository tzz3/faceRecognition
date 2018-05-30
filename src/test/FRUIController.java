package test;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;



public class FRUIController {
    @FXML
    private Button bt1;
    @FXML
    private TextArea ta1;
    @FXML
    private ImageView image1;

    protected void show(ActionEvent event) {
        ta1.setText("Hello World!");
    }


    /***
     * 选择训练文件夹
     * @param actionEvent
     */
    public void selectDir(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("C:\\Users\\tzz\\Desktop"));
        Stage stage = new Stage();
        File dir = directoryChooser.showDialog(stage);
        if (dir != null) {
            ArrayList<File> arrayList = FaceRecognition.getImages(dir);
            FaceRecognition.getTrainFaceMat(arrayList);
        }
    }

    /***
     * 选择图片
     * @param actionEvent
     */
    public void selectFile(ActionEvent actionEvent) {
        final FileChooser filechooser = new FileChooser();
        String picDir = "C:\\Users\\tzz\\Desktop\\图像处理课程设计2018秋\\人脸测试库";
        filechooser.setInitialDirectory(new File(picDir));
        Stage stage = new Stage();
        File file = filechooser.showOpenDialog(stage);
        if (file != null) {
            System.out.println(file);
            try {
                String path = file.toURI().toURL().toString();
                System.out.println(path);
                Image image = new Image(path);
                image1.setImage(image);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
}
