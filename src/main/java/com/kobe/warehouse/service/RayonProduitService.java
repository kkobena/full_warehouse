package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.dto.RayonProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RayonProduitService {

    private final Logger log = LoggerFactory.getLogger(RayonProduitService.class);
    private final RayonProduitRepository rayonProduitRepository;
    private final RayonRepository rayonRepository;
    private final StockProduitRepository stockProduitRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;

    public RayonProduitService(
        RayonProduitRepository rayonProduitRepository,
        RayonRepository rayonRepository,
        StockProduitRepository stockProduitRepository,
        FournisseurProduitRepository fournisseurProduitRepository
    ) {
        this.rayonProduitRepository = rayonProduitRepository;
        this.rayonRepository = rayonRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.fournisseurProduitRepository = fournisseurProduitRepository;
    }

    public Optional<RayonProduitDTO> create(RayonProduitDTO dto) throws GenericError {
        Rayon rayon = rayonRepository.getReferenceById(dto.getRayonId());
        long error = rayonProduitRepository.countRayonProduitByProduitIdAndRayonId(dto.getProduitId(), dto.getRayonId());
        if (error > 0) {
            throw new GenericError("Le produit est déjà rattaché à ce rayon dans ce stockage", "duplicateProduitRayon");
        }

        error = rayonProduitRepository.countRayonProduitByProduitIdAndStockageId(dto.getProduitId(), rayon.getStorage().getId());
        if (error > 0) {
            throw new GenericError("Le produit est déjà rattaché à un autre rayon dans ce stockage", "duplicateProduitStockage");
        }
        RayonProduit rayonProduit = new RayonProduit();
        rayonProduit.setProduit(new Produit().id(dto.getProduitId()));
        rayonProduit.setRayon(rayon);
        return Optional.ofNullable(rayonProduitRepository.saveAndFlush(rayonProduit)).map(RayonProduitDTO::new);
    }

    @Transactional
    public ResponseDTO importFromCsv(InputStream inputStream, Integer storageId) {
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger lineIndex = new AtomicInteger(0);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').get().parse(br);
            for (CSVRecord record : records) {
                if (lineIndex.getAndIncrement() == 0) continue; // skip header
                if (record.size() < 2) { errors.incrementAndGet(); continue; }
                String codeCip = record.get(0).trim();
                String rayonCode = record.get(1).trim();
                if (codeCip.isEmpty() || rayonCode.isEmpty()) { errors.incrementAndGet(); continue; }

                var fp = fournisseurProduitRepository.findFirstByCodeCip(codeCip);
                if (fp.isEmpty()) { errors.incrementAndGet(); continue; }

                var rayon = rayonRepository.findFirstByCodeAndStorageId(rayonCode, storageId);
                if (rayon.isEmpty()) { errors.incrementAndGet(); continue; }

                Integer produitId = fp.get().getProduit().getId();
                
                if (rayonProduitRepository.countRayonProduitByProduitIdAndStockageId(produitId, storageId) > 0) {
                    skipped.incrementAndGet();
                    continue;
                }
                RayonProduit rp = new RayonProduit();
                rp.setProduit(fp.get().getProduit());
                rp.setRayon(rayon.get());
                rayonProduitRepository.save(rp);
                success.incrementAndGet();
            }
        } catch (IOException e) {
            log.error("importRayonProduits", e);
        }
        int total = success.get() + errors.get() + skipped.get();
        return new ResponseDTO()
            .size(success.get())
            .totalSize(total)
            .setErrorSize(errors.get())
            .success(errors.get() == 0)
            .message(success.get() + " affectation(s) créées, " + errors.get() + " erreur(s), " + skipped.get() + " ignorée(s)");
    }

    public void delete(Integer id) throws GenericError {
        RayonProduit rayonProduit = rayonProduitRepository.getReferenceById(id);
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(
            id,
            rayonProduit.getRayon().getStorage().getId()
        );
        if (stockProduit != null && (stockProduit.getQtyStock() > 0 || stockProduit.getQtyUG() > 0)) {
            throw new GenericError("Le produit est en stock dans ce rayon", "stockConflic");
        }

        rayonProduitRepository.delete(rayonProduit);
    }
}
