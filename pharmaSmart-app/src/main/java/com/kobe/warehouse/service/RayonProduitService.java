package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.dto.CloneRayonProduitsDTO;
import com.kobe.warehouse.service.dto.RayonProduitBatchMoveDTO;
import com.kobe.warehouse.service.dto.RayonProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.errors.GenericError;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RayonProduitService {

    private static final String SANS_RAYON_CODE = "SANS";

    private final Logger log = LoggerFactory.getLogger(RayonProduitService.class);
    private final RayonProduitRepository rayonProduitRepository;
    private final RayonRepository rayonRepository;
    private final StockProduitRepository stockProduitRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

    /**
     * Affecte un produit à un rayon.
     * - Si le produit est sans rayon (SANS) → transfert automatique.
     * - Si le produit est dans un autre rayon réel → erreur avec payload pour confirmation UI.
     * - Si le produit est déjà dans ce rayon → erreur doublon.
     */
    public Optional<RayonProduitDTO> assign(RayonProduitDTO dto) throws GenericError {
        Rayon targetRayon = rayonRepository.getReferenceById(dto.getRayonId());

        if (rayonProduitRepository.countRayonProduitByProduitIdAndRayonId(dto.getProduitId(), dto.getRayonId()) > 0) {
            throw new GenericError("Le produit est déjà rattaché à ce rayon", "duplicateProduitRayon");
        }

        Optional<RayonProduit> existing = rayonProduitRepository.findByProduitIdAndStorageId(
            dto.getProduitId(),
            targetRayon.getStorage().getId()
        );

        if (existing.isPresent()) {
            RayonProduit currentAffectation = existing.get();
            Rayon currentRayon = currentAffectation.getRayon();
            if (SANS_RAYON_CODE.equals(currentRayon.getCode())) {
                removeAndEvict(currentAffectation);
            } else {
                throw new GenericError(
                    "Le produit est déjà rattaché au rayon \"" + currentRayon.getLibelle() + "\"",
                    "duplicateProduitStockage",
                    Map.of("rayonCode", currentRayon.getCode(), "rayonLibelle", currentRayon.getLibelle())
                );
            }
        }

        RayonProduit rayonProduit = new RayonProduit();
        rayonProduit.setProduit(new Produit().id(dto.getProduitId()));
        rayonProduit.setRayon(targetRayon);
        return Optional.of(rayonProduitRepository.saveAndFlush(rayonProduit)).map(RayonProduitDTO::new);
    }

    /**
     * Déplace un produit vers un autre rayon sans vérification de stock.
     * Appelé après confirmation explicite de l'utilisateur.
     */
    public Optional<RayonProduitDTO> move(RayonProduitDTO dto) throws GenericError {
        Rayon targetRayon = rayonRepository.getReferenceById(dto.getRayonId());

        rayonProduitRepository.findByProduitIdAndStorageId(dto.getProduitId(), targetRayon.getStorage().getId())
            .ifPresent(this::removeAndEvict);

        RayonProduit rayonProduit = new RayonProduit();
        rayonProduit.setProduit(new Produit().id(dto.getProduitId()));
        rayonProduit.setRayon(targetRayon);
        return Optional.of(rayonProduitRepository.saveAndFlush(rayonProduit)).map(RayonProduitDTO::new);
    }

    /**
     * Retire un produit d'un rayon. Bloqué si le produit est en stock.
     */
    public void delete(Integer id) throws GenericError {
        RayonProduit rayonProduit = rayonProduitRepository.getReferenceById(id);
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(
            rayonProduit.getProduit().getId(),
            rayonProduit.getRayon().getStorage().getId()
        );
        if (stockProduit != null && (stockProduit.getQtyStock() > 0 || stockProduit.getQtyUG() > 0)) {
            throw new GenericError("Le produit est en stock dans ce rayon", "stockConflic");
        }
        removeAndEvict(rayonProduit);
    }

    public void moveBatch(RayonProduitBatchMoveDTO dto) throws GenericError {
        Rayon targetRayon = rayonRepository.getReferenceById(dto.rayonId());
        for (Integer produitId : dto.produitIds()) {
            rayonProduitRepository.findByProduitIdAndStorageId(produitId, targetRayon.getStorage().getId())
                .ifPresent(this::removeAndEvict);
            RayonProduit rayonProduit = new RayonProduit();
            rayonProduit.setProduit(new Produit().id(produitId));
            rayonProduit.setRayon(targetRayon);
            rayonProduitRepository.saveAndFlush(rayonProduit);
        }
    }

    public ResponseDTO cloneToRayons(CloneRayonProduitsDTO dto) {
        List<RayonProduit> sourceAssignments = rayonProduitRepository.findAllByRayonId(dto.sourceRayonId());
        int created = 0;
        int skipped = 0;

        for (Integer targetRayonId : dto.targetRayonIds()) {
            Rayon targetRayon = rayonRepository.getReferenceById(targetRayonId);
            Integer targetStorageId = targetRayon.getStorage().getId();

            for (RayonProduit rp : sourceAssignments) {
                Integer produitId = rp.getProduit().getId();
                Optional<RayonProduit> existing = rayonProduitRepository.findByProduitIdAndStorageId(produitId, targetStorageId);
                if (existing.isPresent()) {
                    if (SANS_RAYON_CODE.equals(existing.get().getRayon().getCode())) {
                        removeAndEvict(existing.get());
                    } else {
                        skipped++;
                        continue;
                    }
                }
                RayonProduit newRp = new RayonProduit();
                newRp.setProduit(rp.getProduit());
                newRp.setRayon(targetRayon);
                rayonProduitRepository.save(newRp);
                created++;
            }
        }

        entityManager.flush();
        return new ResponseDTO()
            .size(created)
            .totalSize(created + skipped)
            .success(true)
            .message(created + " affectation(s) créées, " + skipped + " ignorée(s) (déjà affectées)");
    }

    public ResponseDTO importFromCsv(InputStream inputStream, Integer storageId) {
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger lineIndex = new AtomicInteger(0);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').get().parse(br);
            for (CSVRecord record : records) {
                if (lineIndex.getAndIncrement() == 0) continue;
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

    // Supprime un RayonProduit et évicte le Produit parent du cache L2 (entité + collection).
    // Sans ça, la collection Produit.rayonProduits reste stale en cache Caffeine
    // et cause ObjectNotFoundException au prochain findAll().
    private void removeAndEvict(RayonProduit rayonProduit) {
        Integer produitId = rayonProduit.getProduit().getId();
        rayonProduitRepository.delete(rayonProduit);
        rayonProduitRepository.flush();
        org.hibernate.Cache cache = entityManager
            .unwrap(org.hibernate.Session.class)
            .getSessionFactory()
            .getCache();
        cache.evictEntityData(Produit.class, produitId);
        cache.evictCollectionData("com.kobe.warehouse.domain.Produit.rayonProduits", produitId);
    }
}
