package com.kobe.warehouse.service.pilotage.calcul;

/** Encaissé d'un mode, tranche par tranche, et son total cumulé au fil des ajouts. */
public final class ModeEncaissement {

    private final String libelle;
    private final long[] parTranche;
    private long total;

    public ModeEncaissement(String libelle, int tranches) {
        this.libelle = libelle;
        this.parTranche = new long[tranches];
    }

    public void ajouter(int rang, long montant) {
        parTranche[rang] += montant;
        total += montant;
    }

    public String libelle() {
        return libelle;
    }

    public long[] parTranche() {
        return parTranche;
    }

    public long total() {
        return total;
    }
}
