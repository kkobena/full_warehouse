package com.kobe.warehouse.service.settings;

import com.kobe.warehouse.config.FileStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final Path fileImageStorageLocation;
    private final Path filePharmamlStorageLocation;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();
        this.fileImageStorageLocation = Paths.get(fileStorageProperties.getImagesDir()).toAbsolutePath().normalize();
        this.filePharmamlStorageLocation = Paths.get(fileStorageProperties.getPharmamlDir()).toAbsolutePath().normalize();

        Logger LOG = LoggerFactory.getLogger(FileStorageService.class);
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            LOG.debug("Imposile de créer le repertoire ", ex);
        }
        try {
            Files.createDirectories(this.fileImageStorageLocation);
        } catch (IOException ex) {
            LOG.debug("Imposile de créer le repertoire ", ex);
        }
        try {
            Files.createDirectories(this.filePharmamlStorageLocation);
        } catch (IOException ex) {
            LOG.debug("Imposile de créer le repertoire ", ex);
        }
    }


    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }

    public Path getFileImageStorageLocation() {
        return fileImageStorageLocation;
    }

    public Path getFilePharmamlStorageLocation() {
        return filePharmamlStorageLocation;
    }
}
