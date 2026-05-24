package com.free.strencode;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

public class LogUtil {
    public static void log(String msg) {
        long ts = Instant.now().getEpochSecond();
        String line = "[" + ts + "] " + msg;
        // 控制台输出
        System.out.println(line);
        // 写文件
        try (FileWriter fw = new FileWriter("rust_jni.log", true); PrintWriter pw = new PrintWriter(fw)) {
            pw.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
