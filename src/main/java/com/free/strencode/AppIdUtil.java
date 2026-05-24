package com.free.strencode;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.MessageDigest;

public class AppIdUtil {
        public static String getAppId(Class<?> mainClass) {
            String appName = getAppName();
            String appPath = getAppPath(mainClass);
            return Md5Util.md5(appName + "|" + appPath+"|"+getPort());
        }
        private static String getAppName() {
            String name = System.getProperty("spring.application.name");
            if (name != null && !name.isEmpty()) {
                return name;
            }
            String cmd = System.getProperty("sun.java.command");
            if (cmd != null && !cmd.isEmpty()) {
                return cmd.split("\\s+")[0];
            }
            return "unknown-app";
        }

        private static String getAppPath(Class<?> mainClass) {
            try {
                CodeSource codeSource = mainClass.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    URI uri = codeSource.getLocation().toURI();
                    return new File(uri)
                            .getCanonicalPath()
                            .replace("\\", "/");
                }
            } catch (Exception ignored) {}
            try {
                return new File(System.getProperty("user.dir"))
                        .getCanonicalPath()
                        .replace("\\", "/");
            } catch (Exception e) {
                return System.getProperty("user.dir");
            }
        }

    /**
     * 获取应用端口
     */
    private static Integer getPort() {
        // spring boot
        String port = System.getProperty("server.port");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (Exception ignored) {
            }
        }
        // 环境变量
        port = System.getenv("SERVER_PORT");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

}
