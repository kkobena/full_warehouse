package com.kobe.warehouse.service.referentiel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Rapproche les produits après l'ajout de DCI au catalogue.
 *
 * <p>Après le commit, et hors du fil de la requête : l'utilisateur qui crée une DCI n'attend pas le
 * rapprochement, et un échec ici ne peut pas défaire son ajout — le passage de nuit rattrape.
 */
@Component
public class DcisAjouteesListener {

    private static final Logger LOG = LoggerFactory.getLogger(DcisAjouteesListener.class);

    private final RapprochementProduitService rapprochementProduitService;

    public DcisAjouteesListener(RapprochementProduitService rapprochementProduitService) {
        this.rapprochementProduitService = rapprochementProduitService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void traiterDcisAjoutees(DcisAjouteesEvent event) {
        try {
            rapprochementProduitService.rapprocherApresAjoutDci(event.dciIds());
        } catch (Exception e) {
            LOG.warn("[REFERENTIEL] Rapprochement après ajout de DCI impossible : {}", e.getMessage(), e);
        }
    }
}
