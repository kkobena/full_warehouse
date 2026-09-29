package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.ReinitialisationConsommation;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ReinitialisationConsommationRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remise à zéro de la consommation face au plafond (règle du 2026-09-29) :
 * <ul>
 *   <li>plafond <b>non absolu</b> : la facture définitive remet le compteur à zéro ; son annulation le rend ;</li>
 *   <li>plafond <b>absolu</b> : chaque règlement réduit le compteur du montant réglé ; son annulation le rend.</li>
 * </ul>
 * Compteur de l'adhérent régi par {@code plafondAbsoluClient}, celui de l'organisme par {@code plafondAbsolu}.
 */
@Service
@Transactional
public class ConsommationPlafondService {

    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final TiersPayantRepository tiersPayantRepository;
    private final ReinitialisationConsommationRepository reinitialisationRepository;

    public ConsommationPlafondService(
        ClientTiersPayantRepository clientTiersPayantRepository,
        TiersPayantRepository tiersPayantRepository,
        ReinitialisationConsommationRepository reinitialisationRepository
    ) {
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.tiersPayantRepository = tiersPayantRepository;
        this.reinitialisationRepository = reinitialisationRepository;
    }

    /** Facture définitive : remet à zéro les compteurs à plafond non absolu, en gardant ce qui est effacé. */
    public void reinitialiserALaFacturation(FactureTiersPayant facture, List<ThirdPartySaleLine> dossiers) {
        if (facture.isFactureProvisoire()) {
            return;
        }
        TiersPayant organisme = facture.getTiersPayant();
        FactureItemId id = facture.getId();
        if (!organisme.isPlafondAbsoluClient()) {
            Set<ClientTiersPayant> adherents = new LinkedHashSet<>();
            dossiers.forEach(d -> adherents.add(d.getClientTiersPayant()));
            for (ClientTiersPayant adherent : adherents) {
                long conso = valeur(adherent.getConsoMensuelle());
                if (conso != 0) {
                    reinitialisationRepository.save(trace(id, conso).setClientTiersPayantId(adherent.getId()));
                    adherent.setConsoMensuelle(0L);
                    clientTiersPayantRepository.save(adherent);
                }
            }
        }
        if (!organisme.isPlafondAbsolu()) {
            long conso = valeur(organisme.getConsoMensuelle());
            if (conso != 0) {
                reinitialisationRepository.save(trace(id, conso).setTiersPayantId(organisme.getId()));
                organisme.setConsoMensuelle(0L);
                tiersPayantRepository.save(organisme);
            }
        }
    }

    /** Annulation d'une facture : rend aux compteurs ce que sa création avait effacé. */
    public void annulerReinitialisation(FactureItemId id) {
        List<ReinitialisationConsommation> traces = reinitialisationRepository.findAllByFactureIdAndFactureDate(
            id.getId(),
            id.getInvoiceDate()
        );
        for (ReinitialisationConsommation trace : traces) {
            if (trace.getClientTiersPayantId() != null) {
                clientTiersPayantRepository
                    .findById(trace.getClientTiersPayantId())
                    .ifPresent(adherent -> {
                        adherent.setConsoMensuelle(valeur(adherent.getConsoMensuelle()) + trace.getMontant());
                        clientTiersPayantRepository.save(adherent);
                    });
            } else {
                tiersPayantRepository
                    .findById(trace.getTiersPayantId())
                    .ifPresent(organisme -> {
                        organisme.setConsoMensuelle(valeur(organisme.getConsoMensuelle()) + trace.getMontant());
                        tiersPayantRepository.save(organisme);
                    });
            }
        }
        reinitialisationRepository.deleteAll(traces);
    }

    /** Règlement d'un dossier (montant positif) ou son annulation (montant négatif), pour les plafonds absolus. */
    public void imputerReglement(ThirdPartySaleLine dossier, int montantRegle) {
        if (montantRegle == 0) {
            return;
        }
        ClientTiersPayant adherent = dossier.getClientTiersPayant();
        TiersPayant organisme = adherent.getTiersPayant();
        if (organisme.isPlafondAbsoluClient()) {
            adherent.setConsoMensuelle(Math.max(valeur(adherent.getConsoMensuelle()) - montantRegle, 0L));
            clientTiersPayantRepository.save(adherent);
        }
        if (organisme.isPlafondAbsolu()) {
            organisme.setConsoMensuelle(Math.max(valeur(organisme.getConsoMensuelle()) - montantRegle, 0L));
            tiersPayantRepository.save(organisme);
        }
    }

    private static ReinitialisationConsommation trace(FactureItemId id, long montant) {
        return new ReinitialisationConsommation().setFactureId(id.getId()).setFactureDate(id.getInvoiceDate()).setMontant(montant);
    }

    private static long valeur(Number conso) {
        return Objects.requireNonNullElse(conso, 0L).longValue();
    }
}
