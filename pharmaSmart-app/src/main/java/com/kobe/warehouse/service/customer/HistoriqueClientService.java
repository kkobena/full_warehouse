package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.service.errors.GenericError;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Règle de gestion (docs/PLAN-FICHE-CLIENT.md, décision n° 5 du 2026-09-30) : un client qui a un
 * historique ne se supprime pas, il se désactive. Ses délivrances doivent rester traçables.
 *
 * <p>Un assuré principal emporte ses ayants droit à la suppression : leur historique compte donc
 * aussi.
 */
@Service
@Transactional(readOnly = true)
public class HistoriqueClientService {

    public static final String ERROR_KEY = "clientAvecHistorique";

    private final EntityManager em;

    public HistoriqueClientService(EntityManager em) {
        this.em = em;
    }

    /** Refuse la suppression si le client, ou l'un de ses ayants droit, a un historique. */
    public void verifierSuppression(Integer customerId) {
        List<Integer> ids = new ArrayList<>();
        ids.add(customerId);
        ids.addAll(
            em
                .createQuery("SELECT c.id FROM AssuredCustomer c WHERE c.assurePrincipal.id = :id", Integer.class)
                .setParameter("id", customerId)
                .getResultList()
        );
        List<String> elements = new ArrayList<>();
        ajouter(elements, compter("SELECT COUNT(*) FROM sales WHERE customer_id IN (:ids) OR ayant_droit_id IN (:ids)", ids), "vente(s)");
        ajouter(elements, compter("SELECT COUNT(*) FROM payment_transaction WHERE differecustomer_id IN (:ids)", ids), "règlement(s) de différé");
        ajouter(elements, compter("SELECT COUNT(*) FROM avoir_client WHERE customer_id IN (:ids)", ids), "avoir(s)");
        ajouter(elements, compter("SELECT COUNT(*) FROM retour_client WHERE customer_id IN (:ids)", ids), "retour(s)");
        if (!elements.isEmpty()) {
            throw new GenericError(
                "Ce client a un historique (" + String.join(", ", elements) + ") : il ne peut pas être supprimé. Désactivez-le.",
                ERROR_KEY
            );
        }
    }

    private long compter(String sql, List<Integer> ids) {
        return ((Number) em.createNativeQuery(sql).setParameter("ids", ids).getSingleResult()).longValue();
    }

    private static void ajouter(List<String> elements, long nombre, String libelle) {
        if (nombre > 0) {
            elements.add(nombre + " " + libelle);
        }
    }
}
