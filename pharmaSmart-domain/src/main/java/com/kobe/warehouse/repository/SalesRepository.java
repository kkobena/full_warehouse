package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.dto.SaleInfo;
import com.kobe.warehouse.service.reglement.differe.dto.ClientDiffere;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data repository for the Sales entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalesRepository extends JpaSpecificationExecutor<Sales>, JpaRepository<Sales, SaleId>, CustomSalesRepository {
    @Query("select sale from Sales sale left join fetch sale.salesLines where sale.id =:id AND sale.saleDate =:saleDate")
    Optional<Sales> findOneWithEagerSalesLines(@Param("id") Long id, @Param("saleDate") LocalDate saleDate);

    @Query("select sale from Sales sale where sale.numberTransaction = :ref and sale.statut = 'CLOSED' and sale.canceled = false order by sale.saleDate desc")
    List<Sales> findByNumberTransaction(@Param("ref") String ref);

    List<Sales> findSalesByIdIn(Set<Long> ids);

    @Query(
        "SELECT o.numberTransaction AS reference,c.firstName AS customerFirstName,c.lastName AS customerLastName   FROM Sales o LEFT JOIN o.customer c  WHERE o.id =:id AND o.saleDate =:saleDate"
    )
    SaleInfo findSaleInfoById(@Param("id") Long id, @Param("saleDate") LocalDate saleDate);

    @Query(
        "SELECT c.lastName AS lastName,c.firstName AS firsName,c.id AS id   FROM Sales o  JOIN o.customer c  WHERE o.differe AND o.statut='CLOSED' AND o.canceled =FALSE  GROUP BY c.id ORDER BY c.firstName,c.lastName"
    )
    Page<ClientDiffere> getClientDiffere(Pageable pageable);

    @Query(
        "SELECT SUM(o.restToPay)   FROM Sales o  JOIN o.customer c  WHERE o.differe AND o.statut='CLOSED' AND o.canceled =FALSE  AND c.id =:customerId"
    )
    BigDecimal getDiffereSoldeByCustomerId(Integer customerId);

    @Query(value = "SELECT sales_summary_json(:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:canceled)", nativeQuery = true)
    String fetchSalesSummary(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("canceled") boolean canceled
    );

    @Query(
        value = "SELECT rapport_activite_vente_report(:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:excludeFreeQty,:toIgnore)",
        nativeQuery = true
    )
    String getChiffreAffaire(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("excludeFreeQty") boolean excludeFreeQty,
        @Param("toIgnore") boolean toIgnore
    );

    @Query(value = "SELECT sales_summary_by_type_json(:startDate, :endDate, :statuts,:caterorieChiffreAffaire)", nativeQuery = true)
    String fetchSalesSummaryByTypeVente(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire
    );

    @Query(
        value = "SELECT sales_balance(:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:excludeFreeQty,:toIgnore)",
        nativeQuery = true
    )
    String fetchSalesBalance(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("excludeFreeQty") boolean excludeFreeQty,
        @Param("toIgnore") boolean toIgnore
    );

    @Query(
        value = "SELECT sales_tva_report(:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:excludeFreeQty,:toIgnore)",
        nativeQuery = true
    )
    String fetchSalesTvaReport(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("excludeFreeQty") boolean excludeFreeQty,
        @Param("toIgnore") boolean toIgnore
    );

    @Query(
        value = "SELECT sales_tva_report_journalier(:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:excludeFreeQty,:toIgnore)",
        nativeQuery = true
    )
    String fetchSalesTvaReportJournalier(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("excludeFreeQty") boolean excludeFreeQty,
        @Param("toIgnore") boolean toIgnore
    );

    @Query(
        value = "SELECT tableau_pharmacien_report(:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:excludeFreeQty,:toIgnore)",
        nativeQuery = true
    )
    String fetchTableauPharmacienReport(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("excludeFreeQty") boolean excludeFreeQty,
        @Param("toIgnore") boolean toIgnore
    );

    @Query(
        value = "SELECT tableau_pharmacien_month_report(:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:excludeFreeQty,:toIgnore)",
        nativeQuery = true
    )
    String fetchTableauPharmacienReportMensuel(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("excludeFreeQty") boolean excludeFreeQty,
        @Param("toIgnore") boolean toIgnore
    );

    @Query(value = "SELECT fetch_product_quantity_sold_json(:startDate, :endDate)", nativeQuery = true)
    String fetchProductQuantitySold(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    default Specification<Sales> filterByCustomerId(Integer customerId) {
        if (customerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(Sales_.customer).get(Customer_.id), customerId);
    }

    default Specification<Sales> filterByPaymentStatus(Set<PaymentStatus> paymentStatuses) {
        if (CollectionUtils.isEmpty(paymentStatuses)) {
            return null;
        }
        return (root, _, cb) -> root.get(Sales_.paymentStatus).in(paymentStatuses);
    }

    default Specification<Sales> filterNumberTransaction(String numberTransaction) {
        if (!StringUtils.hasLength(numberTransaction)) {
            return null;
        }
        return (root, query, cb) -> cb.like(root.get(Sales_.numberTransaction), numberTransaction + "%");
    }

    default Specification<Sales> filterByCaissierId(Set<Integer> caissierIds) {
        if (caissierIds == null || caissierIds.isEmpty()) {
            return null; // No filter applied
        }
        return (root, query, cb) -> root.get(Sales_.caissier).get(AppUser_.id).in(caissierIds);
    }

    default Specification<Sales> filterByPeriode(LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, _, cb) -> cb.between(root.get(Sales_.updatedAt), fromDate, toDate);
    }

    default Specification<Sales> between(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> cb.between(root.get(Sales_.saleDate), fromDate, toDate);
    }

    default Specification<Sales> toDay() {
        return (root, query, cb) -> cb.equal(root.get(Sales_.saleDate), LocalDate.now());
    }

    default Specification<Sales> isActif() {
        return (root, query, cb) -> cb.equal(root.get(Sales_.statut), SalesStatut.ACTIVE);
    }

    default Specification<Sales> hasStatut(EnumSet<SalesStatut> statut) {
        return (root, query, cb) -> root.get(Sales_.statut).in(statut);
    }

    default Specification<Sales> notImported() {
        return (root, query, cb) -> cb.equal(root.get(Sales_.imported), false);
    }

    default Specification<Sales> hasType(TypeVente typeVente) {
        return (root, query, cb) -> cb.equal(root.get(Sales_.type), typeVente.name());
    }

    default Specification<Sales> notDepot() {
        return (root, query, cb) ->
            cb.notEqual(root.type(), VenteDepot.class);
    }

    default Specification<Sales> hasCaissier(AppUser caissier) {
        return (root, query, cb) -> cb.equal(root.get(Sales_.caissier), caissier);
    }

    default Specification<Sales> hasVendeur(AppUser vendeur) {
        return (root, query, cb) -> cb.equal(root.get(Sales_.seller), vendeur);
    }

    default Specification<Sales> isDiffere() {
        return (root, query, cb) -> cb.isNull(root.get(Sales_.differe));
    }

    default Specification<Sales> hasCategorieCa(EnumSet<CategorieChiffreAffaire> categorieChiffreAffaires) {
        return (root, query, cb) -> root.get(Sales_.categorieChiffreAffaire).in(categorieChiffreAffaires);
    }

    // ── Requêtes dashboard préparateur ───────────────────────────────────────

    /**
     * Différés à relancer : ventes avec differe=true, rest_to_pay>0, sale_date <= aujourd'hui.
     * Retourne : [id, client_nom, client_telephone, rest_to_pay, sale_date, jours_retard]
     */
    @Query(
        value = """
            SELECT
                s.id,
                COALESCE(c.first_name || ' ' || c.last_name, 'Anonyme') AS client_nom,
                c.phone                                                  AS client_telephone,
                s.rest_to_pay                                            AS montant_du,
                s.sale_date                                              AS date_echeance,
                CAST(CURRENT_DATE - s.sale_date AS INTEGER)              AS jours_retard
            FROM sales s
            LEFT JOIN customer c ON s.customer_id = c.id
            WHERE s.differe    = true
              AND s.statut     = 'CLOSED'
              AND s.rest_to_pay > 0
              AND s.sale_date  <= CURRENT_DATE
              AND s.to_ignore  = false
              AND s.canceled   = false
            ORDER BY s.sale_date ASC
            LIMIT 30
            """,
        nativeQuery = true
    )
    List<Object[]> findDifferesARelancer();

    /**
     * Dernières ventes du caissier connecté pour aujourd'hui (max 20).
     * Le service limite ensuite au nombre souhaité.
     * Retourne : [id, number_transaction, sales_amount, created_at, type_vente, client_nom]
     */
    @Query(
        value = """
            SELECT
                s.id,
                s.number_transaction,
                s.sales_amount,
                s.created_at,
                CASE WHEN s.differe = true THEN 'DIFFERE' ELSE s.nature_vente END AS type_vente,
                COALESCE(c.first_name || ' ' || c.last_name, NULL)                AS client_nom
            FROM sales s
            LEFT JOIN customer c ON s.customer_id = c.id
            WHERE s.caissier_id = (SELECT id FROM app_user WHERE login = :login LIMIT 1)
              AND s.sale_date  = CURRENT_DATE
              AND s.statut     = 'CLOSED'
              AND s.to_ignore  = false
              AND s.canceled   = false
            ORDER BY s.created_at DESC
            LIMIT 20
            """,
        nativeQuery = true
    )
    List<Object[]> findVentesRecentesByCaissier(@Param("login") String login);

    /**
     * Agrégats des ventes pour la session (carnet, différé, nombre de transactions).
     * Retourne une ligne : [carnet_total, differe_total, nombre_transactions]
     */
    @Query(
        value = """
                        SELECT
                            COALESCE(SUM(CASE WHEN nature_vente = 'CARNET' THEN sales_amount ELSE 0 END), 0) AS carnet,
                            COALESCE(SUM(CASE WHEN differe = true  THEN rest_to_pay  ELSE 0 END), 0)         AS differe,
                            COUNT(*)                                                                           AS nombre_transactions
                        FROM sales
                        WHERE cash_register_id = :crId
                          AND sale_date   = CURRENT_DATE
                          AND statut      = 'CLOSED'
                      AND canceled    = false
            """,
        nativeQuery = true
    )
    List<Object[]> findSalesEncaissementsForCaisse(@Param("crId") Integer cashRegisterId);

}
