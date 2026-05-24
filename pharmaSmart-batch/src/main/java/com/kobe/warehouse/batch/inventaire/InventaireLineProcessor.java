package com.kobe.warehouse.batch.inventaire;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Valide et convertit une ligne CSV brute en {@link InventaireLigneValidee}.
 * Les lignes invalides retournent {@code null} → Spring Batch les filtre automatiquement.
 */
@Component
public class InventaireLineProcessor implements ItemProcessor<InventaireLigneRaw, InventaireLigneValidee> {

    private static final Logger LOG = LoggerFactory.getLogger(InventaireLineProcessor.class);

    @Override
    public InventaireLigneValidee process(InventaireLigneRaw raw) {
        if (raw.getCip13() == null || raw.getCip13().isBlank()) {
            LOG.warn("[INVENTAIRE] Ligne ignorée : CIP13 manquant");
            return null;
        }
        int quantite;
        try {
            quantite = Integer.parseInt(raw.getQuantite().trim());
        } catch (NumberFormatException e) {
            LOG.warn("[INVENTAIRE] Ligne ignorée : quantité invalide '{}' pour cip13={}",
                raw.getQuantite(), raw.getCip13());
            return null;
        }
        LocalDate peremption = null;
        if (raw.getPeremption() != null && !raw.getPeremption().isBlank()) {
            try {
                peremption = LocalDate.parse(raw.getPeremption().trim());
            } catch (DateTimeParseException e) {
                LOG.warn("[INVENTAIRE] Date péremption invalide '{}' — ignorée pour cip13={}",
                    raw.getPeremption(), raw.getCip13());
            }
        }
        return new InventaireLigneValidee(raw.getCip13().trim(), raw.getLot(), quantite, peremption);
    }
}
