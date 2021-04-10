package com.kobe.warehouse.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
    @Value("${file.report}")
    private String reportsDir;
    @Value("${file.images}")
    private String imagesDir;

    public String getReportsDir() {
        return reportsDir;
    }

    public String getImagesDir() {
        return imagesDir;
    }
}
