package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.service.declaration_ca.dto.PonctionLigneDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traduit l'algorithme de ponction en requêtes ensemblistes. Aucune logique métier : les règles sont
 * décidées par {@link PonctionService}, ce composant les exécute.
 *
 * <p><strong>Pourquoi tout en SQL.</strong> Une période de deux mois représente couramment 15 000 à
 * 25 000 ventes. Charger, trier et boucler en Java serait intenable en temps comme en mémoire. Le
 * point de coupure — jusqu'où descendre dans les ventes triées — s'obtient en une passe avec une
 * somme cumulée glissante, sans jamais matérialiser la liste côté application.
 */
@Component
public class PonctionCalculator {

    /**
     * Assiette : ventes comptant closes, non annulées, non importées, non différées, réglées selon
     * les modes retenus, dont aucune ligne n'a déjà été retraitée, et dont la part à TVA 0 est
     * strictement positive.
     */
    private static final String ELIGIBLE =
        """
        eligible as (
          select s.id,
                 s.sale_date,
                 s.number_transaction,
                 sum(sl.amount_to_be_taken_into_account)                                 as montant_vente,
                 sum(sl.amount_to_be_taken_into_account) filter (where sl.tax_value = 0) as base_tva0
            from sales s
            join sales_line sl on sl.sales_id = s.id and sl.sales_sale_date = s.sale_date
           where s.sale_date between :dateDebut and :dateFin
             and s.dtype = 'CashSale'
             and s.statut = 'CLOSED'
             and s.canceled = false
             and s.imported = false
             and s.differe = false
             and s.to_ignore = false
             and s.ca = 'CA'
             and s.magasin_id = :magasinId
             and s.ponction_id is null
             and sl.to_ignore = false
           group by s.id, s.sale_date, s.number_transaction
          having coalesce(sum(sl.amount_to_be_taken_into_account) filter (where sl.tax_value = 0), 0) > 0
             and count(*) filter (where sl.exclusion_motif is not null) = 0
             and exists (select 1
                           from payment_transaction p
                          where p.sale_id = s.id
                            and p.sale_date = s.sale_date
                            and p.dtype = 'SalePayment'
                            and p.payment_mode_code in (:modes))
        )
        """;

    /**
     * Plafond par vente : le minimum entre la part autorisée du total et l'assiette exonérée
     * disponible. {@code floor} garantit qu'on ne dépasse jamais le pourcentage annoncé.
     *
     * <p>Le tri porte sur l'assiette exonérée et non sur le montant total : c'est elle qui détermine
     * la capacité réelle d'une vente, et trier sur le total ferait remonter en tête des ventes
     * majoritairement taxées, donc quasiment inéligibles.
     */
    private static final String PLAFONNE =
        """
        plafonne as (
          select e.id, e.sale_date, e.number_transaction, e.montant_vente, e.base_tva0,
                 least(floor(e.montant_vente * :plafond / 100)::bigint, e.base_tva0) as cap,
                 row_number() over (order by e.base_tva0 desc, e.sale_date, e.id)    as rang,
                 sum(least(floor(e.montant_vente * :plafond / 100)::bigint, e.base_tva0))
                     over (order by e.base_tva0 desc, e.sale_date, e.id
                           rows between unbounded preceding and current row)         as cumul
            from eligible e
        )
        """;

    private final JdbcClient jdbcClient;

