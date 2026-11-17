package com.kobe.warehouse.domain.enumeration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum StorageType {
    PRINCIPAL("Stock rayon", true, true, true),
    SAFETY_STOCK("Réserve", false, true, true),
    TOXIQUE("Toxiques", true, false, true),
    QUARANTAINE("Quarantaine", false, false, false);

    private final String value;
    private final boolean vendable;
    private final boolean reassortSuggere;
    private final boolean couvreLaDemande;

    StorageType(String value, boolean vendable, boolean reassortSuggere, boolean couvreLaDemande) {
        this.value = value;
        this.vendable = vendable;
        this.reassortSuggere = reassortSuggere;
        this.couvreLaDemande = couvreLaDemande;
    }

    public static StorageType valueOfLabel(String value) {
        for (StorageType e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    /** Produits dispensables / vendables depuis ce storage. */
    public boolean isVendable() {
        return vendable;
    }

    /** Ce storage participe aux suggestions de réassort automatique. */
    public boolean isReassortSuggere() {
        return reassortSuggere;
    }

    /**
     * Le stock de cet emplacement couvre la demande des clients, et entre donc au crédit du calcul
     * de réapprovisionnement fournisseur (SEMOIS).
     *
     * <p>Distinct de {@link #isVendable()}, qui dit si l'on dispense depuis l'emplacement :
     * la réserve n'est pas vendable mais alimente le rayon, donc elle couvre la demande.
     * Distinct aussi de {@link #isReassortSuggere()}, qui ne concerne que le réassort interne
     * rayon ↔ réserve. Seule la quarantaine ne couvre rien : son stock attend un retour, une
     * destruction ou un contrôle, et le compter retarderait la commande.
     *
     * <p>La même règle est appliquée en SQL par la vue {@code v_semois_suggestion} : toute
     * évolution ici doit s'accompagner d'une nouvelle version de cette vue.
     */
    public boolean couvreLaDemande() {
        return couvreLaDemande;
    }

    /** Les types d'emplacement dont le stock couvre la demande, pour un paramètre de requête. */
    public static Set<StorageType> listerTypesCouvrantLaDemande() {
        return Arrays.stream(values()).filter(StorageType::couvreLaDemande).collect(Collectors.toUnmodifiableSet());
    }
}
