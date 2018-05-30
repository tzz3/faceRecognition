package test;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class test {
    public static void main(String[] args) {
        System.out.println("OPEN_CV");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat m = Mat.eye(3, 3, CvType.CV_8UC1);
        System.out.println("m = " + m.dump());
    }
}
