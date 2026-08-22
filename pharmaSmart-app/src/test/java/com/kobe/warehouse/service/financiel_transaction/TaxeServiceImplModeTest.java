package com.kobe.warehouse.service.financiel_transaction;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.declaration_ca.ModeChiffreAffaireResolver;
import com.kobe.warehouse.service.financiel_transaction.dto.ModeChiffreAffaire;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Le mode de lecture est décidé par le serveur, jamais par le client.
 *
 * <p>{@code mode} arrive dans la requête HTTP : un appel forgé, ou une officine dont la licence a
 * expiré, réclamerait sinon des montants retraités que rien n'autorise. Le masquage des menus et les
 * gardes de route ne couvrent que l'usage courant ; c'est ici que la règle devient opposable.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Rapport TVA — le mode est borné par la licence")
class TaxeServiceImplModeTest {

    @Mock
    private TvaReportReportService tvaReportService;

    @Mock
    private DeclarationTvaPdfReportService declarationTvaPdfReportService;

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ModeChiffreAffaireResolver modeResolver;

    private TaxeServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new TaxeServiceImpl(
            tvaReportService,
            declarationTvaPdfReportService,
            salesRepository,
            appConfigurationService,
            objectMapper,
            modeResolver
        );
        when(modeResolver.resoudre(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(salesRepository.fetchSalesTvaReport(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), anyString()))
            .thenReturn("[]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(new ArrayList<>());
    }





    @Test
    @DisplayName("Un mode DECLARE non souscrit est ramené au chiffre réel avant d'atteindre le SQL")
    void modeDeclareNonSouscritEstRamenéAuReel() {
        // Le résolveur refuse : c'est le cas d'une requête forgée, ou d'une licence expirée.
        when(modeResolver.resoudre(ModeChiffreAffaire.DECLARE)).thenReturn(ModeChiffreAffaire.REEL);
        MvtParam param = param().setMode(ModeChiffreAffaire.DECLARE);

        service.fetchTaxe(param, false);

        verify(salesRepository).fetchSalesTvaReport(any(), any(), any(), any(), eq(false), eq("REEL"));
    }

    @Test
    @DisplayName("Un mode DECLARE souscrit atteint bien le SQL")
    void modeDeclareSouscritEstTransmis() {
        when(modeResolver.resoudre(ModeChiffreAffaire.DECLARE)).thenReturn(ModeChiffreAffaire.DECLARE);
        MvtParam param = param().setMode(ModeChiffreAffaire.DECLARE);

        service.fetchTaxe(param, false);

        verify(salesRepository).fetchSalesTvaReport(any(), any(), any(), any(), eq(false), eq("DECLARE"));
    }

    private MvtParam param() {
        return new MvtParam(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            Set.of(CategorieChiffreAffaire.CA),
            null,
            null,
            "codeTva"
        ).build();
    }
}
