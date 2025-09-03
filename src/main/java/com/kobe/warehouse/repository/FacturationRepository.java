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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    List<FactureTiersPayant> findAllByCreatedEquals(LocalDateTime created, Sort sort);
    @Deprecated(forRemoval = true)
    Optional<FactureTiersPayant> findFactureTiersPayantById(Long id);

    List<FactureTiersPayant> findAllByCreatedEqualsAndGroupeFactureTiersPayantIsNull(LocalDateTime created, Sort sort);

    default Specification<FactureTiersPayant> fetchByIs(Set<Long> ids) {
        return (root, _, cb) -> {
            In<Long> selectionIds = cb.in(root.get(FactureTiersPayant_.id));
            ids.forEach(selectionIds::value);
            return selectionIds;
        };
    }

    @Query(
        value = "SELECT f.groupe_facture_tiers_payant_id AS parentId, f.id AS id,f.num_facture  AS numFacture,f.debut_periode  AS debutPeriode,f.fin_periode AS finPeriode,tp.full_name AS organismeName,  f.montant_regle as montantPaye,items.montantTotal AS montantTotal,items.montantDetailRegle AS montantDetailRegle,COUNT(items.facture_tiers_payant_id) AS itemsCount FROM  facture_tiers_payant f JOIN (SELECT s.facture_tiers_payant_id,  SUM(s.montant) AS montantTotal,SUM(s.montant_regle) as montantDetailRegle FROM third_party_sale_line s WHERE s.statut NOT IN('PAID') group by s.facture_tiers_payant_id) AS items ON f.id=items.facture_tiers_payant_id JOIN tiers_payant tp ON f.tiers_payant_id = tp.id " +
        " WHERE f.groupe_facture_tiers_payant_id =:id AND f.statut NOT IN ('PAID') group by f.id ORDER BY  f.id  ",
        countQuery = "SELECT COUNT(f.id) FROM  facture_tiers_payant f WHERE f.groupe_facture_tiers_payant_id =:id AND f.statut NOT IN ('PAID')",
        nativeQuery = true
    )
    Page<FacturationGroupeDossier> findGroupeFactureById(@Param("id") Long id, Pageable pageable);

    @Query(
        value = "SELECT s.created_at AS saleDate, t.id AS id, s.num_bon AS bonNumber ,CONCAT(cu.first_name,' ',cu.last_name) AS customerFullName,c.num AS matricule,f.created  AS facturationDate,t.montant_regle  AS montantPaye,t.montant  AS montantTotal,t.facture_tiers_payant_id as parentId FROM third_party_sale_line t  JOIN  client_tiers_payant c ON c.id=t.client_tiers_payant_id" +
        " JOIN customer cu ON c.assured_customer_id = cu.id JOIN sales s ON s.id=t.sale_id  JOIN facture_tiers_payant f ON f.id=t.facture_tiers_payant_id  WHERE t.statut NOT IN ('PAID') AND f.id =:id  ",
        countQuery = "SELECT COUNT(t.id)  FROM third_party_sale_line t WHERE t.statut NOT IN ('PAID') AND t.facture_tiers_payant_id =:id",
        nativeQuery = true
    )
    Page<FacturationDossier> findFacturationDossierByFactureId(@Param("id") Long id, Pageable pageable);

    @Query(
        value = "SELECT f.id AS id, f.created AS facturationDate, g.name AS name,f.num_facture AS numFacture,SUM(singleFacture.montantDetailRegle) AS montantDetailRegle,SUM(singleFacture.montantPaye) AS montantPaye,SUM(singleFacture.montantTotal) AS montantTotal,COUNT(singleFacture.id) as itemCount FROM  facture_tiers_payant f JOIN groupe_tiers_payant g ON f.groupe_tiers_payant_id = g.id " +
        " JOIN (SELECT f.groupe_facture_tiers_payant_id , f.id,f.montant_regle as montantPaye,items.montantTotal AS montantTotal,items.montantDetailRegle AS montantDetailRegle FROM  facture_tiers_payant f JOIN (SELECT s.facture_tiers_payant_id," +
        "SUM(s.montant) AS montantTotal,SUM(s.montant_regle) as montantDetailRegle FROM third_party_sale_line s WHERE s.statut NOT IN('PAID') group by s.facture_tiers_payant_id) AS items ON f.id=items.facture_tiers_payant_id JOIN tiers_payant tp ON f.tiers_payant_id = tp.id " +
        " WHERE   f.statut NOT IN ('PAID') group by f.id ORDER BY  f.id) as singleFacture ON f.id=singleFacture.groupe_facture_tiers_payant_id where f.id=:id",
        nativeQuery = true
    )
    DossierFactureGroupProjection findGroupDossierFacture(@Param("id") Long id);

    @Query(
        value = " SELECT f.id AS id,f.created AS facturationDate,  COUNT(items.id) as itemCount, f.montant_regle AS montantPaye, SUM(items.montantTotal) AS montantTotal, SUM(items.montantPaye) AS montantDetailRegle,tp.categorie AS categorie,tp.name  AS name,f.num_facture AS numFacture from  facture_tiers_payant f JOIN  tiers_payant tp ON f.tiers_payant_id = tp.id" +
        " JOIN (SELECT t.id, t.facture_tiers_payant_id,t.montant_regle  AS montantPaye,t.montant  AS montantTotal FROM third_party_sale_line t   WHERE  t.facture_tiers_payant_id  IS NOT NULL ) as items ON f.id=items.facture_tiers_payant_id   WHERE f.statut NOT IN ('PAID') AND  f.id =:id  ",
        nativeQuery = true
    )
    DossierFactureSingleProjection findSingleDossierFacture(@Param("id") Long id);

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
                    cb.between(
                        root.get(FactureTiersPayant_.created),
                        invoiceSearchParams.startDate().atStartOfDay(),
                        invoiceSearchParams.endDate().atTime(23, 59, 59)
                    )
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
                    cb.between(
                        root.get(FactureTiersPayant_.created),
                        invoiceSearchParams.startDate().atStartOfDay(),
                        invoiceSearchParams.endDate().atTime(23, 59, 59)
                    )
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
