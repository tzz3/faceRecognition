package FR;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class FaceRecogintion {

    public Mat getGrayImgFromMat(Image image) {
        if (image != null) {
            PixelReader pixelReader = image.getPixelReader();
            Mat imageMat = new Mat((int) image.getHeight(), (int) image.getWidth(), CvType.CV_32F);
            for (int i = 0; i < image.getHeight(); i++) {
                for (int j = 0; j < image.getWidth(); j++) {
                    Color color = pixelReader.getColor(i, j);
                    float gray = (float) ((color.getBlue() + color.getGreen() + color.getRed()) / 3);
                    imageMat.put(j, i, gray);
                }
            }
            return imageMat;
        }
        return null;
    }

    public Image getImgFromMat(Mat imageMat) {
        if (imageMat != null) {
            WritableImage writableImage = new WritableImage(imageMat.height(), imageMat.width());
            PixelWriter pixelWriter = writableImage.getPixelWriter();
            float[] gray = new float[imageMat.height() * imageMat.width()];
            imageMat.get(0, 0, gray);
            for (int y = 0; y < imageMat.height(); y++) {
                for (int x = 0; x < imageMat.width(); x++) {
                    Color color = Color.gray(gray[y * imageMat.width() + x]);
                    pixelWriter.setColor(x, y, color);
                }
            }
            return writableImage;
        }
        return null;
    }
}
