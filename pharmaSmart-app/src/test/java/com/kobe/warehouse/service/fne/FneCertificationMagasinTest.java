package com.kobe.warehouse.service.fne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.PlanificationCertificationFne;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.fne.model.CertificationFneResult;
import com.kobe.warehouse.service.fne.service.FneCertificationTransactionService;
import com.kobe.warehouse.service.fne.service.FneServiceImpl;
import com.kobe.warehouse.service.license.LicenseService;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La facture normalisée porte le nom de l'établissement qui certifie et son point de vente FNE :
 * c'est un document fiscal, et le mauvais nom dessus n'est pas un défaut d'affichage.
 *
 * <p>Le service prenait l'établissement par {@code magasinRepository.findAll().getFirst()}.
 * {@code findAll()} n'impose aucun ordre, et il suffit qu'une installation ait un second magasin —
 * un dépôt, une parapharmacie — pour que le premier venu ne soit pas l'officine. Vérifié sur une
 * base réelle portant une officine et un dépôt : c'est le <b>dépôt</b> qui sortait en premier.
 *
 * <p>Seule l'officine certifie : un dépôt ne vend pas dans le système et une unité de gestion
 * facture sous l'identité de son officine. Ces deux essais tiennent cette règle par les deux bouts
 * — on prend bien l'officine, et à défaut d'officine on ne certifie rien plutôt que n'importe quoi.
 */
@DisplayName("FNE — seule l'officine certifie")
class FneCertificationMagasinTest {

    private FacturationRepository facturationRepository;
    private MagasinRepository magasinRepository;
    private FneCertificationTransactionService certificationTransactionService;
    private LicenseService licenseService;
    private FneServiceImpl service;

    @BeforeEach
    void preparer() {
        facturationRepository = mock(FacturationRepository.class);
        magasinRepository = mock(MagasinRepository.class);
        certificationTransactionService = mock(FneCertificationTransactionService.class);
        licenseService = mock(LicenseService.class);
        when(licenseService.isDemo()).thenReturn(false);

        service = new FneServiceImpl(
            facturationRepository,
            mock(HttpClient.class),
            mock(StorageService.class),
            new ObjectMapper(),
            magasinRepository,
            certificationTransactionService,
            licenseService
        );
    }

    @Test
    @DisplayName("l'établissement certifié est l'officine, jamais le premier magasin venu")
    void certifieSousLOfficine() {
        when(facturationRepository.findPendingFneCertification()).thenReturn(List.of(new FactureTiersPayant()));
        when(magasinRepository.findFirstByTypeMagasinOrderByIdAsc(TypeMagasin.OFFICINE)).thenReturn(Optional.of(officine()));

        service.certifierFacturesPendantes(plan());

        verify(magasinRepository).findFirstByTypeMagasinOrderByIdAsc(TypeMagasin.OFFICINE);
        verify(magasinRepository, never()).findAll();
    }

    /**
     * Ne rien certifier vaut mieux que certifier sous un nom qui n'est pas le bon : les factures
     * restent en attente, rien n'est perdu, et le journal dit pourquoi.
     */
    @Test
    @DisplayName("sans officine paramétrée, aucune facture n'est certifiée")
    void sansOfficineRienNEstCertifie() {
        when(facturationRepository.findPendingFneCertification()).thenReturn(List.of(new FactureTiersPayant()));
        when(magasinRepository.findFirstByTypeMagasinOrderByIdAsc(TypeMagasin.OFFICINE)).thenReturn(Optional.empty());

        CertificationFneResult resultat = service.certifierFacturesPendantes(plan());

        assertThat(resultat.nbCertifiees()).isZero();
        assertThat(resultat.nbEchecs()).isZero();
        verifyNoInteractions(certificationTransactionService);
    }

    @Test
    @DisplayName("sans facture en attente, le magasin n'est même pas résolu")
    void sansFactureRienNEstResolu() {
        when(facturationRepository.findPendingFneCertification()).thenReturn(List.of());

        CertificationFneResult resultat = service.certifierFacturesPendantes(plan());

        assertThat(resultat.nbCertifiees()).isZero();
        verifyNoInteractions(magasinRepository);
        verifyNoInteractions(certificationTransactionService);
    }

    private Magasin officine() {
        Magasin magasin = new Magasin();
        magasin.setName("OFFICINE");
        magasin.setFullName("PHARMACIE PHARMA SMART");
        magasin.setTypeMagasin(TypeMagasin.OFFICINE);
        magasin.setFnePointOfSale("PV-001");
        return magasin;
    }

    private PlanificationCertificationFne plan() {
        return new PlanificationCertificationFne().setId(1);
    }
}
