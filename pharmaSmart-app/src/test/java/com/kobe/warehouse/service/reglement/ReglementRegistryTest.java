package com.kobe.warehouse.service.reglement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.reglement.dto.ModeEditionReglement;
import com.kobe.warehouse.service.reglement.service.ReglementFactureModeAllService;
import com.kobe.warehouse.service.reglement.service.ReglementFactureSelectionneesService;
import com.kobe.warehouse.service.reglement.service.ReglementGroupeFactureService;
import com.kobe.warehouse.service.reglement.service.ReglementGroupeSelectionFactureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReglementRegistry")
class ReglementRegistryTest {

    @Mock
    private ReglementGroupeSelectionFactureService reglementGroupeSelectionFactureService;

    @Mock
    private ReglementGroupeFactureService reglementGroupeFactureService;

    @Mock
    private ReglementFactureModeAllService reglementFactureModeAllService;

    @Mock
    private ReglementFactureSelectionneesService reglementFactureSelectionneesService;

    private ReglementRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ReglementRegistry(
            reglementGroupeSelectionFactureService,
            reglementGroupeFactureService,
            reglementFactureModeAllService,
            reglementFactureSelectionneesService
        );
    }

    @Test
    @DisplayName("GROUPE_TOTAL est servi par le reglement de groupe complet")
    void groupeTotal() {
        assertThat(registry.getService(ModeEditionReglement.GROUPE_TOTAL)).isSameAs(reglementGroupeFactureService);
    }

    @Test
    @DisplayName("GROUPE_PARTIEL est servi par le reglement de groupe sur selection")
    void groupePartiel() {
        assertThat(registry.getService(ModeEditionReglement.GROUPE_PARTIEL)).isSameAs(reglementGroupeSelectionFactureService);
    }

    @Test
    @DisplayName("FACTURE_TOTAL est servi par le reglement de facture complete")
    void factureTotal() {
        assertThat(registry.getService(ModeEditionReglement.FACTURE_TOTAL)).isSameAs(reglementFactureModeAllService);
    }

    @Test
    @DisplayName("FACTURE_PARTIEL est servi par le reglement sur bons selectionnes")
    void facturePartiel() {
        assertThat(registry.getService(ModeEditionReglement.FACTURE_PARTIEL)).isSameAs(reglementFactureSelectionneesService);
    }

    @ParameterizedTest(name = "le mode {0} n est pas pris en charge")
    @EnumSource(
        value = ModeEditionReglement.class,
        names = { "GROUPE_TOTAL", "GROUPE_PARTIEL", "FACTURE_TOTAL", "FACTURE_PARTIEL" },
        mode = EnumSource.Mode.EXCLUDE
    )
    void modeNonPrisEnCharge(ModeEditionReglement mode) {
        assertThatThrownBy(() -> registry.getService(mode))
            .isInstanceOf(GenericError.class)
            .hasMessageContaining("Ce mode de facturation n'est pas pris en charge");
    }

    @Test
    @DisplayName("un mode nul n est pas pris en charge")
    void modeNul() {
        assertThatThrownBy(() -> registry.getService(null)).isInstanceOf(GenericError.class);
    }
}
