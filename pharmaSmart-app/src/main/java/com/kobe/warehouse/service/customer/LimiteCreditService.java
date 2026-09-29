package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.LimiteCreditDerogation;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.repository.LimiteCreditDerogationRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Limite de crédit des ventes différées (docs/PLAN-FICHE-CLIENT.md, lot 3) : une seule, pour toute
 * l'officine ({@code APP_LIMITE_CREDIT_CLIENT}, 0 = aucune). Une vente différée qui porterait
 * l'encours du client au-delà est refusée à la clôture, sauf dérogation tracée pour cette vente —
 * par le droit {@value #DROIT_DEROGATION} ou la clé d'un collègue qui le détient.
 */
@Service
@Transactional
public class LimiteCreditService {

    public static final String DROIT_DEROGATION = "pr-depasser-limite-credit";
    public static final String ERROR_KEY = "limiteCreditDepassee";

    private final AppConfigurationService appConfigurationService;
    private final SalesRepository salesRepository;
    private final LimiteCreditDerogationRepository derogationRepository;
    private final DerogationAuthorizer derogationAuthorizer;

    public LimiteCreditService(
        AppConfigurationService appConfigurationService,
        SalesRepository salesRepository,
        LimiteCreditDerogationRepository derogationRepository,
        DerogationAuthorizer derogationAuthorizer
    ) {
        this.appConfigurationService = appConfigurationService;
        this.salesRepository = salesRepository;
        this.derogationRepository = derogationRepository;
        this.derogationAuthorizer = derogationAuthorizer;
    }

    /** Situation du client face à la limite ; {@code disponible} est nul quand il n'y a pas de limite. */
    @Transactional(readOnly = true)
    public SituationCreditDTO situation(Integer customerId) {
        int limite = appConfigurationService.getLimiteCreditClient();
        BigDecimal solde = salesRepository.getDiffereSoldeByCustomerId(customerId);
        long encours = solde == null ? 0 : solde.longValue();
        return new SituationCreditDTO(limite, encours, limite > 0 ? Math.max(0, limite - encours) : null);
    }

    /** Appelé à la clôture d'une vente : refuse une vente différée qui dépasserait la limite sans dérogation. */
    public void controlerCloture(Sales vente) {
        Integer restant = vente.getRestToPay();
        if (!vente.isDiffere() || vente.getCustomer() == null || restant == null || restant <= 0) {
            return;
        }
        int limite = appConfigurationService.getLimiteCreditClient();
        if (limite <= 0) {
            return;
        }
        Integer customerId = vente.getCustomer().getId();
        long encours = salesRepository.getDiffereSoldeHorsVente(customerId, vente.getId().getId(), vente.getSaleDate());
        if (encours + restant <= limite || derogationRepository.existsBySaleIdAndSaleDate(vente.getId().getId(), vente.getSaleDate())) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", customerId);
        payload.put("saleId", vente.getId().getId());
        payload.put("saleDate", vente.getSaleDate().toString());
        payload.put("montant", restant);
        payload.put("encours", encours);
        payload.put("limite", limite);
        throw new GenericError(
            "Limite de crédit dépassée : l'encours du client passerait à %d pour une limite de %d.".formatted(encours + restant, limite),
            ERROR_KEY,
            payload
        );
    }

    /** Autorise, pour une vente donnée, la vente à crédit au-delà de la limite. */
    public void deroger(Integer customerId, DerogationLimiteCreditDTO demande) {
        int limite = appConfigurationService.getLimiteCreditClient();
        long encours = salesRepository.getDiffereSoldeHorsVente(customerId, demande.saleId(), demande.saleDate());
        if (limite <= 0 || encours + demande.montant() <= limite) {
            throw new GenericError("Cette vente ne dépasse pas la limite de crédit.", "limiteCreditNonDepassee");
        }
        AppUser autorisePar = derogationAuthorizer.autoriser(
            DROIT_DEROGATION,
            demande.actionAuthorityKey(),
            "la vente à crédit au-delà de la limite",
            "limiteCredit"
        );
        derogationRepository.save(
            new LimiteCreditDerogation()
                .setCustomerId(customerId)
                .setSaleId(demande.saleId())
                .setSaleDate(demande.saleDate())
                .setMontant(demande.montant())
                .setEncours((int) encours)
                .setLimite(limite)
                .setMotif(demande.motif().trim())
                .setUser(derogationAuthorizer.utilisateurCourant())
                .setAutorisePar(autorisePar)
        );
    }
}
