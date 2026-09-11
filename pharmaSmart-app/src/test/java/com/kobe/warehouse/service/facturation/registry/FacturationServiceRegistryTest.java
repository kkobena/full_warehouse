package com.kobe.warehouse.service.facturation.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import com.kobe.warehouse.service.facturation.service.EditionAllService;
import com.kobe.warehouse.service.facturation.service.EditionByGroupTiersService;
import com.kobe.warehouse.service.facturation.service.EditionBySelectionBonsService;
import com.kobe.warehouse.service.facturation.service.EditionByTiersPayantService;
import com.kobe.warehouse.service.facturation.service.EditionByTypeTiersPayantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Le registre qui aiguille chaque mode d'édition vers son service.
 *
 * <p>Un aiguillage n'a pas d'intérêt tant qu'il est complet ; ce qui compte, c'est le mode qu'il ne
 * connaît pas. {@link ModeEditionEnum} en compte six, le registre n'en enregistre que cinq :
 * demander le sixième doit échouer clairement plutôt que rendre {@code null}, faute de quoi
 * l'appelant part sur une référence nulle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FacturationServiceRegistry")
class FacturationServiceRegistryTest {

    @Mock
    private EditionAllService editionAllService;

    @Mock
    private EditionBySelectionBonsService editionBySelectionBonsService;

    @Mock
    private EditionByTiersPayantService editionByTiersPayantService;

    @Mock
    private EditionByTypeTiersPayantService editionByTypeTiersPayantService;

    @Mock
    private EditionByGroupTiersService editionByGroupTiersService;

    private FacturationServiceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new FacturationServiceRegistry(
            editionAllService,
            editionBySelectionBonsService,
            editionByTiersPayantService,
            editionByTypeTiersPayantService,
            editionByGroupTiersService
        );
    }

    @Test
    @DisplayName("chaque mode connu rend son service")
    void rendLeServiceDuMode() {
        assertThat(registry.getService(ModeEditionEnum.ALL)).isSameAs(editionAllService);
        assertThat(registry.getService(ModeEditionEnum.SELECTION_BON)).isSameAs(editionBySelectionBonsService);
        assertThat(registry.getService(ModeEditionEnum.TIERS_PAYANT)).isSameAs(editionByTiersPayantService);
        assertThat(registry.getService(ModeEditionEnum.TYPE)).isSameAs(editionByTypeTiersPayantService);
        assertThat(registry.getService(ModeEditionEnum.GROUP)).isSameAs(editionByGroupTiersService);
    }

    @Test
    @DisplayName("le mode SELECTED n'est pas pris en charge et le dit")
    void modeNonPrisEnCharge() {
        assertThatThrownBy(() -> registry.getService(ModeEditionEnum.SELECTED))
            .isInstanceOf(GenericError.class)
            .hasMessageContaining("n'est pas pris en charge");
    }
}
