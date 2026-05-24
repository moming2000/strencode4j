package com.free.strencode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.io.FileUtils;

public class Crypto {
    //rust 函数映射
    private static native int init(String appname,String appid,String licensekey,String url);
    //rust 函数映射
    public static native String encrypt(String scene, String plaintext);
    //rust 函数映射
    public static native String decrypt(String scene, String ciphertext);

    //初始化
    public static void initLoad(String appName,String licenseKey,String url){
        Crypto.appName=appName;
        Crypto.licenseKey=licenseKey;
        Crypto.url=url;
        //System.load("/Users/fbtb102/working/txencode/target/x86_64-apple-darwin/debug/libtxencode.dylib");
        String so = loadFile();
        System.load(so);
        init(Crypto.appName,AppIdUtil.getAppId(Crypto.class),Crypto.licenseKey, Crypto.url);
    }

    private static String loadFile()  {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                os = "win";
            } else if (os.contains("mac")) {
                os = "mac";
            } else if (os.contains("linux")) {
                os = "linux";
            }else{
                os = "linux";
            }
            // 1. 下载
            URL url = new URL(Crypto.url + "/api/download/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            val request = new DownLoadRequest();request.setAppName(Crypto.appName);request.setOs(os);
            // 写入 body
            try (OutputStream oss = conn.getOutputStream()) {
                byte[] input = JsonUtil.serialize(request).getBytes(StandardCharsets.UTF_8);
                oss.write(input);
            }
            String dir = "rcache/"+ UUID.randomUUID().toString().replace("-","");
            String zip = dir+"/rust.zip";
            //删除缓存目录
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        try {
                            FileUtils.deleteDirectory(new File(dir));
                        }catch (Exception e){}
                    })
            );
            File temp = new File(zip);
            try (InputStream in = conn.getInputStream()) {
                FileUtils.createParentDirectories(temp.getAbsoluteFile());
                // 3. 写入文件
                Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                in.close();
                //return ;
            }
            ZipUtil.unzip(temp.getAbsolutePath(),dir);
            //及时清理文件
            try{temp.delete();}catch (Exception e){}
            return getRustFile(dir);
        }catch (Exception e){
            throw new RuntimeException("加密包加载异常",e);
        }
    }

    private static String getRustFile(String directory){
        String osName = System.getProperty("os.name").toLowerCase();
        String ext=".so";
        if (osName.contains("win")) {
            ext = ".dll";
        } else if (osName.contains("mac")) {
            ext = ".dylib";
        } else if (osName.contains("linux")) {
            ext = ".so";
        }
        val ext2=ext;
        File dir = new File(directory);
        File[] files = dir.listFiles((d, name) -> name.endsWith(ext2));
        if (files != null) {
            for (File file : files) {
                return file.getAbsolutePath();
            }
        }
        throw new RuntimeException("加密包加载异常,未找到加密包");
    }

    private static String url;
    private static String licenseKey;
    private static String appName;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class DownLoadRequest{
        @JsonProperty("app_name")
        private String appName;
        private String os;
    }
}