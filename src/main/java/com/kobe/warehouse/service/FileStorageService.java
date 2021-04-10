package com.kobe.warehouse.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.web.rest.AjustementResource;
import com.kobe.warehouse.web.rest.errors.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {
    private final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);
	private final   Path fileStorageLocation;
    private final   Path fileImageStorageLocation;


    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir())
            .toAbsolutePath().normalize();
        this.fileImageStorageLocation=Paths.get(fileStorageProperties.getImagesDir())
            .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            LOG.debug("Imposile de créer le repertoire ",ex);
        }
        try {
            Files.createDirectories(this.fileImageStorageLocation);
        } catch (IOException ex) {
            LOG.debug("Imposile de créer le repertoire ",ex);
        }
    }
	public String storeFile(MultipartFile file) throws  FileNotFoundException{
		String fileName = StringUtils.cleanPath(file.getOriginalFilename());
		try {
			// Check if the file's name contains invalid characters
			if (fileName.contains("..")) {
				throw new FileNotFoundException("Sorry! Filename contains invalid path sequence " + fileName);
			}

			// Copy file to the target location (Replacing existing file with the same name)

			Path targetLocation =    this.fileStorageLocation.resolve(fileName).normalize();
			Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
			return fileName;
		} catch (IOException ex) {
			throw new FileNotFoundException("Could not store file " + fileName + ". Please try again!");
		}
	}

	public Resource loadFileAsResource(String fileName)  throws  FileNotFoundException{
		try {

            Path targetLocation =     this.fileStorageLocation.resolve(fileName).normalize();
			Resource resource = new UrlResource(targetLocation.toUri());
			if (resource.exists()) {
				return resource;
			} else {
				throw new FileNotFoundException("File not found " + fileName);
			}
		} catch (IOException ex) {
            throw new FileNotFoundException("Fichier existant");
		}
	}

	public File loadFile(String fileName) throws IOException {

        Path targetLocation =     this.fileStorageLocation.resolve(fileName).normalize();
		File resource = targetLocation.toFile();
		if (resource.exists()) {
			return resource;
		} else {
			throw new FileNotFoundException("Fichier existant");
		}
	}

    public String storeImage(MultipartFile file) throws  FileNotFoundException{
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new FileNotFoundException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)

            Path targetLocation =    this.fileImageStorageLocation.resolve(fileName).normalize();
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetLocation.toFile().getAbsolutePath();
        } catch (IOException ex) {
            throw new FileNotFoundException("Could not store file " + fileName + ". Please try again!");
        }
    }

}
