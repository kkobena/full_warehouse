package com.kobe.warehouse.batch.semois;

import com.kobe.warehouse.domain.Produit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;

public class SemoisSkipListener implements SkipListener<Produit, SemoisUpdateResult> {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisSkipListener.class);

    @Override
    public void onSkipInProcess(Produit produit, Throwable t) {
        LOG.warn("[SEMOIS-SKIP] Produit id={} libelle={} ignoré en traitement : {}",
            produit.getId(), produit.getLibelle(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(SemoisUpdateResult result, Throwable t) {
        LOG.error("[SEMOIS-SKIP-WRITE] Écriture ignorée pour produit id={} : {}",
            result.produit().getId(), t.getMessage());
    }

    @Override
    public void onSkipInRead(Throwable t) {
        LOG.warn("[SEMOIS-SKIP-READ] Lecture ignorée : {}", t.getMessage());
    }
}
