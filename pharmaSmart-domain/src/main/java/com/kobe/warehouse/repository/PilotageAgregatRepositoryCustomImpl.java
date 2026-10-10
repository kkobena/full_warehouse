package com.kobe.warehouse.repository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Appels natifs : les agrégats s'écrivent par fonctions PL/pgSQL, que JPQL ne sait pas appeler. */
@Repository
public class PilotageAgregatRepositoryCustomImpl implements PilotageAgregatRepositoryCustom {

    private static final String JOURS_MODIFIES =
        """
        SELECT sale_date AS jour FROM sales WHERE updated_at >= :depuis
        UNION
        SELECT receipt_date FROM commande WHERE updated_at >= :depuis AND receipt_date IS NOT NULL
        UNION
        SELECT transaction_date FROM payment_transaction WHERE created_at >= :depuis
        ORDER BY jour
        """;

    private final EntityManager em;

    public PilotageAgregatRepositoryCustomImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public void recalculer(LocalDate du, LocalDate au) {
        em.createNativeQuery("SELECT pilotage_recalculer(:du, :au)").setParameter("du", du).setParameter("au", au).getSingleResult();
    }

    @Override
    public void photographierStock(LocalDate jour) {
        em.createNativeQuery("SELECT pilotage_photographier_stock(:jour)").setParameter("jour", jour).getSingleResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LocalDate> listerJoursModifiesDepuis(LocalDateTime depuis) {
        return em.createNativeQuery(JOURS_MODIFIES, LocalDate.class).setParameter("depuis", depuis).getResultList();
    }
}
