package com.kobe.warehouse.service.customer;

/**
 * Alerte levée par l'ajout d'un produit à la vente d'un client.
 *
 * @param niveau {@code BLOQUANTE} (allergie : la délivrance exige une dérogation tracée) ou
 *               {@code INFO} (grossesse, allaitement : simple rappel)
 * @param type   {@code ALLERGIE}, {@code GROSSESSE} ou {@code ALLAITEMENT}
 */
public record AlerteSanteDTO(String niveau, String type, String message) {
    public static final String BLOQUANTE = "BLOQUANTE";
    public static final String INFO = "INFO";

    public boolean bloquante() {
        return BLOQUANTE.equals(niveau);
    }
}
