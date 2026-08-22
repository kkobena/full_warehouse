package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.declaration_ca.dto.AnomalieDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contrôle les invariants du chiffre d'affaires à déclarer.
 *
 * <p>Le montant déclaré vit dans trois endroits — les lignes de vente, la vente, les règlements — et
 * les rapports agrègent le premier. Rien dans le schéma n'empêche ces trois valeurs de diverger : ce
 * service vérifie qu'elles ne l'ont pas fait.
 *
 * <p>Chaque contrôle porte le code de l'invariant du plan, pour qu'un écart constaté à l'écran
 * renvoie sans ambiguïté à la règle qu'il enfreint.
 */
@Service
public class AuditDeclarationCaService {

    /**
     * Les exemples sont lisibles tels quels : dates en JJ/MM/AAAA et montants séparés par milliers.
     *
     * <p>Le formatage est fait en SQL et non à l'écran parce que ces exemples sont des phrases
     * assemblées ici — l'écran ne reçoit qu'une chaîne, sans savoir où commence un montant.
     *
     * <p>La virgule est un littéral du masque {@code to_char}, puis remplacée par une espace :
     * le jeton {@code G} aurait suivi le {@code lc_numeric} du serveur, donc varié d'une
     * installation à l'autre.
     */
    /** Assez d'exemples pour retrouver les ventes, pas assez pour noyer l'écran. */
    private static final int MAX_EXEMPLES = 10;

    private final JdbcClient jdbcClient;
    private final StorageService storageService;

    public AuditDeclarationCaService(JdbcClient jdbcClient, StorageService storageService) {
        this.jdbcClient = jdbcClient;
        this.storageService = storageService;
    }

    /**
     * Le magasin de l'utilisateur connecté.
     *
     * <p>Un contrôle de cohérence qui remonterait les ventes d'une autre officine ferait apparaître
     * des anomalies sur des données que le lecteur ne peut ni expliquer ni corriger.
     */
    private Integer magasinId() {
        return storageService.getDefaultConnectedUserMainStorage().getMagasin().getId();
    }

    /**
     * Passe tous les contrôles sur une période.
     *
     * @param dateDebut début de période, bornes incluses ; {@code null} pour ne pas borner
     */
    @Transactional(readOnly = true)
    public List<AnomalieDTO>  controler(LocalDate dateDebut, LocalDate dateFin) {
        List<AnomalieDTO> resultats = new ArrayList<>();
        resultats.add(montantDeclarableHorsBornes(dateDebut, dateFin));
        resultats.add(sommeDesLignesDifferenteDeLaVente(dateDebut, dateFin));
        resultats.add(reglementsSuperieursAuChiffreDeclare(dateDebut, dateFin));
        resultats.add(ponctionSurLigneTaxee(dateDebut, dateFin));
        resultats.add(ponctionAuDelaDuPlafond());
        resultats.add(detailIncoherentAvecLaPonction());
        resultats.add(tvaNonRapprochee(dateDebut, dateFin));
        return resultats;
    }

    /** V1 — un montant déclarable négatif, ou supérieur au montant réel, n'a aucun sens. */
    private AnomalieDTO montantDeclarableHorsBornes(LocalDate dateDebut, LocalDate dateFin) {
        return controle(
            "V1",
            "Le montant déclarable d'une ligne reste entre 0 et son montant réel",
            "Un montant hors de ces bornes fait apparaître un chiffre d'affaires déclaré supérieur à l'encaissé, ou négatif.",
            """
            select sl.sales_id || ' du ' || to_char(sl.sale_date, 'DD/MM/YYYY') || ' : déclaré ' || replace(to_char(sl.amount_to_be_taken_into_account, 'FM999,999,999,999'), ',', ' ')
                   || ' pour un réel de ' || replace(to_char(sl.quantity_requested * sl.regular_unit_price, 'FM999,999,999,999'), ',', ' ')
              from sales_line sl
              join sales s on s.id = sl.sales_id and s.sale_date = sl.sales_sale_date
             where sl.to_ignore = false
               and s.magasin_id = :magasinId
               %s
               and (sl.amount_to_be_taken_into_account < 0
                    or sl.amount_to_be_taken_into_account > sl.quantity_requested * sl.regular_unit_price)
            """.formatted(bornes("sl.sale_date", dateDebut, dateFin)),
            dateDebut,
            dateFin
        );
    }

