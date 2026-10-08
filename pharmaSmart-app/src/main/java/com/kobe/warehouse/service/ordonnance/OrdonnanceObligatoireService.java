package com.kobe.warehouse.service.ordonnance;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.repository.OrdonnanceVenteRepository;
import com.kobe.warehouse.repository.VentePrescripteurRepository;
import com.kobe.warehouse.domain.ordonnance.VentePrescripteur;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Une vente qui contient un produit dont le statut légal exige une ordonnance doit porter une
 * ordonnance ou un prescripteur : sans l'un des deux, sa clôture est refusée.
 */
@Service
@Transactional(readOnly = true)
public class OrdonnanceObligatoireService {

    public static final String ERROR_KEY = "ordonnanceRequise";

    private final AppConfigurationService appConfigurationService;
    private final OrdonnanceVenteRepository ordonnanceVenteRepository;
    private final VentePrescripteurRepository ventePrescripteurRepository;

    public OrdonnanceObligatoireService(
        AppConfigurationService appConfigurationService,
        OrdonnanceVenteRepository ordonnanceVenteRepository,
        VentePrescripteurRepository ventePrescripteurRepository
    ) {
        this.appConfigurationService = appConfigurationService;
        this.ordonnanceVenteRepository = ordonnanceVenteRepository;
        this.ventePrescripteurRepository = ventePrescripteurRepository;
    }

    /** Appelé à la clôture d'une vente ; la vente dépôt n'est pas une dispensation au public. */
    public void controlerCloture(Sales vente) {
        if (vente.getTypePrescription() == TypePrescription.DEPOT || !appConfigurationService.isOrdonnanceObligatoireExigee()) {
            return;
        }
        List<String> produits = vente
            .getSalesLines()
            .stream()
            .filter(l -> l.getQuantitySold() != null && l.getQuantitySold() > 0 && l.getProduit() != null && l.getProduit().isOrdonnanceObligatoire())
            .map((SalesLine l) -> l.getProduit().getLibelle())
            .distinct()
            .toList();
        if (produits.isEmpty()) {
            return;
        }
        Long salesId = Objects.requireNonNull(vente.getId()).getId();
        if (
            ordonnanceVenteRepository.existsParVente(salesId, vente.getSaleDate()) ||
            ventePrescripteurRepository.existsById(new VentePrescripteur.Id(salesId, vente.getSaleDate()))
        ) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("saleId", salesId);
        payload.put("saleDate", vente.getSaleDate().toString());
        payload.put("customerId", vente.getCustomer() == null ? null : vente.getCustomer().getId());
        payload.put("produits", produits);
        throw new GenericError(
            "Une ordonnance ou un prescripteur est obligatoire pour : " + String.join(", ", produits) + ".",
            ERROR_KEY,
            payload
        );
    }
}
