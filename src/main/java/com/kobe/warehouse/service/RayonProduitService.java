package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.dto.RayonProduitDTO;
import com.kobe.warehouse.web.rest.errors.GenericError;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RayonProduitService {

    private final RayonProduitRepository rayonProduitRepository;
    private final RayonRepository rayonRepository;
    private final StockProduitRepository stockProduitRepository;

    public RayonProduitService(
        RayonProduitRepository rayonProduitRepository,
        RayonRepository rayonRepository,
        StockProduitRepository stockProduitRepository
    ) {
        this.rayonProduitRepository = rayonProduitRepository;
        this.rayonRepository = rayonRepository;
        this.stockProduitRepository = stockProduitRepository;
    }

    public Optional<RayonProduitDTO> create(RayonProduitDTO dto) throws GenericError {
        Rayon rayon = rayonRepository.getReferenceById(dto.getRayonId());
        long error = rayonProduitRepository.countRayonProduitByProduitIdAndRayonId(dto.getProduitId(), dto.getRayonId());
        if (error > 0) {
            throw new GenericError("Le produit est déjà rattaché à ce rayon dans ce stockage",
                "duplicateProduitRayon");
        }

        error = rayonProduitRepository.countRayonProduitByProduitIdAndStockageId(dto.getProduitId(), rayon.getStorage().getId());
        if (error > 0) {
            throw new GenericError("Le produit est déjà rattaché à un autre rayon dans ce stockage",
                "duplicateProduitStockage");
        }
        RayonProduit rayonProduit = new RayonProduit();
        rayonProduit.setProduit(new Produit().id(dto.getProduitId()));
        rayonProduit.setRayon(rayon);
        return Optional.ofNullable(rayonProduitRepository.saveAndFlush(rayonProduit)).map(RayonProduitDTO::new);
    }

    public void delete(long id) throws GenericError {
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
