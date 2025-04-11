package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.repository.DeliveryReceiptItemRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.service.dto.EtatProduit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EtatProduitServiceImpl implements EtatProduitService {

    private final SuggestionLineRepository suggestionLineRepository;
    private final OrderLineRepository orderLineRepository;
    private final DeliveryReceiptItemRepository deliveryReceiptItemRepository;

    public EtatProduitServiceImpl(
        SuggestionLineRepository suggestionLineRepository,
        OrderLineRepository orderLineRepository,
        DeliveryReceiptItemRepository deliveryReceiptItemRepository
    ) {
        this.suggestionLineRepository = suggestionLineRepository;
        this.orderLineRepository = orderLineRepository;
        this.deliveryReceiptItemRepository = deliveryReceiptItemRepository;
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

    private EtatProduit buildEtatProduit(Long idProduit, int currentStock) {
        boolean stockPositif = currentStock > 0;
        boolean stockNegatif = currentStock < 0;
        boolean stockZero = currentStock == 0;
        int suggestionCount = suggestionLineRepository.countByFournisseurProduitProduitId(idProduit);
        int commandeCount = orderLineRepository.countByFournisseurProduitProduitIdAndCommandeOrderStatus(
            idProduit,
            OrderStatut.REQUESTED
        );
        boolean entree = deliveryReceiptItemRepository.existsByFournisseurProduitProduitIdAndDeliveryReceiptReceiptStatut(
            idProduit,
            ReceiptStatut.PENDING
        );
        return new EtatProduit(stockPositif, stockNegatif, stockZero, suggestionCount>0, commandeCount>0, entree,suggestionCount>1,commandeCount>1);
    }
}
