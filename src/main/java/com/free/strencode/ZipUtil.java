package com.free.strencode;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import net.lingala.zip4j.ZipFile;

public class ZipUtil {
    public static void unzip(String zipFilePath, String destDir) throws IOException {
        new ZipFile(zipFilePath).extractAll(destDir);
    }
}