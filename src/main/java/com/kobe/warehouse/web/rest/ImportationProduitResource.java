package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Importation;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.ImportationStatus;
import com.kobe.warehouse.domain.enumeration.ImportationType;
import com.kobe.warehouse.service.ImportationProduitService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.ResponseDTO;
import io.github.jhipster.web.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;


@RestController
@RequestMapping("/api/importation")
public class ImportationProduitResource {
    private final ImportationProduitService importationProduitService;
    private final UserService userService;

    public ImportationProduitResource(ImportationProduitService importationProduitService,UserService userService) {
        this.importationProduitService = importationProduitService;
        this.userService=userService;
    }

    @PostMapping("importjson")
    public ResponseEntity<Void> uploadFile(@RequestPart("importjson") MultipartFile file) throws URISyntaxException, IOException {
        User user=userService.getUser();
        importationProduitService.updateStocFromJSON(file.getInputStream(),user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/result")
    public ResponseEntity<ResponseDTO> getCuurent() {
        Importation importation = importationProduitService.current(ImportationType.STOCK_PRODUIT);
        if (importation == null) {
            return ResponseUtil.wrapOrNotFound(Optional.of(new ResponseDTO().setCompleted(true)));
        }
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setSize(importation.getSize());
        responseDTO.setTotalSize(importation.getTotalZise());
        if (importation.getImportationStatus() != ImportationStatus.PROCESSING){
            responseDTO.setCompleted(true) ;
        }

        return ResponseUtil.wrapOrNotFound(Optional.of(responseDTO));

    }
}
