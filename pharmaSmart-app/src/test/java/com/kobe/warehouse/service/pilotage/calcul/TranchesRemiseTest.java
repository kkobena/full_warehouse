package com.kobe.warehouse.service.pilotage.calcul;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Pilotage — tranches de remise")
class TranchesRemiseTest {

    private final TranchesRemise tranches = TranchesRemise.decouper(List.of(5, 10, 20));

    @ParameterizedTest(name = "{0} % → {2}")
    @CsvSource({ "0, 0-0, Sans remise", "1, 1-4, Moins de 5 %", "4, 1-4, Moins de 5 %", "5, 5-9, 5 à 10 %", "19, 10-19, 10 à 20 %", "20, 20-100, 20 % et plus", "100, 20-100, 20 % et plus" })
    void classer(int taux, String cle, String libelle) {
        assertThat(tranches.classer(taux)).isEqualTo(new MembreAnalyseDTO(cle, libelle));
    }

    @Test
    @DisplayName("un seuil à 1 % ou en double ne crée pas de tranche vide")
    void seuilsDegeneres() {
        assertThat(TranchesRemise.decouper(List.of(1, 10, 10)).tranches()).extracting(TranchesRemise.Tranche::libelle).containsExactly("Sans remise", "Moins de 10 %", "10 % et plus");
    }
}
