package com.kobe.warehouse.web.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static ResponseEntity<Resource> printPDF(String gereratefilePath,
        HttpServletRequest request)
        throws MalformedURLException {
        Path filePath = Paths.get(gereratefilePath);
        Resource resource = new UrlResource(filePath.toUri());
        return getDocument(resource, "application/pdf", request);
    }

    public static ResponseEntity<Resource> printPDF(Resource resource,
        HttpServletRequest request) {
        return getDocument(resource, "application/pdf", request);
    }


    public static ResponseEntity<Resource> exportCsv(Resource resource,
        HttpServletRequest request) {
        return getDocument(resource, "text/csv", request);

    }

    private static ResponseEntity<Resource> getDocument(Resource resource,
        String defaultContentType,
        HttpServletRequest request) {
        String contentType = null;
        try {
            contentType = request.getServletContext()
                .getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        if (contentType == null) {
            contentType = defaultContentType;

        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }
}
