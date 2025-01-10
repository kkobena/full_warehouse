package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Importation;
import com.kobe.warehouse.domain.enumeration.ImportationStatus;
import com.kobe.warehouse.domain.enumeration.ImportationType;
import com.kobe.warehouse.service.ImportationProduitService;
import com.kobe.warehouse.service.dto.InstallationDataDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api/importation")
public class ImportationProduitResource {

    private final ImportationProduitService importationProduitService;

    public ImportationProduitResource(ImportationProduitService importationProduitService) {
        this.importationProduitService = importationProduitService;
    }

    @PostMapping("importjson")
    public ResponseEntity<Void> uploadFile(@RequestPart("importjson") MultipartFile file) throws URISyntaxException, IOException {
        importationProduitService.updateStocFromJSON(file.getInputStream());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/result")
    public ResponseEntity<ResponseDTO> getCurrent() {
        Importation importation = importationProduitService.current(ImportationType.STOCK_PRODUIT);
        if (importation == null) {
            return ResponseUtil.wrapOrNotFound(Optional.of(new ResponseDTO().setCompleted(true)));
        }
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setSize(importation.getSize());
        responseDTO.setTotalSize(importation.getTotalZise());
        if (importation.getImportationStatus() != ImportationStatus.PROCESSING) {
            responseDTO.setCompleted(true);
        }

        return ResponseUtil.wrapOrNotFound(Optional.of(responseDTO));
    }

    @PostMapping(path = "importcsv", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ResponseDTO> importCsv(
        @RequestPart("fichier") MultipartFile file,
        @RequestPart("data") InstallationDataDTO installationData
    ) throws IOException {
        return ResponseEntity.ok(importationProduitService.installNewOfficine(file.getInputStream(), installationData));
    }

    @GetMapping("/rejet-csv/{nom-fichier}")
    public ResponseEntity<Resource> getRejetCsv(@PathVariable("nom-fichier") String nomFichier, HttpServletRequest request) {
        final Resource resource = importationProduitService.getRejets(nomFichier);
        return Utils.exportCsv(resource, request);
    }
}
