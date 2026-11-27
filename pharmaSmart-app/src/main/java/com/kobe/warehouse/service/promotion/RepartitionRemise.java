package com.kobe.warehouse.service.promotion;

/**
 * Répartit une remise entière entre plusieurs lignes, au prorata de leur poids, par la méthode du
 * plus fort reste (plan §5.8) : chaque ligne reçoit la partie entière de sa quote-part, puis les
 * francs qui restent vont, un par un, aux lignes de plus forte partie décimale.
 *
 * <p>Pas de {@code Math.ceil} ligne par ligne : la somme des parts dépasserait la remise voulue. Ici
 * elle lui est toujours égale, au franc près. À reste égal, la ligne la plus haute dans la liste
 * l'emporte : le résultat ne dépend que de l'ordre donné.
 */
public final class RepartitionRemise {

    private RepartitionRemise() {}

    /**
     * @param total la remise à répartir, en francs
     * @param poids le montant de chaque ligne participante (positif ou nul)
     * @return la part de chaque ligne, dans l'ordre des poids ; leur somme vaut {@code total}
     *     (zéro partout si tous les poids sont nuls)
     */
    public static int[] auPlusFortReste(long total, long[] poids) {
        int[] parts = new int[poids.length];
        long somme = 0;
        for (long p : poids) {
            somme += Math.max(0, p);
        }
        if (total <= 0 || somme == 0) {
            return parts;
        }
        long distribue = 0;
        long[] restes = new long[poids.length];
        for (int i = 0; i < poids.length; i++) {
            long quotePart = total * Math.max(0, poids[i]);
            parts[i] = (int) (quotePart / somme);
            restes[i] = poids[i] > 0 ? quotePart % somme : -1;
            distribue += parts[i];
        }
        for (long reste = total - distribue; reste > 0; reste--) {
            int plusFort = -1;
            for (int i = 0; i < restes.length; i++) {
                if (restes[i] >= 0 && (plusFort < 0 || restes[i] > restes[plusFort])) {
                    plusFort = i;
                }
            }
            parts[plusFort]++;
            // Une ligne ne reçoit qu'un franc de reste : elle sort du tour suivant.
            restes[plusFort] = -1;
        }
        return parts;
    }
}
