package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.dto.enumeration.StatGroupBy;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.utils.DateUtil;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.springframework.util.StringUtils;

public final class VenteStatQueryBuilder {

    public static final String PERIOQIQUE_CA_QUERY =
        "SELECT SUM(s.sales_amount) AS sales_amount,SUM(s.ht_amount)/COUNT(s.id) AS panierMoyen,SUM(s.amount_to_be_paid) AS amount_to_be_paid,SUM(s.discount_amount) AS discount_amount,SUM(s.cost_amount) AS cost_amount, SUM(s.ht_amount)-SUM(s.cost_amount) AS marge, SUM(s.amount_to_be_taken_into_account) AS amount_to_be_taken_into_account,SUM(s.net_amount) AS net_amount,SUM(s.ht_amount) AS ht_amount,SUM(s.part_assure) AS part_assure,SUM(s.part_tiers_payant) AS part_tiers_payant,SUM(s.tax_amount) AS tax_amount,SUM(s.rest_to_pay) AS rest_to_pay,SUM(s.ht_amount_ug) AS ht_amount_ug,SUM(s.discount_amount_hors_ug) AS discount_amount_hors_ug,SUM(s.discount_amount_ug) AS discount_amount_ug,SUM(s.net_ug_amount) AS net_ug_amount,SUM(s.marge_ug) AS marge_ug,SUM(s.montant_ttc_ug) AS montant_ttc_ug,SUM(s.payroll_amount) AS payroll_amount,SUM(s.montant_tva_ug) AS montant_tva_ug,SUM(s.montant_net_ug) AS montant_net_ug,SUM(payment.paid_amount) AS paid_amount,SUM(payment.real_net_amount) AS real_net_amount,COUNT(s.id) AS sale_count FROM sales s,(SELECT SUM(p.reel_amount) AS real_net_amount,SUM(p.paid_amount) AS paid_amount,p.sale_id AS sale_id FROM payment_transaction p GROUP BY p.sale_id) as payment WHERE payment.sale_id=s.id   AND s.imported=0 AND  DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut IN (?3)  %s %s %s";

