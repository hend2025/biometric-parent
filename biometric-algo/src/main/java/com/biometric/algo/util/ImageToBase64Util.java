package com.biometric.algo.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class ImageToBase64Util {

    /**
     * 将本地图片文件转换为 Base64 字符串
     *
     * @param filePath 图片的本地绝对路径 (例如: "D:\\images\\test.jpg")
     * @return Base64 编码后的字符串 (不包含 "data:image/png;base64," 前缀)
     * @throws IOException 如果读取文件失败
     */
    public static String convertImageToBase64(String filePath) throws IOException {
        // 1. 获取文件路径
        Path path = Paths.get(filePath);

        // 2. 读取文件所有字节
        byte[] imageBytes = Files.readAllBytes(path);

        // 3. 使用 Java 8 的 Base64 编码器进行编码
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 测试主方法
     */
    public static void main(String[] args) {
        // 请修改为你本地实际存在的图片路径
        String imagePath = "C:\\Users\\Public\\Pictures\\sample.jpg";

        try {
            // 调用转换方法
            String base64String = convertImageToBase64(imagePath);

            System.out.println("转换成功！");
            System.out.println("Base64 字符串长度: " + base64String.length());

            // 如果需要在前端(HTML/Img标签)中使用，通常需要加上前缀
            // 假设图片是 jpg 格式
            String htmlReadyString = "data:image/jpeg;base64," + base64String;
            System.out.println("前端可用格式示例: " + htmlReadyString.substring(0, 50) + "...");

        } catch (IOException e) {
            System.err.println("转换失败，请检查文件路径是否正确。");
            e.printStackTrace();
        }
    }

}