package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.service.ProduitDuService;
import com.kobe.warehouse.service.dto.ClientsEnAttenteDTO;
import com.kobe.warehouse.service.dto.ProduitDuNonCommandeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProduitDuServiceImpl implements ProduitDuService {

    private final AvoirClientRepository avoirClientRepository;

    public ProduitDuServiceImpl(AvoirClientRepository avoirClientRepository) {
        this.avoirClientRepository = avoirClientRepository;
    }

    @Override
    public List<ProduitDuNonCommandeDTO> lireProduitsDusNonCommandes() {
        List<Object[]> lignes = avoirClientRepository.lireProduitsDusNonCommandes();
        List<ProduitDuNonCommandeDTO> produits = new ArrayList<>(lignes.size());
        for (Object[] ligne : lignes) {
            produits.add(
                new ProduitDuNonCommandeDTO(
                    (Integer) ligne[0],
                    (String) ligne[1],
                    (String) ligne[2],
                    ((Number) ligne[3]).longValue(),
                    ((Number) ligne[4]).longValue(),
                    ((Number) ligne[5]).longValue(),
                    (LocalDateTime) ligne[6],
                    ((Number) ligne[7]).intValue()
                )
            );
        }
        return produits;
    }

    @Override
    public Map<Integer, ClientsEnAttenteDTO> lireClientsEnAttente(Set<Integer> produitIds) {
        if (produitIds == null || produitIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> lignes = avoirClientRepository.lireClientsEnAttente(produitIds);
        Map<Integer, ClientsEnAttenteDTO> parProduit = new HashMap<>(lignes.size());
        for (Object[] ligne : lignes) {
            Integer produitId = (Integer) ligne[0];
            parProduit.put(
                produitId,
                new ClientsEnAttenteDTO(
                    produitId,
                    ((Number) ligne[1]).longValue(),
                    ((Number) ligne[2]).longValue(),
                    ((Number) ligne[3]).longValue()
                )
            );
        }
        return parProduit;
    }
}
