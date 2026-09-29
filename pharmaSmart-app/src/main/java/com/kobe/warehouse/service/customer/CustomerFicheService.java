package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereWrapperDTO;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Données de la fiche client « 360° » (docs/PLAN-FICHE-CLIENT.md, lot 1). Toutes les lectures sont
 * bornées au client : la fiche ne doit pas ouvrir, par ce biais, les écrans des différés ou des
 * ventes à qui n'y a pas accès.
 */
@Service
@Transactional(readOnly = true)
public class CustomerFicheService {

    /** Ventes qui comptent : clôturées et non annulées. */
    private static final String VENTES_DU_CLIENT = "s.customer.id = :customerId AND s.statut = :statut AND s.canceled = false";

    private final EntityManager em;
    private final SalesRepository salesRepository;
    private final ReglementDiffereService reglementDiffereService;

    public CustomerFicheService(EntityManager em, SalesRepository salesRepository, ReglementDiffereService reglementDiffereService) {
        this.em = em;
        this.salesRepository = salesRepository;
        this.reglementDiffereService = reglementDiffereService;
    }

    public CustomerSyntheseDTO synthese(Integer customerId) {
        Object[] douzeMois = em
            .createQuery(
                "SELECT COUNT(s), COALESCE(SUM(s.salesAmount), 0L) FROM Sales s WHERE " + VENTES_DU_CLIENT + " AND s.saleDate >= :debut",
                Object[].class
            )
            .setParameter("customerId", customerId)
            .setParameter("statut", SalesStatut.CLOSED)
            .setParameter("debut", LocalDate.now().minusMonths(12))
            .getSingleResult();
        LocalDateTime derniereVisite = em
            .createQuery("SELECT MAX(s.updatedAt) FROM Sales s WHERE " + VENTES_DU_CLIENT, LocalDateTime.class)
            .setParameter("customerId", customerId)
            .setParameter("statut", SalesStatut.CLOSED)
            .getSingleResult();
        BigDecimal encours = salesRepository.getDiffereSoldeByCustomerId(customerId);
        return new CustomerSyntheseDTO(
            customerId,
            encours == null ? 0 : encours.longValue(),
            derniereVisite,
            ((Number) douzeMois[0]).longValue(),
            ((Number) douzeMois[1]).longValue()
        );
    }

    /** Produits délivrés sur la période, du plus récemment délivré au plus ancien. */
    public Page<ProduitDelivreDTO> produitsDelivres(Integer customerId, LocalDate fromDate, LocalDate toDate, String search, Pageable pageable) {
        String filtre =
            " FROM SalesLine l JOIN l.sales s JOIN l.produit p WHERE " +
            VENTES_DU_CLIENT +
            " AND s.saleDate BETWEEN :debut AND :fin" +
            (StringUtils.hasText(search) ? " AND UPPER(p.libelle) LIKE :search" : "");

        TypedQuery<Long> count = em.createQuery("SELECT COUNT(DISTINCT p.id)" + filtre, Long.class);
        TypedQuery<ProduitDelivreDTO> query = em.createQuery(
            "SELECT new com.kobe.warehouse.service.customer.ProduitDelivreDTO(p.id, p.libelle, COUNT(l), " +
            "COALESCE(SUM(l.quantitySold), 0L), COALESCE(SUM(l.salesAmount), 0L), MAX(s.updatedAt))" +
            filtre +
            " GROUP BY p.id, p.libelle ORDER BY MAX(s.updatedAt) DESC",
            ProduitDelivreDTO.class
        );
        for (TypedQuery<?> q : List.of(count, query)) {
            q.setParameter("customerId", customerId);
            q.setParameter("statut", SalesStatut.CLOSED);
            q.setParameter("debut", fromDate);
            q.setParameter("fin", toDate);
            if (StringUtils.hasText(search)) {
                q.setParameter("search", "%" + search.trim().toUpperCase(Locale.ROOT) + "%");
            }
        }
        long total = count.getSingleResult();
        if (total == 0) {
            return Page.empty(pageable);
        }
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return new PageImpl<>(query.getResultList(), pageable, total);
    }

    /** Ventes différées non soldées du client ; vide s'il n'en a pas. */
    public Optional<DiffereDTO> differes(Integer customerId) {
        // getOne échoue sur un client sans différé, et son exception marque la transaction pour
        // annulation : un catch ici ne suffirait pas. On ne l'appelle donc qu'avec un reste dû.
        BigDecimal solde = salesRepository.getDiffereSoldeByCustomerId(customerId);
        if (solde == null || solde.signum() == 0) {
            return Optional.empty();
        }
        return reglementDiffereService.getOne(customerId);
    }

    public Page<ReglementDiffereWrapperDTO> reglementsDifferes(Integer customerId, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return reglementDiffereService.getReglementsDifferes(customerId, fromDate, toDate, pageable);
    }
}
