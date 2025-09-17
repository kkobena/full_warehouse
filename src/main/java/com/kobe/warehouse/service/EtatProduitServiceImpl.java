package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.service.dto.EtatProduit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class EtatProduitServiceImpl implements EtatProduitService {

    private final SuggestionLineRepository suggestionLineRepository;
    private final OrderLineRepository orderLineRepository;
    private final AppConfigurationService appConfigurationService;

    public EtatProduitServiceImpl(SuggestionLineRepository suggestionLineRepository, OrderLineRepository orderLineRepository, AppConfigurationService appConfigurationService) {
        this.suggestionLineRepository = suggestionLineRepository;
        this.orderLineRepository = orderLineRepository;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public EtatProduit getEtatProduit(Long idProduit, int currentStock) {
        return buildEtatProduit(idProduit, currentStock);
    }

    @Override
    public EtatProduit getEtatProduit(Produit produit) {
        int currentStock = produit.getStockProduits().stream().map(StockProduit::getTotalStockQuantity).reduce(0, Integer::sum);
        return buildEtatProduit(produit.getId(), currentStock);
    }

    @Override
    public boolean canSuggere(Long idProduit) {
        return getCommandeCount(idProduit, OrderStatut.REQUESTED) == 0 && getCommandeCount(idProduit, OrderStatut.RECEIVED) == 0;
    }

    private EtatProduit buildEtatProduit(Long idProduit, int currentStock) {
        boolean stockPositif = currentStock > 0;
        boolean stockNegatif = currentStock < 0;
        boolean stockZero = currentStock == 0;
        int suggestionCount = suggestionLineRepository.countByFournisseurProduitProduitId(idProduit);
        int commandeCount = getCommandeCount(idProduit, OrderStatut.REQUESTED);
        boolean entree = orderLineRepository.existsByFournisseurProduitProduitIdAndCommandeOrderStatusAndCommandeOrderDateGreaterThan(idProduit, OrderStatut.RECEIVED, getDateRetentionCommande());
        return new EtatProduit(
            stockPositif,
            stockNegatif,
            stockZero,
            suggestionCount > 0,
            commandeCount > 0,
            entree,
            suggestionCount > 1,
            commandeCount > 1
        );
    }

    private LocalDate getDateRetentionCommande() {
        return LocalDate.now().plusDays(appConfigurationService.getNombreJourRetentionCommande());
    }

    private int getCommandeCount(Long idProduit, OrderStatut orderStatut) {
        LocalDate dateRetentionCommande = getDateRetentionCommande();
        return orderLineRepository.countByFournisseurProduitProduitIdAndCommandeOrderStatusAndCommandeOrderDateGreaterThan(idProduit, orderStatut, dateRetentionCommande);
    }
}
