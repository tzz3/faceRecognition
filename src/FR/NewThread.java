package FR;

import java.io.File;
import java.util.ArrayList;

public class NewThread extends Thread {
    private Thread thread;
    private String threadName;
    private ArrayList<File> imgList;

    NewThread(String threadName, ArrayList<File> imgList) {
        this.threadName = threadName;
        this.imgList = imgList;
    }

    public void run() {
        FaceRecognition FR = new FaceRecognition();
        FR.getTrainFaceMat(imgList);
        FR.calMeanFaceMat();
        FR.calNormTrainFaceMat();
        FR.calculateEigenTrain();
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.start();
        }
    }
}
