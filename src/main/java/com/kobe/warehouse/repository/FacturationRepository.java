package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant_;
import com.kobe.warehouse.domain.GroupeTiersPayant_;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.service.facturation.dto.DossierFactureGroupProjection;
import com.kobe.warehouse.service.facturation.dto.DossierFactureSingleProjection;
import com.kobe.warehouse.service.facturation.dto.FacturationDossier;
import com.kobe.warehouse.service.facturation.dto.FacturationGroupeDossier;
import com.kobe.warehouse.service.facturation.dto.InvoiceSearchParams;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Repository
public interface FacturationRepository
    extends
        JpaRepository<FactureTiersPayant, FactureItemId>, JpaSpecificationExecutor<FactureTiersPayant>, FactureTiersPayantRepositoryCustom {
    @Query(value = "SELECT f.num_facture FROM facture_tiers_payant f  ORDER BY f.id DESC LIMIT 1", nativeQuery = true)
    String findLatestFactureNumber();

    @Query("SELECT o FROM  FactureTiersPayant o WHERE o.generationCode=:generationCode AND o.invoiceDate >=:invoiceDate  ")
    List<FactureTiersPayant> findAll(
        @Param("generationCode") Integer generationCode,
        @Param("invoiceDate") LocalDate invoiceDate,
        Sort sort
    );

    @Query(
        "SELECT o FROM  FactureTiersPayant o WHERE o.generationCode=:generationCode AND o.invoiceDate >=:invoiceDate AND o.groupeFactureTiersPayant IS NULL "
    )
    List<FactureTiersPayant> findAllByGenerationCodeAndGroupeFactureTiersPayantIsNull(
        @Param("generationCode") Integer generationCode,
        @Param("invoiceDate") LocalDate invoiceDate,
        Sort sort
    );

    default Specification<FactureTiersPayant> fetchByIs(Set<FactureItemId> ids) {
        return (root, _, cb) -> {
            In<Long> selectionIds = cb.in(root.get(FactureTiersPayant_.id));
            ids.forEach(factureItemId -> selectionIds.value(factureItemId.getId()));
            In<LocalDate> localDateIn = cb.in(root.get(FactureTiersPayant_.invoiceDate));
            ids.forEach(factureItemId -> localDateIn.value(factureItemId.getInvoiceDate()));
            return cb.and(selectionIds, localDateIn);
        };
    }

    @Query(
        value = """
            SELECT
                f.groupeFactureTiersPayant.id AS parentId,
                f.groupeFactureTiersPayant.invoiceDate AS parentInvoiceDate,
                f.invoiceDate AS invoiceDate,
                f.id AS id,
                f.numFacture AS numFacture,
                f.debutPeriode AS debutPeriode,
                f.finPeriode AS finPeriode,
                tp.fullName AS organismeName,
                f.montantRegle AS montantPaye,
                SUM(s.montant) AS montantTotal,
                SUM(s.montantRegle) AS montantDetailRegle,
                COUNT(s.id) AS itemsCount

            FROM FactureTiersPayant f
            JOIN f.tiersPayant tp
            JOIN f.facturesDetails s
            WHERE f.groupeFactureTiersPayant.id = :id
              AND f.statut <> 'PAID'
              AND f.invoiceDate = :invoiceDate
              AND s.statut <> 'PAID'
            GROUP BY f.invoiceDate,f.groupeFactureTiersPayant.id, f.groupeFactureTiersPayant.invoiceDate , f.id, f.numFacture, f.debutPeriode, f.finPeriode, tp.fullName, f.montantRegle
            ORDER BY f.id
        """,
        countQuery = """
            SELECT COUNT(f.id)
            FROM FactureTiersPayant f
            WHERE f.groupeFactureTiersPayant.id = :id
              AND f.statut <> 'PAID'
              AND f.invoiceDate = :invoiceDate
        """
    )
    Page<FacturationGroupeDossier> findGroupeFactureById(
        @Param("id") Long id,
        @Param("invoiceDate") LocalDate invoiceDate,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT
                f.invoiceDate AS invoiceDate,
                s.createdAt AS saleDate,
                t.id AS id,
                s.numBon AS bonNumber,
                CONCAT(cu.firstName, ' ', cu.lastName) AS customerFullName,
                c.num AS matricule,
                f.created AS facturationDate,
                t.montantRegle AS montantPaye,
                t.montant AS montantTotal,
                f.id AS parentId

            FROM ThirdPartySaleLine t
            JOIN t.clientTiersPayant c
            JOIN c.assuredCustomer cu
            JOIN t.sale s
            JOIN t.factureTiersPayant f
            WHERE t.statut <> 'PAID'
              AND f.id = :id
              AND f.statut <> 'PAID'
              AND f.invoiceDate = :invoiceDate

            ORDER BY t.id

        """,
        countQuery = """
            SELECT COUNT(t.id)
            FROM ThirdPartySaleLine t
            JOIN t.factureTiersPayant f
            WHERE t.statut <> 'PAID'
              AND f.id = :id
                AND f.statut <> 'PAID'
              AND f.invoiceDate = :invoiceDate
        """
    )
    Page<FacturationDossier> findFacturationDossierByFactureId(
        @Param("id") Long id,
        @Param("invoiceDate") LocalDate invoiceDate,
        Pageable pageable
    );

    @Query(
        """
            SELECT
                f.id AS id,
                f.invoiceDate AS invoiceDate,
                f.created AS facturationDate,
                g.name AS name,
                f.numFacture AS numFacture,
                SUM(s.montantRegle) AS montantDetailRegle,
                SUM(f.montantRegle) AS montantPaye,
                SUM(s.montant) AS montantTotal,
                COUNT(s.id) as itemCount

            FROM FactureTiersPayant f
            JOIN f.groupeTiersPayant g
            JOIN f.tiersPayant tp
            JOIN f.facturesDetails s
            WHERE f.id = :id
              AND f.statut <> 'PAID'
              AND s.statut <> 'PAID'
               AND f.invoiceDate = :invoiceDate
            GROUP BY f.id,f.invoiceDate, f.created, g.name, f.numFacture
        """
    )
    DossierFactureGroupProjection findGroupDossierFacture(@Param("id") Long id, @Param("invoiceDate") LocalDate invoiceDate);

    @Query(
        """
            SELECT
                f.id AS id,
                f.invoiceDate AS invoiceDate,
                f.created AS facturationDate,
                COUNT(t.id) AS itemCount,
                f.montantRegle AS montantPaye,
                SUM(t.montant) AS montantTotal,
                SUM(t.montantRegle) AS montantDetailRegle,
                tp.categorie AS categorie,
                tp.name AS name,
                f.numFacture AS numFacture

            FROM FactureTiersPayant f
            JOIN f.tiersPayant tp
            JOIN f.facturesDetails t
            WHERE f.statut <> 'PAID'
                AND t.statut <> 'PAID'
                AND f.invoiceDate = :invoiceDate
              AND f.id = :id
            GROUP BY f.invoiceDate ,f.id, f.created, f.montantRegle, tp.categorie, tp.name, f.numFacture
        """
    )
    DossierFactureSingleProjection findSingleDossierFacture(@Param("id") Long id, @Param("invoiceDate") LocalDate invoiceDate);

    default Specification<FactureTiersPayant> aFacture(InvoiceSearchParams invoiceSearchParams) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(invoiceSearchParams.search())) {
                var search = "%" + invoiceSearchParams.search() + "%";
                predicates.add(
                    cb.or(
                        cb.like(root.get(FactureTiersPayant_.numFacture), search),
                        cb.like(root.get(FactureTiersPayant_.tiersPayant).get(TiersPayant_.fullName), search)
                    )
                );
            } else {
                predicates.add(
                    cb.between(root.get(FactureTiersPayant_.invoiceDate), invoiceSearchParams.startDate(), invoiceSearchParams.endDate())
                );
            }

            if (!CollectionUtils.isEmpty(invoiceSearchParams.tiersPayantIds())) {
                predicates.add(root.get(FactureTiersPayant_.tiersPayant).get(TiersPayant_.id).in(invoiceSearchParams.tiersPayantIds()));
            }
            predicates.add(cb.equal(root.get(FactureTiersPayant_.factureProvisoire), invoiceSearchParams.factureProvisoire()));
            if (!CollectionUtils.isEmpty(invoiceSearchParams.statuts())) {
                predicates.add(root.get(FactureTiersPayant_.statut).in(invoiceSearchParams.statuts()));
            }
            predicates.add(root.get(FactureTiersPayant_.groupeFactureTiersPayant).isNull());

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    default Specification<FactureTiersPayant> aGroupedFacture(InvoiceSearchParams invoiceSearchParams) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(invoiceSearchParams.search())) {
                var search = "%" + invoiceSearchParams.search() + "%";
                predicates.add(
                    cb.or(
                        cb.like(root.get(FactureTiersPayant_.numFacture), search),
                        cb.like(root.get(FactureTiersPayant_.groupeTiersPayant).get(GroupeTiersPayant_.name), search)
                    )
                );
            } else {
                predicates.add(
                    cb.between(root.get(FactureTiersPayant_.invoiceDate), invoiceSearchParams.startDate(), invoiceSearchParams.endDate())
                );
            }

            if (!CollectionUtils.isEmpty(invoiceSearchParams.groupIds())) {
                predicates.add(
                    root.get(FactureTiersPayant_.groupeTiersPayant).get(GroupeTiersPayant_.id).in(invoiceSearchParams.groupIds())
                );
            }
            predicates.add(cb.equal(root.get(FactureTiersPayant_.factureProvisoire), invoiceSearchParams.factureProvisoire()));
            if (!CollectionUtils.isEmpty(invoiceSearchParams.statuts())) {
                predicates.add(root.get(FactureTiersPayant_.statut).in(invoiceSearchParams.statuts()));
            }
            predicates.add(root.get(FactureTiersPayant_.groupeTiersPayant).isNotNull());

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
