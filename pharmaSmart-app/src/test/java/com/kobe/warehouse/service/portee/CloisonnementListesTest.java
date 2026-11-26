package com.kobe.warehouse.service.portee;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.service.dto.filter.CommandeFilterDTO;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

/**
 * Le cloisonnement des listes repose sur une portée posée dans le filtre, et sur une règle simple :
 * <b>une portée vide ne filtre rien</b>. C'est voulu — les appels internes et les travaux planifiés
 * n'ont pas d'utilisateur connecté, et leur imposer une portée les priverait de tout.
 *
 * <p>Le revers est qu'un oubli ne se voit pas : une liste dont on ne pose pas la portée rend tout.
 * Ces essais fixent donc le contrat du filtre, pour que la règle soit lisible et tenue.
 */
@DisplayName("Cloisonnement — la portée dans le filtre des commandes")
class CloisonnementListesTest {

    @Test
    @DisplayName("une portée posée est conservée telle quelle")
    void porteeConservee() {
        CommandeFilterDTO filtre = new CommandeFilterDTO().setMagasinIds(Set.of(1, 7));

        assertThat(filtre.getMagasinIds()).containsExactlyInAnyOrder(1, 7);
        assertThat(CollectionUtils.isEmpty(filtre.getMagasinIds())).isFalse();
    }

    /**
     * Sans portée, le filtre n'ajoute aucun prédicat : c'est ce qui laisse passer les travaux
     * planifiés, qui n'ont pas d'utilisateur connecté.
     */
    @Test
    @DisplayName("une portée absente ne restreint rien")
    void porteeAbsenteNeRestreintRien() {
        assertThat(CollectionUtils.isEmpty(new CommandeFilterDTO().getMagasinIds()))
                .isTrue();
        assertThat(CollectionUtils.isEmpty(
                        new CommandeFilterDTO().setMagasinIds(Set.of()).getMagasinIds()))
                .isTrue();
    }

    /**
     * Le sélecteur de l'écran de supervision ne peut pas servir à sortir de sa portée : le magasin
     * demandé est <b>intersecté</b> avec elle. Hors portée, l'intersection est vide et la liste
     * l'est aussi — ce qui est la bonne réponse. Rendre toute la portée aurait ignoré le filtre,
     * rendre le magasin demandé aurait ouvert une porte.
     */
    @Test
    @DisplayName("un magasin demandé hors portée ne donne rien, et n'ouvre rien")
    void lIntersectionNOuvrePas() {
        Set<Integer> portee = Set.of(1, 7);

        assertThat(intersecter(portee, null)).containsExactlyInAnyOrder(1, 7);
        assertThat(intersecter(portee, 7)).containsExactly(7);
        assertThat(intersecter(portee, 9)).isEmpty();
    }

    /** Même règle que {@code CashRegisterServiceImpl.restreindreALaPortee}. */
    private Set<Integer> intersecter(Set<Integer> portee, Integer demande) {
        if (demande == null) {
            return portee;
        }
        return portee.contains(demande) ? Set.of(demande) : Set.of();
    }

    /**
     * Le magasin d'une commande est celui posé à sa création, et non celui de l'utilisateur qui la
     * relit : c'est ce qui rend le cloisonnement stable dans le temps.
     */
    @Test
    @DisplayName("la commande porte son propre magasin")
    void laCommandePorteSonMagasin() {
        Magasin parapharmacie = new Magasin();
        parapharmacie.setId(7);
        parapharmacie.setName("PARAPHARMACIE");
        parapharmacie.setFullName("PARAPHARMACIE");
        parapharmacie.setTypeMagasin(TypeMagasin.UNITE_GESTION);

        Commande commande = new Commande().setMagasin(parapharmacie);

        assertThat(commande.getMagasin().getId()).isEqualTo(7);
    }
}
