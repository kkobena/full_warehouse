package com.kobe.warehouse.service;

import java.net.MalformedURLException;
import java.nio.file.Paths;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class FileResourceService {

    public Resource getResource(String path) throws MalformedURLException {
        return new UrlResource(Paths.get(path).toUri());
    }
}
