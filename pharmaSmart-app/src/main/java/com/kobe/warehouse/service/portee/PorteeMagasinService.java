package com.kobe.warehouse.service.portee;

import com.kobe.warehouse.domain.Magasin;
import java.util.Set;

/**
 * Les deux portées d'un utilisateur, qu'il ne faut jamais confondre.
 *
 * <p><b>Le magasin d'exploitation</b> est celui où j'écris : un seul, celui de mon compte. Il ne se
 * négocie pas et ne dépend d'aucun droit.
 *
 * <p><b>La portée de supervision</b> est ce que je peux lire : mon magasin, plus ses rattachés si
 * je détiens le droit {@code supervision.magasins}. C'est ce qui permet au titulaire de consulter
 * les mouvements de son dépôt ou de sa parapharmacie <b>sans s'y connecter</b> — la demande qui a
 * ouvert tout ce chantier.
 *
 * <p>Le droit n'élargit que la lecture. Écrire ailleurs que dans son magasin reste impossible.
 */
public interface PorteeMagasinService {
    /** Magasin où l'utilisateur courant écrit. */
    Magasin resoudreLeMagasinDExploitation();

    /**
     * Identifiants des magasins que l'utilisateur courant peut lire. Contient toujours au moins son
     * magasin d'exploitation : sans le droit de supervision, il est seul.
     */
    Set<Integer> resoudreLaPorteeDeSupervision();

    /** Vrai si la portée de lecture dépasse le seul magasin d'exploitation. */
    boolean superviseDAutresMagasins();
}