    /**
     * V2 — les rapports agrègent les lignes, les écrans de vente lisent la vente. Si les deux
     * divergent, les états cessent de se recouper sans que rien ne le signale.
     *
     * <p>Les ventes dépôt sont écartées : elles portent {@code ca = CA_DEPOT} et sont hors du CA
     * déclaré, leur montant de vente valant zéro par construction.
     */
    private AnomalieDTO sommeDesLignesDifferenteDeLaVente(LocalDate dateDebut, LocalDate dateFin) {
        return controle(
            "V2",
            "La somme des lignes égale le montant déclarable de la vente",
            "Les rapports agrègent les lignes alors que les écrans de vente lisent la vente : les deux chiffres cesseraient de se recouper.",
            """
            select s.id || ' du ' || to_char(s.sale_date, 'DD/MM/YYYY') || ' : vente ' || replace(to_char(s.amount_to_be_taken_into_account, 'FM999,999,999,999'), ',', ' ')
                   || ' contre ' || replace(to_char(coalesce(sum(sl.amount_to_be_taken_into_account), 0), 'FM999,999,999,999'), ',', ' ') || ' en lignes'
              from sales s
              join sales_line sl on sl.sales_id = s.id and sl.sales_sale_date = s.sale_date
             where s.dtype <> 'VenteDepot'
               and s.magasin_id = :magasinId
               and s.statut = 'CLOSED'
               %s
             group by s.id, s.sale_date, s.amount_to_be_taken_into_account
            having s.amount_to_be_taken_into_account <> coalesce(sum(sl.amount_to_be_taken_into_account), 0)
            """.formatted(bornes("s.sale_date", dateDebut, dateFin)),
            dateDebut,
            dateFin
        );
    }

    /**
     * Cohérence entre le chiffre déclaré et l'encaissement déclaré : c'est l'écart le plus visible
     * pour un lecteur, et le plus difficile à justifier.
     */
    private AnomalieDTO reglementsSuperieursAuChiffreDeclare(LocalDate dateDebut, LocalDate dateFin) {
        return controle(
            "V2b",
            "L'encaissement déclaré ne dépasse pas le chiffre d'affaires déclaré",
            "Un encaissement supérieur au chiffre déclaré rend l'état indéfendable : l'écart ne se rattache à rien.",
            """
            select s.id || ' du ' || to_char(s.sale_date, 'DD/MM/YYYY') || ' : encaissé déclaré '
                   || replace(to_char(sum(coalesce(p.amount_to_be_taken_into_account, p.paid_amount)), 'FM999,999,999,999'), ',', ' ')
                   || ' pour un CA déclaré de ' || replace(to_char(s.amount_to_be_taken_into_account, 'FM999,999,999,999'), ',', ' ')
              from sales s
              join payment_transaction p
                on p.sale_id = s.id and p.sale_date = s.sale_date and p.dtype = 'SalePayment'
             where s.dtype = 'CashSale'
               and s.magasin_id = :magasinId
               and s.statut = 'CLOSED'
               and s.canceled = false
               and s.differe = false
               %s
             group by s.id, s.sale_date, s.amount_to_be_taken_into_account
            having sum(coalesce(p.amount_to_be_taken_into_account, p.paid_amount)) > s.amount_to_be_taken_into_account
            """.formatted(bornes("s.sale_date", dateDebut, dateFin)),
            dateDebut,
            dateFin
        );
    }

    /** V8 — la ponction ne doit jamais toucher une ligne taxée : elle réduirait la TVA collectée. */
    private AnomalieDTO ponctionSurLigneTaxee(LocalDate dateDebut, LocalDate dateFin) {
        return controle(
            "V8",
            "Aucune ponction sur une ligne soumise à TVA",
            "Ponctionner une ligne taxée réduit la TVA collectée déclarée, alors qu'elle a été facturée au client.",
            """
            select sl.sales_id || ' du ' || to_char(sl.sale_date, 'DD/MM/YYYY') || ' : taux ' || sl.tax_value
              from sales_line sl
              join sales s on s.id = sl.sales_id and s.sale_date = sl.sales_sale_date
             where sl.exclusion_motif = 'PONCTION'
               and sl.tax_value <> 0
               and s.magasin_id = :magasinId
               %s
            """.formatted(bornes("sl.sale_date", dateDebut, dateFin)),
            dateDebut,
            dateFin
        );
    }

    /** V5 — le plafond par vente est la promesse faite au pharmacien ; il doit tenir a posteriori. */
    private AnomalieDTO ponctionAuDelaDuPlafond() {
        return controle(
            "V5",
            "Aucune vente n'a cédé plus que le plafond annoncé",
            "Le plafond par vente est ce qui rend la ponction discrète : le dépasser la rend visible.",
            """
            select d.sale_id || ' du ' || to_char(d.sale_date, 'DD/MM/YYYY') || ' : ' || replace(to_char(d.montant_ponctionne, 'FM999,999,999,999'), ',', ' ')
                   || ' pris sur ' || replace(to_char(d.montant_vente, 'FM999,999,999,999'), ',', ' ') || ' (plafond ' || p.plafond_par_vente || ' %)'
              from ca_ponction_detail d
              join ca_ponction p on p.id = d.ponction_id
             where p.statut = 'VALIDEE'
               and p.magasin_id = :magasinId
               and d.montant_ponctionne > floor(d.montant_vente * p.plafond_par_vente / 100)
            """,
            null,
            null
        );
    }

