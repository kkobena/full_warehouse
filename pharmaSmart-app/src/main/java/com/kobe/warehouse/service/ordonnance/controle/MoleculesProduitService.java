package com.kobe.warehouse.service.ordonnance.controle;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.DecisionRapprochement;
import com.kobe.warehouse.repository.ProduitDciRepository;
import com.kobe.warehouse.repository.ProduitRefSpecialiteRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.dto.controle.MoleculeDTO;
import com.kobe.warehouse.service.dto.controle.MoleculeProduitDTO;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Molécules d'un produit, par les deux chemins du référentiel (plan §6.1) : la DCI portée par le
 * produit, et la composition de sa spécialité retenue. Une proposition {@code A_VERIFIER} ou
 * {@code EN_ATTENTE} n'entre pas : seules les décisions {@code AUTO} et {@code VALIDE} sont de confiance.
 */
@Service
@Transactional(readOnly = true)
public class MoleculesProduitService {

    private static final List<DecisionRapprochement> DECISIONS_DE_CONFIANCE = List.of(DecisionRapprochement.AUTO, DecisionRapprochement.VALIDE);

    private final ProduitDciRepository produitDciRepository;
    private final ProduitRefSpecialiteRepository produitRefSpecialiteRepository;
    private final ProduitRepository produitRepository;

    public MoleculesProduitService(
        ProduitDciRepository produitDciRepository,
        ProduitRefSpecialiteRepository produitRefSpecialiteRepository,
        ProduitRepository produitRepository
    ) {
        this.produitDciRepository = produitDciRepository;
        this.produitRefSpecialiteRepository = produitRefSpecialiteRepository;
        this.produitRepository = produitRepository;
    }

    /** Molécules par produit ; un produit sans molécule exploitable est absent du résultat. */
    public Map<Integer, List<MoleculeDTO>> molecules(Collection<Integer> produitIds) {
        if (produitIds.isEmpty()) {
            return Map.of();
        }
        return Stream.concat(
            produitDciRepository.findMolecules(produitIds).stream(),
            produitRefSpecialiteRepository.findMoleculesRetenues(produitIds, DECISIONS_DE_CONFIANCE).stream()
        )
            .distinct()
            .sorted(Comparator.comparing(MoleculeProduitDTO::produitId).thenComparing(MoleculeProduitDTO::libelle))
            .collect(
                Collectors.groupingBy(
                    MoleculeProduitDTO::produitId,
                    LinkedHashMap::new,
                    Collectors.mapping(m -> new MoleculeDTO(m.refDciId(), m.libelle()), Collectors.toList())
                )
            );
    }

    public Map<Integer, String> libelles(Collection<Integer> produitIds) {
        return produitRepository
            .findAllById(produitIds)
            .stream()
            .sorted(Comparator.comparing(Produit::getId))
            .collect(Collectors.toMap(Produit::getId, Produit::getLibelle, (a, b) -> a, LinkedHashMap::new));
    }
}
