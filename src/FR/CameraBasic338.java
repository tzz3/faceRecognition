package FR;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

class CameraBasic338 {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private JFrame frame;
    private static JLabel label;
    private static int flag = 0;//类静态变量，用于控制按下按钮后 停止摄像头的读取

    static void photo() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    CameraBasic338 window = new CameraBasic338();
                    window.frame.setVisible(true);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        VideoCapture camera = new VideoCapture();//创建Opencv中的视频捕捉对象
        camera.open(0);//open函数中的0代表当前计算机中索引为0的摄像头，如果你的计算机有多个摄像头，那么一次1,2,3……
        if (!camera.isOpened()) {//isOpened函数用来判断摄像头调用是否成功
            System.out.println("Camera Error");//如果摄像头调用失败，输出错误信息
        } else {
            Mat frame = new Mat();//创建一个输出帧
            while (flag == 0) {
                camera.read(frame);//read方法读取摄像头的当前帧
                label.setIcon(new ImageIcon(Objects.requireNonNull(mat2BufferedImage.matToBufferedImage(frame))));//转换图像格式并输出
                try {
                    Thread.sleep(100);//线程暂停100ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            camera.release();
        }
    }

    private CameraBasic338() {
        initialize();
    }

    private void initialize() {
        flag = 0;
        frame = new JFrame();
        frame.setBounds(100, 100, 800, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);//WindowConstants.EXIT_ON_CLOSE
        frame.getContentPane().setLayout(null);

        JButton btnNewButton = new JButton("\u62CD\u7167");
        btnNewButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                flag = 1;//静态变量设置为1，从而按下按钮时会停止摄像头的调用
                frame.dispose();
            }
        });
        btnNewButton.setBounds(660, 13, 113, 27);
        frame.getContentPane().add(btnNewButton);

        label = new JLabel("");
        label.setBounds(0, 0, 700, 500);
        frame.getContentPane().add(label);
    }
}
