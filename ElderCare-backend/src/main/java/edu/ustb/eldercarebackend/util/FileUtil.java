package edu.ustb.eldercarebackend.util;

import java.util.UUID;

public class FileUtil {
    /**
     * 获得随机不重复的文件名
     * @return 文件名
     */
    public static String getRandomFileName(){
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-","");
    }

    /**
     * 获取上传文件的原后缀名
     * @param fileName 上传文件的真实名称
     * @return 后缀名
     */
    public static String getFileType(String fileName){
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public static void main(String[] args) {
        System.out.print(FileUtil.getRandomFileName());
        System.out.println(FileUtil.getFileType("aaa.png"));
    }
}

