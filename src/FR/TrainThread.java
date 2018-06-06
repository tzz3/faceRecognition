package FR;

import java.io.File;
import java.util.ArrayList;

public class TrainThread extends Thread {
    private Thread thread;
    private String threadName;
    private ArrayList<File> imgList;

    TrainThread(String threadName, ArrayList<File> imgList) {
        this.threadName = threadName;
        this.imgList = imgList;
    }

    public void run() {
        FaceRecognition FR = new FaceRecognition();
        if (threadName.equals("training")) {
            FR.getTrainFaceMat(imgList);//处理完成图片
        } else {
            FR.getTrainFace(imgList);//未处理图片
        }
        FR.calMeanFaceMat();
        FR.calNormTrainFaceMat();
        FR.calculateEigenTrain();
        FR.LDA();
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.start();
        }
    }
}
