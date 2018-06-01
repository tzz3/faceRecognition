//package test;
//
//import javax.imageio.ImageIO;
//import javax.imageio.stream.ImageInputStream;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//
//public class FaceRecognition {
//
//
//    //图像归一化
//
//    /***
//     * 判断是否为图片
//     * @param file 输入File
//     * @return boolean
//     */
//    private static boolean isImage(File file) {
//        boolean flag = false;
//        try {
//            ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
//            if (imageInputStream == null) {
//                return false;
//            }
//            imageInputStream.close();
//            flag = true;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return flag;
//    }
//
//    public static ArrayList<File> getImages(File dir) {
//        File[] files = dir.listFiles();
//        ArrayList<File> fileArrayList = new ArrayList<>();
//        if (files != null) {
//            for (File file : files) {
//                if (file.isFile() && isImage(file)) {
//                    fileArrayList.add(file);
//                }
//            }
//            System.out.println(fileArrayList.toString());
//        }
//        return fileArrayList;
//    }
//
//    public static void getTrainFaceMat(ArrayList<File> arrayList) {
//        int len = arrayList.size();
//        arrayList.sort(File::compareTo);
//        int[][] trainFaceMat = new int[len][64 * 64];
//        for (File file : arrayList) {
//
//        }
//    }
//}
