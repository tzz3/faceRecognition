package FR;

public class RecognitionThread extends Thread {
    private String threadName;
    private Thread thread;

    RecognitionThread() {

    }

    public void run() {
        FaceRecognition FR = new FaceRecognition();

    }


    public synchronized void start() {
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.start();
        }
    }
}
