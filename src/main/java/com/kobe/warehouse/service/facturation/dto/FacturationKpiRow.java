package com.kobe.warehouse.service.facturation.dto;

/**
 * Résultat brut de la requête KPI de facturation.
 * Produit par {@code FactureTiersPayantRepositoryCustom#getKpiData}.
 *
 * @param totalFacture   Somme totale facturée (montant_net).
 * @param totalRegle     Somme totale réglée (montant_regle).
 * @param countFactures  Nombre total de factures.
 * @param countImpayees  Nombre de factures non réglées (statut ≠ PAID).
 * @param countEnRetard  Nombre de factures en retard de paiement.
 */
public record FacturationKpiRow(
    long totalFacture,
    long totalRegle,
    long countFactures,
    long countImpayees,
    long countEnRetard
) {

    /** Instance vide (aucune donnée). */
    public static FacturationKpiRow empty() {
        return new FacturationKpiRow(0L, 0L, 0L, 0L, 0L);
    }

    /**
     * Construit un {@code FacturationKpiRow} à partir d'un tableau brut
     * retourné par une requête native JPA.
     * Ordre attendu : [totalFacture, totalRegle, countFactures, countImpayees, countEnRetard].
     */
    public static FacturationKpiRow from(Object[] row) {
        return new FacturationKpiRow(
            toLong(row[0]),
            toLong(row[1]),
            toLong(row[2]),
            toLong(row[3]),
            toLong(row[4])
        );
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private static long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number n) return n.longValue();
        String s = obj.toString().trim();
        if (s.isEmpty()) return 0L;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
            try {
                return (long) Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
    }
}

