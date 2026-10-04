package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.enumeration.TypeSubstitut;
import com.kobe.warehouse.repository.SubstitutRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.SubstitutionComptoirService;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import com.kobe.warehouse.service.stock.dto.SubstitutPropose;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubstitutionComptoirServiceImpl implements SubstitutionComptoirService {

    private final SubstitutRepository substitutRepository;
    private final ProduitService produitService;
    private final StorageService storageService;

    public SubstitutionComptoirServiceImpl(
        SubstitutRepository substitutRepository,
        ProduitService produitService,
        StorageService storageService
    ) {
        this.substitutRepository = substitutRepository;
        this.produitService = produitService;
        this.storageService = storageService;
    }

    @Override
    public List<SubstitutPropose> lireSubstitutsDisponibles(Integer produitId) {
        // La table s'écrit dans un sens et se lit dans les deux : « A est un générique de B » vaut « B est l'équivalent de A ».
        Map<Integer, TypeSubstitut> types = new LinkedHashMap<>();
        substitutRepository.findAllByProduitId(produitId).forEach(s -> types.putIfAbsent(s.getSubstitut().getId(), s.getType()));
        substitutRepository.findAllBySubstitutId(produitId).forEach(s -> types.putIfAbsent(s.getProduit().getId(), s.getType()));
        types.remove(produitId);
        if (types.isEmpty()) {
            return List.of();
        }
        Integer magasinId = storageService.getConnectedUserMagasin().getId();
        List<SubstitutPropose> proposes = new ArrayList<>();
        for (ProduitSearch produit : produitService.findSearchByIds(new ArrayList<>(types.keySet()), magasinId)) {
            // Rien à proposer d'un produit qu'on ne peut pas servir : même règle que la vente (stock du rayon).
            if (produit.totalQuantity() > 0) {
                TypeSubstitut type = types.get(produit.id());
                proposes.add(new SubstitutPropose(produit, type.name(), type.getLibelle()));
            }
        }
        proposes.sort(
            Comparator.comparing((SubstitutPropose p) -> TypeSubstitut.valueOf(p.typeSubstitut()))
                .thenComparing(p -> p.produit().regularunitprice() == null ? Integer.MAX_VALUE : p.produit().regularunitprice())
                .thenComparing(p -> p.produit().libelle())
        );
        return proposes;
    }
}
