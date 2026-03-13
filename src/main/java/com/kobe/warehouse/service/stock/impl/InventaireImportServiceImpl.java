package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.service.dto.records.ImportLineErrorRecord;
import com.kobe.warehouse.service.dto.records.ImportResultRecord;
import com.kobe.warehouse.service.stock.InventaireImportService;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class InventaireImportServiceImpl implements InventaireImportService {

    private final Logger log = LoggerFactory.getLogger(InventaireImportServiceImpl.class);

    private final StoreInventoryLineRepository storeInventoryLineRepository;

    public InventaireImportServiceImpl(StoreInventoryLineRepository storeInventoryLineRepository) {
        this.storeInventoryLineRepository = storeInventoryLineRepository;
    }

    @Override
    public ImportResultRecord importDetail(Long storeInventoryId, MultipartFile multipartFile) {
        Map<String, Integer> codeCipQuantity = new HashMap<>();
        List<ImportLineErrorRecord> errors = new ArrayList<>();

        CSVFormat csvFormat = CSVFormat.EXCEL.builder()
            .setDelimiter(';')
            .get();

        try (
            Reader reader = new InputStreamReader(multipartFile.getInputStream());
            CSVParser parser = CSVParser.builder()
                .setReader(reader)
                .setFormat(csvFormat)
                .get()
        ) {
            int lineNumber = 0;
            for (CSVRecord record : parser) {
                lineNumber++;

                // Vérification du nombre de colonnes
                if (record.size() < 2) {
                    errors.add(new ImportLineErrorRecord(
                        lineNumber, record.size() > 0 ? record.get(0) : "", "",
                        "Ligne incomplète : " + record.size() + " colonne(s) au lieu de 2"
                    ));
                    continue;
                }

                String code = record.get(0).trim();
                String rawQty = record.get(1).trim();

                if (code.isEmpty()) {
                    errors.add(new ImportLineErrorRecord(lineNumber, code, rawQty,
                        "Code CIP vide"));
                    continue;
                }

                try {
                    int qty = Integer.parseInt(rawQty);
                    codeCipQuantity.put(code, qty);
                } catch (NumberFormatException e) {
                    errors.add(new ImportLineErrorRecord(lineNumber, code, rawQty,
                        "Quantité non numérique : \"" + rawQty + "\""));
                }
            }
        } catch (Exception e) {
            log.error("Erreur lecture fichier CSV import inventaire id={}", storeInventoryId, e);
            return new ImportResultRecord(0, 0, 0,
                List.of(new ImportLineErrorRecord(0, "", "", "Erreur lecture fichier : " + e.getMessage()))
            );
        }

        if (codeCipQuantity.isEmpty()) {
            return new ImportResultRecord(0, 0, errors.size(), errors);
        }

        // Recherche filtrée par inventoryId pour éviter de toucher d'autres inventaires ouverts
        List<StoreInventoryLine> lines = storeInventoryLineRepository
            .findAllByStoreInventoryIdAndCodeCipIn(storeInventoryId, codeCipQuantity.keySet());

        // Ensemble des codes CIP effectivement trouvés dans l'inventaire
        Set<String> foundCips = lines.stream()
            .flatMap(l -> l.getProduit().getFournisseurProduits().stream()
                .map(FournisseurProduit::getCodeCip))
            .collect(Collectors.toSet());

        int ignored = (int) codeCipQuantity.keySet().stream()
            .filter(cip -> !foundCips.contains(cip))
            .count();

        lines.forEach(line -> {
            int qty = resolveQuantity(codeCipQuantity, line.getProduit());
            line.setQuantityOnHand(qty);
            line.setUpdated(true);
            line.setUpdatedAt(LocalDateTime.now());
        });

        storeInventoryLineRepository.saveAll(lines);

        return new ImportResultRecord(lines.size(), ignored, errors.size(), errors);
    }

    private int resolveQuantity(Map<String, Integer> codeCipQuantity, Produit produit) {
        return produit.getFournisseurProduits().stream()
            .map(fp -> codeCipQuantity.get(fp.getCodeCip()))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(0);
    }
}
