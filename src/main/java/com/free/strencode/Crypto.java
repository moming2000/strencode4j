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
        String appId = AppIdUtil.getAppId(Crypto.class);
        String so = loadFile(appId);
        System.load(so);
        init(Crypto.appName,appId,Crypto.licenseKey, Crypto.url);
    }

    private static String loadFile(String appId)  {
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
            // 缓存
            File dir = new File("rcache/"+ appId);
            File zip = new File(dir.getAbsolutePath()+"/"+appId+".zip");
            File version = new File(dir.getAbsolutePath()+"/version.txt");
            String versionIdName="VERSION-ID";
            String versionId="";
            if(version.exists()){
                versionId = new String(Files.readAllBytes(version.toPath()));
            }
            // 1. 下载
            URL url = new URL(Crypto.url + "/api/download/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            if(!versionId.isEmpty()){
                 conn.setRequestProperty(versionIdName,versionId);
            }
            val request = new DownLoadRequest();request.setAppName(Crypto.appName);request.setOs(os);
            // 写入 body
            try (OutputStream oss = conn.getOutputStream()) {
                byte[] input = JsonUtil.serialize(request).getBytes(StandardCharsets.UTF_8);
                oss.write(input);
            }
            val newVersionId=conn.getHeaderField(versionIdName);
            if(!versionId.isEmpty()&&versionId.equals(newVersionId)){
                //
            }else {
                try (InputStream in = conn.getInputStream()) {
                    FileUtils.createParentDirectories(zip.getAbsoluteFile());
                    // 3. 写入文件
                    Files.copy(in, zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    in.close();
                    //return ;
                }
                ZipUtil.unzip(zip.getAbsolutePath(), dir.getAbsolutePath());
                //及时清理文件
                try {
                    zip.delete();
                } catch (Exception e) {
                }
            }
            String rustFile = getRustFile(dir.getAbsolutePath());
            Files.write(version.toPath(),newVersionId.getBytes());
            return rustFile;
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