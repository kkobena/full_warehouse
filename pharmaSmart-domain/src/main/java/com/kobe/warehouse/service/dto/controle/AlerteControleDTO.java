package com.kobe.warehouse.service.dto.controle;

import com.kobe.warehouse.domain.pharmacovigilance.NiveauInteraction;
import java.util.List;

/**
 * Alerte du moteur de contrôle.
 *
 * @param type       {@code INTERACTION}, {@code REDONDANCE}, {@code REDONDANCE_CLASSE} ou
 *                   {@code CONTRE_INDICATION}
 * @param conduite   conduite à tenir du référentiel, absente pour une redondance
 * @param source     source et version du référentiel qui fonde l'alerte, absente pour une redondance
 * @param produitIds produits du panier concernés
 * @param bloquant   vrai si le niveau est paramétré pour bloquer ; faux = simple avertissement
 */
public record AlerteControleDTO(
    NiveauInteraction niveau,
    String type,
    String message,
    String conduite,
    String source,
    List<Integer> produitIds,
    boolean bloquant
) {
    /** Alerte telle que produite par le moteur : le caractère bloquant est posé ensuite, d'après les paramètres. */
    public AlerteControleDTO(NiveauInteraction niveau, String type, String message, String conduite, String source, List<Integer> produitIds) {
        this(niveau, type, message, conduite, source, produitIds, false);
    }

    public AlerteControleDTO bloquantSi(boolean bloquant) {
        return new AlerteControleDTO(niveau, type, message, conduite, source, produitIds, bloquant);
    }

    /** Une alerte bloquante exige un motif et le droit de passer outre ; un avertissement, rien. */
    public boolean exigeMotif() {
        return bloquant;
    }
}