    /** le détail justifie le total : s'ils divergent, la ponction n'est plus explicable. */
    private AnomalieDTO detailIncoherentAvecLaPonction() {
        return controle(
            "V4",
            "Le détail d'une ponction totalise bien le montant ponctionné",
            "Sans cette égalité, le justificatif ne reconstitue plus le montant retiré et l'annulation devient inexacte.",
            """
            select p.id || ' (' || p.date_debut || ' au ' || p.date_fin || ') : total ' || p.montant_ponctionne
                   || ' contre ' || coalesce(sum(d.montant_ponctionne), 0) || ' en détail'
              from ca_ponction p
              left join ca_ponction_detail d on d.ponction_id = p.id
             where p.statut = 'VALIDEE'
               and p.magasin_id = :magasinId
             group by p.id, p.date_debut, p.date_fin, p.montant_ponctionne
            having p.montant_ponctionne <> coalesce(sum(d.montant_ponctionne), 0)
            """,
            null,
            null
        );
    }

    /**
     * V9 — l'écart de TVA entre le réel et le déclaré s'explique intégralement par les lignes
     * exclues du même taux.
     *
     * <p>C'est le contrôle qu'on montre à un tiers : un rapport TVA déclaré inférieur au réel est
     * normal, mais chaque franc d'écart doit se rattacher à une ligne portant un motif. Un écart
     * orphelin est indéfendable — rien ne dit d'où il vient.
     *
     * <p>Le rapprochement porte sur le TTC et non sur la taxe : à taux fixe la taxe en est
     * strictement proportionnelle, et raisonner sur le TTC évite d'imputer à un écart réel ce qui
     * ne serait qu'un arrondi de division.
     */
    private AnomalieDTO tvaNonRapprochee(LocalDate dateDebut, LocalDate dateFin) {
        return controle(
            "V9",
            "L'écart de TVA entre réel et déclaré s'explique par les seules lignes exclues",
            "Un écart qui ne se rattache à aucune ligne exclue rend le rapport TVA déclaré impossible à justifier.",
            """
            select 'Taux ' || r.tax_value || ' %% : écart TTC de ' || (r.reel - r.declare)
                   || ', dont ' || r.explique || ' porté par des lignes exclues et '
                   || (r.reel - r.declare - r.explique) || ' inexpliqué (soit '
                   || (r.reel - r.declare - r.explique
                       - round((r.reel - r.declare - r.explique) / tva_divisor(r.tax_value)))
                   || ' de TVA)'
              from (select sl.tax_value,
                           sum(sl.quantity_requested * sl.regular_unit_price)      as reel,
                           sum(coalesce(sl.amount_to_be_taken_into_account, 0))    as declare,
                           sum(case when sl.exclusion_motif is not null
                                    then sl.quantity_requested * sl.regular_unit_price
                                         - coalesce(sl.amount_to_be_taken_into_account, 0)
                                    else 0 end)                                    as explique
                      from sales_line sl
                      join sales s on s.id = sl.sales_id and s.sale_date = sl.sales_sale_date
                     where s.dtype <> 'VenteDepot'
                       and s.magasin_id = :magasinId
                       and s.statut = 'CLOSED'
                       and s.canceled = false
                       and sl.to_ignore = false
                       %s
                     group by sl.tax_value) r
             where r.reel - r.declare <> r.explique
                or r.declare > r.reel
            """.formatted(bornes("sl.sale_date", dateDebut, dateFin)),
            dateDebut,
            dateFin
        );
    }

    // ===== Exécution =====

    private AnomalieDTO controle(
        String code,
        String libelle,
        String consequence,
        String sql,
        LocalDate dateDebut,
        LocalDate dateFin
    ) {
        var requete = jdbcClient
            .sql("select * from (" + sql + ") anomalies limit " + (MAX_EXEMPLES + 1))
            .param("magasinId", magasinId());
        if (sql.contains(":dateDebut")) {
            requete = requete.param("dateDebut", dateDebut).param("dateFin", dateFin);
        }
        List<String> lignes = requete.query(String.class).list();

        // Une ligne de plus que la limite signale qu'il y en a d'autres, sans compter l'ensemble :
        // sur une table de plusieurs millions de lignes, le décompte exact coûterait plus que le
        // contrôle lui-même, pour une information que personne n'utilise.
        boolean tronque = lignes.size() > MAX_EXEMPLES;
        List<String> exemples = tronque ? lignes.subList(0, MAX_EXEMPLES) : lignes;
        long nombre = tronque ? MAX_EXEMPLES + 1 : lignes.size();

        return new AnomalieDTO(code, libelle, consequence, nombre, List.copyOf(exemples));
    }

    private String bornes(String colonne, LocalDate dateDebut, LocalDate dateFin) {
        return dateDebut == null || dateFin == null ? "" : "and " + colonne + " between :dateDebut and :dateFin";
    }
}
