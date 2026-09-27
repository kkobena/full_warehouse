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
import com.kobe.warehouse.service.fne.model.DetailProduitFacture;
import com.kobe.warehouse.service.fne.model.InfoTiersPayant;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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

    /**
     * Délai d'attente maximal des verrous de ligne, pour la seule transaction courante
     * ({@code set_config(..., true)} équivaut à {@code SET LOCAL}). Au-delà, PostgreSQL abandonne
     * l'attente plutôt que de bloquer le poste derrière une transaction qui traîne.
     */
    String DELAI_VERROU = "10s";

    @Query(value = "SELECT set_config('lock_timeout', :delai, true)", nativeQuery = true)
    String definirDelaiVerrou(@Param("delai") String delai);

    @Query(
        value = "SELECT f.id FROM facture_tiers_payant f WHERE f.id = :id AND f.invoice_date = :invoiceDate FOR UPDATE",
        nativeQuery = true
    )
    List<Long> verrouillerLigne(@Param("id") Long id, @Param("invoiceDate") LocalDate invoiceDate);

    /** L'ordre de verrouillage est fixé par l'{@code ORDER BY} : c'est lui qui écarte les interblocages. */
    @Query(
        value = """
        SELECT f.id FROM facture_tiers_payant f
         WHERE f.groupe_facture_tiers_payant_id = :id AND f.groupe_facture_tiers_payant_invoice_date = :invoiceDate
         ORDER BY f.invoice_date, f.id
           FOR UPDATE
        """,
        nativeQuery = true
    )
    List<Long> verrouillerLignesFilles(@Param("id") Long id, @Param("invoiceDate") LocalDate invoiceDate);

    /**
     * Factures demandées et leurs filles, verrouillées en une seule instruction, dans le même ordre
     * ({@code invoice_date}, {@code id}) que les règlements. Un groupe reçoit son identifiant avant
     * ses filles : il passe donc devant elles, comme dans {@link #verrouillerFilles}.
     */
    @Query(
        value = """
        SELECT f.id FROM facture_tiers_payant f
         WHERE f.id IN (:ids) OR f.groupe_facture_tiers_payant_id IN (:ids)
         ORDER BY f.invoice_date, f.id
           FOR UPDATE
        """,
        nativeQuery = true
    )
    List<Long> verrouillerLignesEtFilles(@Param("ids") Collection<Long> ids);

    /** Relu en SQL, hors contexte de persistance : la valeur est celle validée en base, pas une copie en mémoire. */
    @Query(value = "SELECT f.num_facture FROM facture_tiers_payant f WHERE f.id IN (:ids) AND f.montant_regle > 0", nativeQuery = true)
    List<String> numerosFacturesReglees(@Param("ids") Collection<Long> ids);

    /**
     * Verrou consultatif de transaction : une seule édition de factures à la fois. Deux éditions
     * simultanées liraient les mêmes dossiers non facturés — chacun finirait sur deux factures — et
     * le même dernier numéro de facture. Libéré automatiquement à la fin de la transaction.
     *
     * <p>Un verrou consultatif vaut pour toute la base, pas pour un schéma : la clé comprend donc le
     * schéma courant — celui contre lequel se résolvent les tables de cette requête et des autres —
     * sans quoi deux officines hébergées dans la même base, chacune dans son schéma, bloqueraient
     * mutuellement leurs éditions. Seconde moitié de la clé : l'opération verrouillée.
     */
    @Query(
        value = "SELECT count(*) FROM (SELECT pg_advisory_xact_lock(hashtext(current_schema()), hashtext('facturation.edition'))) verrou",
        nativeQuery = true
    )
    Long verrouillerEditionSql();

    default void verrouillerEdition() {
        definirDelaiVerrou(DELAI_VERROU);
        verrouillerEditionSql();
    }

    default void verrouillerAvecFilles(Collection<FactureItemId> ids) {
        definirDelaiVerrou(DELAI_VERROU);
        verrouillerLignesEtFilles(ids.stream().map(FactureItemId::getId).toList());
    }

    /**
     * Verrou pessimiste sur une facture, puis relecture : sérialise les règlements et les
     * annulations d'une même facture. La relecture suit le verrou, elle voit donc l'état laissé
     * par la transaction qu'on a attendue. À appeler avant tout autre chargement de la facture dans
     * la transaction — une instance déjà gérée ne serait pas relue.
     */
    default Optional<FactureTiersPayant> verrouiller(FactureItemId id) {
        definirDelaiVerrou(DELAI_VERROU);
        verrouillerLigne(id.getId(), id.getInvoiceDate());
        return findById(id);
    }

    /**
     * Verrouille les factures filles d'une facture de groupe, dans un ordre stable. Le groupe doit
     * être verrouillé d'abord ({@link #verrouiller}) : groupe puis filles, toujours dans ce sens.
     */
    default void verrouillerFilles(FactureItemId groupeId) {
        verrouillerLignesFilles(groupeId.getId(), groupeId.getInvoiceDate());
    }

    /**
     * Factures définitives (non provisoires) dont la réponse FNE n'est pas encore enregistrée
     * et dont le tiers payant dispose d'un téléphone et d'un email valides.
     * Exclut les factures de type DEPOT (non éligibles à la certification FNE).
     */
    @Query("""
        SELECT f FROM FactureTiersPayant f
        JOIN f.tiersPayant tp
        WHERE f.fneResponse IS NULL
          AND f.factureProvisoire = FALSE
          AND tp.telephone IS NOT NULL AND tp.telephone <> ''
          AND tp.email IS NOT NULL AND tp.email <> ''
          AND tp.categorie <> 'DEPOT'
        ORDER BY f.created ASC
        """)
    List<FactureTiersPayant> findPendingFneCertification();


    @Query(
        """
            SELECT f FROM FactureTiersPayant f
            LEFT JOIN FETCH f.tiersPayant
            LEFT JOIN FETCH f.groupeTiersPayant
            LEFT JOIN FETCH f.facturesDetails d
            LEFT JOIN FETCH d.sale s
            LEFT JOIN FETCH s.ayantDroit
            LEFT JOIN FETCH s.customer
            LEFT JOIN FETCH s.remise
            LEFT JOIN FETCH d.clientTiersPayant c
            LEFT JOIN FETCH c.assuredCustomer
            LEFT JOIN FETCH c.tiersPayant
            WHERE f.id = :id AND f.invoiceDate = :invoiceDate
            """
    )
    Optional<FactureTiersPayant> findOneWithDetails(@Param("id") Long id, @Param("invoiceDate") LocalDate invoiceDate);

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

    @Query(
        "SELECT tp.id AS id, tp.name AS name, tp.fullName AS fullName, tp.telephone AS telephone,tp.email AS email, tp.adresse AS adresse, tp.ncc AS ncc, tp.categorie AS categorie FROM  FactureTiersPayant o JOIN  o.tiersPayant tp WHERE o.id=:id AND o.invoiceDate =:invoiceDate "
    )
    InfoTiersPayant getInfoTiersPayantByFactureId(@Param("id") Long id,
                                                  @Param("invoiceDate") LocalDate invoiceDate);

    @Query(
        value = """
            SELECT sl.quantity_requested       AS quantite,
                   sl.tax_value                AS codeTva,
                   four.code_cip               AS produitCode,
                   prod.code_ean_labo          AS produitCodeEan,
                   prod.libelle,
                   sl.regular_unit_price       AS prixUnitaire,
                   COALESCE(sl.taux_remise, 0) AS tauxRemise,
                   tl.taux                     AS tauxCouverture,
                      sl.calculation_base_price AS tarifReferenceAssurance
            from sales_line sl
                     JOIN third_party_sale_line tl ON tl.sale_id = sl.sales_id
                AND tl.sale_sale_date = sl.sales_sale_date
                     JOIN facture_tiers_payant fac ON fac.id = tl.facture_tiers_payant_id
                AND fac.invoice_date = tl.invoice_date
                     JOIN produit prod ON prod.id = sl.produit_id
                     JOIN fournisseur_produit four ON four.id = prod.fournisseur_produit_principal_id
            WHERE fac.invoice_date = :invoiceDate
              AND fac.id = :id
              AND fac.tiers_payant_id = :tiersPayantId

            GROUP BY sl.id, sl.quantity_requested,
                     sl.tax_value,
                     four.code_cip,
                     prod.code_ean_labo,
                     prod.libelle, sl.regular_unit_price,
                     sl.taux_remise,
                     tl.taux,sl.calculation_base_price
            """, nativeQuery = true
    )
    List<DetailProduitFacture> getDetailProduitFacture(@Param("id") Long id,
                                                       @Param("invoiceDate") LocalDate invoiceDate, @Param("tiersPayantId") Integer tiersPayantId);


    default Specification<FactureTiersPayant> fetchByIds(Set<FactureItemId> ids) {
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
            // null = les deux : le sélecteur de l'écran offre « Toutes » en plus de
            // « Définitives » et « Provisoires ».
            if (invoiceSearchParams.factureProvisoire() != null) {
                predicates.add(cb.equal(root.get(FactureTiersPayant_.factureProvisoire), invoiceSearchParams.factureProvisoire()));
            }
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
            // null = les deux : le sélecteur de l'écran offre « Toutes » en plus de
            // « Définitives » et « Provisoires ».
            if (invoiceSearchParams.factureProvisoire() != null) {
                predicates.add(cb.equal(root.get(FactureTiersPayant_.factureProvisoire), invoiceSearchParams.factureProvisoire()));
            }
            if (!CollectionUtils.isEmpty(invoiceSearchParams.statuts())) {
                predicates.add(root.get(FactureTiersPayant_.statut).in(invoiceSearchParams.statuts()));
            }
            predicates.add(root.get(FactureTiersPayant_.groupeTiersPayant).isNotNull());

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