    public PonctionCalculator(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Assiette de la période, avant tout objectif : trois montants et deux compteurs. */
    @Transactional(readOnly = true)
    public Assiette calculerAssiette(LocalDate dateDebut, LocalDate dateFin, int magasinId, BigDecimal plafond, String[] modes) {
        String sql =
            "with " +
            ELIGIBLE +
            ", " +
            PLAFONNE +
            """
            select coalesce(sum(p.base_tva0), 0),
                   coalesce(sum(p.cap), 0),
                   count(*)
              from plafonne p
            """;
        return jdbcClient
            .sql(sql)
            .params(parametres(dateDebut, dateFin, magasinId, modes, plafond))
            .query((rs, n) ->
                new Assiette(rs.getLong(1), rs.getLong(2), rs.getInt(3))
            )
            .single();
    }

    /**
     * Les ventes retenues pour atteindre l'objectif, dans l'ordre, avec ce qui est pris sur chacune.
     *
     * <p>{@code cumul - cap < objectif} sélectionne exactement les ventes situées avant le point de
     * coupure, et {@code least/greatest} donne à la dernière le solde nécessaire, sans dépasser son
     * plafond. Le tri est total — {@code (base_tva0, sale_date, id)} — donc deux exécutions donnent
     * le même résultat : la simulation vaut engagement.
     */
    @Transactional(readOnly = true)
    public List<PonctionLigneDTO> repartir(
        LocalDate dateDebut,
        LocalDate dateFin,
        int magasinId,
        BigDecimal plafond,
        String[] modes,
        long objectif
    ) {
        String sql =
            "with " +
            ELIGIBLE +
            ", " +
            PLAFONNE +
            """
            select p.id, p.sale_date, p.number_transaction, p.montant_vente, p.base_tva0, p.rang,
                   least(p.cap, greatest(0, :objectif - (p.cumul - p.cap))) as prise
              from plafonne p
             where p.cumul - p.cap < :objectif
             order by p.rang
            """;
        Map<String, Object> params = parametres(dateDebut, dateFin, magasinId, modes, plafond);
        params.put("objectif", objectif);
        return jdbcClient
            .sql(sql)
            .params(params)
            .query((rs, n) ->
                new PonctionLigneDTO(
                    rs.getLong("id"),
                    rs.getDate("sale_date").toLocalDate(),
                    rs.getString("number_transaction"),
                    rs.getLong("montant_vente"),
                    rs.getLong("base_tva0"),
                    rs.getLong("prise"),
                    rs.getInt("rang")
                )
            )
            .list();
    }

    /**
     * Écrit le détail de la ponction en <strong>une seule instruction</strong>.
     *
     * <p>{@code INSERT … SELECT} plutôt qu'une boucle d'insertions : une ponction peut retenir
     * plusieurs milliers de ventes, et autant d'allers-retours seraient inutilement coûteux.
     *
     * <p>Surtout, c'est ce qui permet ensuite de <strong>déduire l'en-tête du détail</strong> plutôt
     * que d'un calcul séparé. Les deux étaient auparavant issus de deux exécutions distinctes de la
     * même requête : une vente close entre les deux les faisait diverger, et l'invariant « le détail
     * totalise le montant ponctionné » tombait sans que rien ne le signale.
     *
     * @return le nombre de ventes retenues
     */
    @Transactional
    public int enregistrerDetail(
        Integer ponctionId,
        LocalDate dateDebut,
        LocalDate dateFin,
        int magasinId,
        BigDecimal plafond,
        String[] modes,
        long objectif
    ) {
        String sql =
            "with " +
            ELIGIBLE +
            ", " +
            PLAFONNE +
            """
            insert into ca_ponction_detail (ponction_id, sale_id, sale_date, montant_vente,
                                            montant_base, montant_ponctionne, rang, numero_transaction)
            select :ponctionId, p.id, p.sale_date, p.montant_vente, p.base_tva0,
                   least(p.cap, greatest(0, :objectif - (p.cumul - p.cap))), p.rang, p.number_transaction
              from plafonne p
             where p.cumul - p.cap < :objectif
            """;
        Map<String, Object> params = parametres(dateDebut, dateFin, magasinId, modes, plafond);
        params.put("objectif", objectif);
        params.put("ponctionId", ponctionId);
        return jdbcClient.sql(sql).params(params).update();
    }

    /**
     * Applique une ponction déjà détaillée dans {@code ca_ponction_detail}.
     *
     * <p>Trois écritures, dans cet ordre : les lignes exonérées au prorata, la vente, puis ses
     * règlements — espèces d'abord. Le détail sert de source unique : ce qui est écrit ici est
     * exactement ce que l'écran a montré à la simulation.
     */
    @Transactional
    public void appliquer(Integer ponctionId) {
        // Les lignes exonérées, au prorata de leur poids dans l'assiette TVA 0 de la vente.
        //
        // Le reliquat d'arrondi est réparti à raison d'UN franc par ligne, aux plus fortes
        // décimales d'abord. Le verser en bloc sur la plus grosse ligne — ce qui était fait — la
        // faisait passer sous zéro dès que la vente comptait beaucoup de petites lignes : avec
        // 25 lignes à 4 F et une prise de 35, chacune arrondissait à 1, laissant 10 F à imputer sur
        // une ligne qui n'en portait que 4. Un franc par ligne ne peut pas dépasser : la part
        // entière d'une ligne est au plus son montant moins un dès que la prise est inférieure à
        // l'assiette, et le reliquat est nul quand elle lui est égale.
        jdbcClient
            .sql(
                """
                with base as (
                  select l.id,
                         l.sale_date,
                         d.sale_id,
                         d.sale_date as vente_date,
                         l.amount_to_be_taken_into_account as montant_ligne,
                         floor(d.montant_ponctionne * l.amount_to_be_taken_into_account::numeric / d.montant_base) as part,
                         d.montant_ponctionne * l.amount_to_be_taken_into_account::numeric / d.montant_base
                           - floor(d.montant_ponctionne * l.amount_to_be_taken_into_account::numeric / d.montant_base) as fraction,
                         d.montant_ponctionne
                    from ca_ponction_detail d
                    join sales_line l on l.sales_id = d.sale_id and l.sales_sale_date = d.sale_date
                   where d.ponction_id = :ponctionId
                     and l.tax_value = 0
                     and l.to_ignore = false
                ),
                reparti as (
                  select b.id,
                         b.sale_date,
                         b.part::bigint as part,
                         (b.montant_ponctionne - sum(b.part) over (partition by b.sale_id, b.vente_date))::bigint as reliquat,
                         row_number() over (partition by b.sale_id, b.vente_date
                                            order by b.fraction desc, b.montant_ligne desc, b.id) as rang
                    from base b
                )
                update sales_line sl
                   set amount_to_be_taken_into_account =
                         sl.amount_to_be_taken_into_account
                         - (r.part + case when r.rang <= r.reliquat then 1 else 0 end),
                       exclusion_motif = 'PONCTION'
                  from reparti r
                 where sl.id = r.id and sl.sale_date = r.sale_date
                """
            )
            .param("ponctionId", ponctionId)
            .update();

        jdbcClient
            .sql(
                """
                update sales s
                   set amount_to_be_taken_into_account = s.amount_to_be_taken_into_account - d.montant_ponctionne,
                       ponction_id = d.ponction_id
                  from ca_ponction_detail d
                 where s.id = d.sale_id and s.sale_date = d.sale_date and d.ponction_id = :ponctionId
                """
            )
            .param("ponctionId", ponctionId)
            .update();

        // Les règlements : espèces d'abord, puis le reste. Même somme cumulée glissante que pour le
        // choix des ventes — la prise mord sur chaque règlement à hauteur de ce qu'il porte.
        jdbcClient
            .sql(
                """
                with ordonne as (
                  select p.id, p.transaction_date, p.paid_amount, d.montant_ponctionne,
                         sum(p.paid_amount) over (partition by d.sale_id, d.sale_date
                                                  order by case when p.payment_mode_code = 'CASH' then 0 else 1 end, p.id
                                                  rows between unbounded preceding and current row) as cumul
                    from ca_ponction_detail d
                    join payment_transaction p
                      on p.sale_id = d.sale_id and p.sale_date = d.sale_date and p.dtype = 'SalePayment'
                   where d.ponction_id = :ponctionId
                )
                update payment_transaction pt
                   set amount_to_be_taken_into_account =
                         o.paid_amount - least(o.paid_amount,
                                               greatest(0, o.montant_ponctionne - (o.cumul - o.paid_amount)))
                  from ordonne o
                 where pt.id = o.id and pt.transaction_date = o.transaction_date
                """
            )
            .param("ponctionId", ponctionId)
            .update();
    }

    /**
     * Défait une ponction en rétablissant les montants d'origine.
     *
     * <p>La restauration est exacte sans avoir à rejouer le calcul, et c'est une conséquence directe
     * de la règle d'éligibilité : une vente ponctionnée était <strong>intacte</strong>. Son montant
     * déclarable valait donc exactement son montant réel, et celui d'un règlement son montant
     * encaissé. Il n'y a rien à reconstituer, seulement à revenir à la valeur brute.
     */
    @Transactional
    public void annuler(Integer ponctionId) {
        jdbcClient
            .sql(
                """
                update sales_line sl
                   set amount_to_be_taken_into_account = sl.quantity_requested * sl.regular_unit_price,
                       exclusion_motif = null
                  from ca_ponction_detail d
                 where sl.sales_id = d.sale_id and sl.sales_sale_date = d.sale_date
                   and d.ponction_id = :ponctionId
                """
            )
            .param("ponctionId", ponctionId)
            .update();

        jdbcClient
            .sql(
                """
                update sales s
                   set amount_to_be_taken_into_account = s.amount_to_be_taken_into_account + d.montant_ponctionne,
                       ponction_id = null
                  from ca_ponction_detail d
                 where s.id = d.sale_id and s.sale_date = d.sale_date and d.ponction_id = :ponctionId
                """
            )
            .param("ponctionId", ponctionId)
            .update();

        jdbcClient
            .sql(
                """
                update payment_transaction pt
                   set amount_to_be_taken_into_account = null
                  from ca_ponction_detail d
                 where pt.sale_id = d.sale_id and pt.sale_date = d.sale_date
                   and pt.dtype = 'SalePayment' and d.ponction_id = :ponctionId
                """
            )
            .param("ponctionId", ponctionId)
            .update();
    }

    private Map<String, Object> parametres(
        LocalDate dateDebut,
        LocalDate dateFin,
        int magasinId,
        String[] modes,
        BigDecimal plafond
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("dateDebut", dateDebut);
        params.put("dateFin", dateFin);
        params.put("magasinId", magasinId);
        params.put("modes", List.of(modes));
        params.put("plafond", plafond);
        return params;
    }

    /**
     * @param assietteTva0 part exonérée des ventes éligibles — la seule ponctionnable
     * @param montantPonctionnable maximum atteignable une fois les plafonds appliqués
     * @param nombreVentes ventes éligibles
     */
    public record Assiette(long assietteTva0, long montantPonctionnable, int nombreVentes) {}
}