    public static final String TYPE_VENTE = " AND s.dtype ='%s' ";
    public static final String PERIOQIQUE_CA_QUERY_GROUPING_BY_PERIODE =
        "SELECT %s AS mvtDate,SUM(s.ht_amount)/COUNT(s.id) AS panierMoyen, s.statut AS statut, SUM(s.sales_amount) AS sales_amount,SUM(s.amount_to_be_paid) AS amount_to_be_paid,SUM(s.discount_amount) AS discount_amount,SUM(s.cost_amount) AS cost_amount, SUM(s.ht_amount)-SUM(s.cost_amount) AS marge, SUM(s.amount_to_be_taken_into_account) AS amount_to_be_taken_into_account,SUM(s.net_amount) AS net_amount,SUM(s.ht_amount) AS ht_amount,SUM(s.part_assure) AS part_assure,SUM(s.part_tiers_payant) AS part_tiers_payant,SUM(s.tax_amount) AS tax_amount,SUM(s.rest_to_pay) AS rest_to_pay,SUM(s.ht_amount_ug) AS ht_amount_ug,SUM(s.discount_amount_hors_ug) AS discount_amount_hors_ug,SUM(s.discount_amount_ug) AS discount_amount_ug,SUM(s.net_ug_amount) AS net_ug_amount,SUM(s.marge_ug) AS marge_ug,SUM(s.montant_ttc_ug) AS montant_ttc_ug,SUM(s.payroll_amount) AS payroll_amount,SUM(s.montant_tva_ug) AS montant_tva_ug,SUM(s.montant_net_ug) AS montant_net_ug,SUM(payment.paid_amount) AS paid_amount,SUM(payment.real_net_amount) AS real_net_amount,COUNT(s.id) AS sale_count FROM sales s,(SELECT SUM(p.reel_amount) AS real_net_amount,SUM(p.paid_amount) AS paid_amount,p.sale_id AS sale_id FROM payment_transaction p GROUP BY p.sale_id) as payment WHERE payment.sale_id=s.id   AND s.imported=0 AND  DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut IN (?3) %s %s %s GROUP BY mvtDate,statut ORDER BY mvtDate ";
    public static final String PERIOQIQUE_CA_QUERY_BY_TYPE =
        "SELECT s.dtype as type_vente,SUM(s.ht_amount)/COUNT(s.id) AS panierMoyen, SUM(s.sales_amount) AS sales_amount,SUM(s.amount_to_be_paid) AS amount_to_be_paid,SUM(s.discount_amount) AS discount_amount,SUM(s.cost_amount) AS cost_amount, SUM(s.ht_amount)-SUM(s.cost_amount) AS marge, SUM(s.amount_to_be_taken_into_account) AS amount_to_be_taken_into_account,SUM(s.net_amount) AS net_amount,SUM(s.ht_amount) AS ht_amount,SUM(s.part_assure) AS part_assure,SUM(s.part_tiers_payant) AS part_tiers_payant,SUM(s.tax_amount) AS tax_amount,SUM(s.rest_to_pay) AS rest_to_pay,SUM(s.ht_amount_ug) AS ht_amount_ug,SUM(s.discount_amount_hors_ug) AS discount_amount_hors_ug,SUM(s.discount_amount_ug) AS discount_amount_ug,SUM(s.net_ug_amount) AS net_ug_amount,SUM(s.marge_ug) AS marge_ug,SUM(s.montant_ttc_ug) AS montant_ttc_ug,SUM(s.payroll_amount) AS payroll_amount,SUM(s.montant_tva_ug) AS montant_tva_ug,SUM(s.montant_net_ug) AS montant_net_ug,SUM(payment.paid_amount) AS paid_amount,SUM(payment.real_net_amount) AS real_net_amount,COUNT(s.id) AS sale_count FROM sales s,(SELECT SUM(p.reel_amount) AS real_net_amount,SUM(p.paid_amount) AS paid_amount,p.sale_id AS sale_id FROM payment_transaction p GROUP BY p.sale_id) as payment WHERE payment.sale_id=s.id AND s.imported=0 AND DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut IN (?3)  %s %s %s GROUP BY s.dtype";
    public static final String MODE_PAIMENT =
        """
        SELECT p.payment_mode_code,md.libelle, SUM(p.reel_amount) AS real_net_amount,SUM(p.paid_amount) AS paid_amount,p.sale_id AS sale_id
        FROM payment_transaction p,sales s,payment_mode md WHERE p.payment_mode_code=md.code AND p.sale_id=s.id AND s.imported=0 AND DATE(s.updated_at) BETWEEN ?1 AND ?2 AND s.statut IN (?3) %s %s %s GROUP BY p.payment_mode_code
        """;

    private VenteStatQueryBuilder() {}






    private static TypeVenteDTO fromTypeVente(String typeVente) {
        if (StringUtils.hasLength(typeVente)) {
            if (typeVente.equalsIgnoreCase(TypeVente.CashSale.name())) {
                return TypeVenteDTO.CashSale;
            } else if (typeVente.equalsIgnoreCase(TypeVente.ThirdPartySales.name())) {
                return TypeVenteDTO.ThirdPartySales;
            }
        }
        return null;
    }

    private static String buildMvtDate(Tuple tuple, StatGroupBy statGroupBy) {
        return switch (statGroupBy) {
            case DAY -> LocalDate.parse(tuple.get("mvtDate", String.class)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            case MONTH -> DateUtil.getMonthFromMonth(Month.of(tuple.get("mvtDate", Integer.class)));
            case YEAR -> Year.of(tuple.get("mvtDate", Integer.class)).toString();
            case HOUR -> tuple.get("mvtDate", Integer.class).toString();
        };
    }
}
