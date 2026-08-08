package com.kobe.warehouse.service.inventaire.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.records.ImportLineErrorRecord;
import com.kobe.warehouse.service.dto.records.ImportResultRecord;
import com.kobe.warehouse.service.inventaire.InventaireImportService;
import com.kobe.warehouse.service.inventaire.InventoryStockService;
import java.io.InputStreamReader;
import java.io.Reader;
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
    private final InventoryStockService inventoryStockService;
    private final UserService userService;

    public InventaireImportServiceImpl(
        StoreInventoryLineRepository storeInventoryLineRepository,
        InventoryStockService inventoryStockService,
        UserService userService
    ) {
        this.storeInventoryLineRepository = storeInventoryLineRepository;
        this.inventoryStockService = inventoryStockService;
        this.userService = userService;
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

        applyCounts(lines, codeCipQuantity);
        storeInventoryLineRepository.saveAll(lines);

        return new ImportResultRecord(lines.size(), ignored, errors.size(), errors);
    }

    /**
     * Compte les lignes comme le ferait la saisie écran : quantité importée, quantité initiale
     * relue du stock théorique, valorisation et traçabilité. Se contenter de poser
     * {@code quantityOnHand} laissait {@code quantity_init} et la valorisation à NULL, ce qui
     * faisait échouer la clôture et sortait la ligne des filtres d'écart.
     */
    private void applyCounts(List<StoreInventoryLine> lines, Map<String, Integer> codeCipQuantity) {
        // Toutes les lignes appartiennent au même inventaire (requête filtrée sur son id).
        StoreInventory inventory = lines.getFirst().getStoreInventory();
        Set<Integer> produitIds = lines.stream().map(line -> line.getProduit().getId())
            .collect(Collectors.toSet());
        Map<Integer, Integer> stockMap = inventoryStockService.buildStockMapForInventory(inventory,
            produitIds);
        AppUser countedBy = userService.getUser();

        lines.forEach(line -> line.applyCount(
            stockMap.getOrDefault(line.getProduit().getId(), 0),
            resolveQuantity(codeCipQuantity, line.getProduit()),
            countedBy
        ));
    }

    private int resolveQuantity(Map<String, Integer> codeCipQuantity, Produit produit) {
        return produit.getFournisseurProduits().stream()
            .map(fp -> codeCipQuantity.get(fp.getCodeCip()))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(0);
    }
}
