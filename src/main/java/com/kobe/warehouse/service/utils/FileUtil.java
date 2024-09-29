package com.kobe.warehouse.service.utils;

import java.util.Objects;

public final class FileUtil {

    public static final String CSV = "csv";
    public static final String TXT = "txt";

    private FileUtil() {}

    public static String getFileExtension(String fileName) {
        Objects.requireNonNull(fileName, "File name is required");
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
