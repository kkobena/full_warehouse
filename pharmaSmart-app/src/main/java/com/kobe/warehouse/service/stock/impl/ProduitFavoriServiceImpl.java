package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ProduitFavori;
import com.kobe.warehouse.repository.ProduitFavoriRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.stock.ProduitFavoriService;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.dto.FavoriSuggere;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProduitFavoriServiceImpl implements ProduitFavoriService {

    private static final String ENTITY_NAME = "produitFavori";

    /** Au-delà, la rangée de tuiles du comptoir déborde de l'écran. */
    static final int MAX_FAVORIS = 12;

    private final ProduitFavoriRepository produitFavoriRepository;
    private final ProduitRepository produitRepository;
    private final ProduitService produitService;
    private final StorageService storageService;

    public ProduitFavoriServiceImpl(
        ProduitFavoriRepository produitFavoriRepository,
        ProduitRepository produitRepository,
        ProduitService produitService,
        StorageService storageService
    ) {
        this.produitFavoriRepository = produitFavoriRepository;
        this.produitRepository = produitRepository;
        this.produitService = produitService;
        this.storageService = storageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitSearch> lister() {
        Integer magasinId = storageService.getConnectedUserMagasin().getId();
        return produitService.findSearchByIds(produitFavoriRepository.findProduitIdsByMagasinId(magasinId), magasinId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriSuggere> suggerer(int limite) {
        Integer magasinId = storageService.getConnectedUserMagasin().getId();
        List<Object[]> lignes = produitFavoriRepository.findSuggestions(magasinId, limite);
        if (lignes.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = lignes.stream().map(l -> ((Number) l[0]).intValue()).toList();
        Map<Integer, ProduitSearch> parId = new HashMap<>();
        produitService.findSearchByIds(ids, magasinId).forEach(p -> parId.put(p.id(), p));
        List<FavoriSuggere> suggestions = new ArrayList<>();
        for (Object[] ligne : lignes) {
            ProduitSearch produit = parId.get(((Number) ligne[0]).intValue());
            if (produit != null) {
                suggestions.add(new FavoriSuggere(produit, ((Number) ligne[1]).longValue(), ((Number) ligne[2]).longValue()));
            }
        }
        return suggestions;
    }

    @Override
    public void ajouter(Integer produitId) {
        Magasin magasin = storageService.getConnectedUserMagasin();
        if (produitFavoriRepository.findByMagasinIdAndProduitId(magasin.getId(), produitId).isPresent()) {
            return;
        }
        if (produitFavoriRepository.countByMagasinId(magasin.getId()) >= MAX_FAVORIS) {
            throw new BadRequestAlertException(
                "La grille est pleine : " + MAX_FAVORIS + " favoris au maximum. Retirez-en un avant d'en ajouter.",
                ENTITY_NAME,
                "grillepleine"
            );
        }
        Produit produit = produitRepository
            .findById(produitId)
            .orElseThrow(() -> new BadRequestAlertException("Produit introuvable", ENTITY_NAME, "idnotfound"));
        produitFavoriRepository.save(
            new ProduitFavori().setMagasin(magasin).setProduit(produit).setOrdre(produitFavoriRepository.maxOrdre(magasin.getId()) + 1)
        );
    }

    @Override
    public void retirer(Integer produitId) {
        Integer magasinId = storageService.getConnectedUserMagasin().getId();
        produitFavoriRepository.findByMagasinIdAndProduitId(magasinId, produitId).ifPresent(produitFavoriRepository::delete);
    }

    @Override
    public void reordonner(List<Integer> produitIds) {
        Integer magasinId = storageService.getConnectedUserMagasin().getId();
        Map<Integer, ProduitFavori> parProduit = new LinkedHashMap<>();
        for (ProduitFavori favori : produitFavoriRepository.findAllByMagasinIdOrderByOrdreAscIdAsc(magasinId)) {
            parProduit.put(favori.getProduit().getId(), favori);
        }
        // Les produits cités d'abord, dans l'ordre cité ; puis les oubliés, dans leur ordre actuel.
        List<ProduitFavori> rangee = new ArrayList<>();
        for (Integer id : produitIds == null ? List.<Integer>of() : produitIds) {
            ProduitFavori favori = parProduit.remove(id);
            if (favori != null) {
                rangee.add(favori);
            }
        }
        rangee.addAll(parProduit.values());
        for (int i = 0; i < rangee.size(); i++) {
            rangee.get(i).setOrdre(i + 1);
        }
    }
}
