package com.kobe.warehouse.service.exports.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ExportFichier;
import com.kobe.warehouse.domain.ExportModele;
import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.PeriodeRelative;
import com.kobe.warehouse.domain.enumeration.ScheduledReportFrequency;
import com.kobe.warehouse.domain.enumeration.StatutExport;
import com.kobe.warehouse.repository.ExportFichierRepository;
import com.kobe.warehouse.repository.ExportModeleRepository;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.exports.DemandeExportDTO;
import com.kobe.warehouse.service.dto.exports.ExportFichierDTO;
import com.kobe.warehouse.service.dto.exports.ExportModeleDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.exports.ExportsFichiersService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Exports — demandes, historique, modèles")
class ExportsServicesTest {

    private static final AppUser MOI = utilisateur(1);

    @Mock
    private ExportFichierRepository exportFichierRepository;

    @Mock
    private GenerateurExports generateurExports;

    @Mock
    private RepertoireExports repertoireExports;

    @Mock
    private NavAccessService navAccessService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ExportsFichiersServiceImpl fichiers;

    @BeforeEach
    void connecter() {
        TransactionSynchronizationManager.initSynchronization();
        when(userService.getUser()).thenReturn(MOI);
        when(navAccessService.isAllowed(List.of("exports.donnees"), NavAction.EXPORT)).thenReturn(true);
        when(exportFichierRepository.save(any())).thenAnswer(appel -> appel.getArgument(0));
    }

    @AfterEach
    void fermer() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("une demande est inscrite en attente ; la génération part après validation de la transaction")
    void demandeInscrite() {
        ExportFichierDTO fichier = fichiers.demander(new DemandeExportDTO(ExportDonnees.VENTES, FormatExport.CSV, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 9)));

        assertThat(fichier.statut()).isEqualTo(StatutExport.EN_ATTENTE);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        verify(generateurExports, never()).generer(any());
    }

    @Test
    @DisplayName("sans le droit de la rubrique (nominatif), refusé ; un export périodique exige une période")
    void refus() {
        assertThatThrownBy(() -> fichiers.demander(new DemandeExportDTO(ExportDonnees.CLIENTS, FormatExport.CSV, null, null))).isInstanceOf(
            ForbiddenOperationException.class
        );
        assertThatThrownBy(() -> fichiers.demander(new DemandeExportDTO(ExportDonnees.VENTES, FormatExport.CSV, null, null))).isInstanceOf(GenericError.class);
    }

    @Test
    @DisplayName("on ne télécharge pas le fichier d'un collègue")
    void fichierDUnCollegue() {
        when(exportFichierRepository.findById(5L)).thenReturn(
            Optional.of(new ExportFichier().setExport(ExportDonnees.VENTES).setDemandePar(utilisateur(2)).setStatut(StatutExport.TERMINE))
        );

        assertThatThrownBy(() -> fichiers.lireFichier(5L)).isInstanceOf(ForbiddenOperationException.class);
    }

    @Nested
    @DisplayName("modèles")
    class Modeles {

        @Mock
        private ExportModeleRepository exportModeleRepository;

        @Mock
        private ExportsFichiersService exportsFichiersService;

        private ModelesExportServiceImpl modeles;

        @BeforeEach
        void creer() {
            modeles = new ModelesExportServiceImpl(exportModeleRepository, exportsFichiersService, navAccessService, userService);
            when(exportModeleRepository.save(any())).thenAnswer(appel -> appel.getArgument(0));
        }

        @Test
        @DisplayName("un modèle programmé reçoit sa prochaine exécution ; un modèle périodique exige sa période")
        void enregistrement() {
            ExportModeleDTO modele = modeles.enregistrerModele(dto(PeriodeRelative.MOIS_PRECEDENT, ScheduledReportFrequency.MONTHLY));

            assertThat(modele.prochaineExecution()).isAfter(LocalDateTime.now());
            assertThat(modele.heure()).isEqualTo(LocalTime.of(7, 0));
            assertThat(modele.modifiable()).isTrue();
            assertThatThrownBy(() -> modeles.enregistrerModele(dto(null, null))).isInstanceOf(GenericError.class);
        }

        @Test
        @DisplayName("les modèles dus partent au nom de leur propriétaire, puis sont reprogrammés")
        void programmes() {
            LocalDateTime maintenant = LocalDateTime.of(2026, 10, 9, 10, 0);
            ExportModele du = new ExportModele()
                .setLibelle("Ventes du mois")
                .setExport(ExportDonnees.VENTES)
                .setFormat(FormatExport.XLSX)
                .setPeriode(PeriodeRelative.MOIS_PRECEDENT)
                .setProprietaire(utilisateur(2))
                .setFrequence(ScheduledReportFrequency.DAILY)
                .setHeure(LocalTime.of(7, 0));
            when(exportModeleRepository.listerProgrammesDus(maintenant)).thenReturn(List.of(du));

            modeles.executerProgrammes(maintenant);

            verify(exportsFichiersService).lancerModele(eq(du), eq(du.getProprietaire()), eq(maintenant.toLocalDate()));
            assertThat(du.getProchaineExecution()).isEqualTo(LocalDateTime.of(2026, 10, 10, 7, 0));
        }

        @Test
        @DisplayName("on ne rejoue pas le modèle privé d'un collègue")
        void modelePrive() {
            when(exportModeleRepository.findById(3L)).thenReturn(
                Optional.of(new ExportModele().setExport(ExportDonnees.VENTES).setProprietaire(utilisateur(2)).setPartage(false))
            );

            assertThatThrownBy(() -> modeles.executerModele(3L)).isInstanceOf(ForbiddenOperationException.class);
            verify(exportsFichiersService, never()).lancerModele(any(), any(), any());
        }

        private ExportModeleDTO dto(PeriodeRelative periode, ScheduledReportFrequency frequence) {
            return new ExportModeleDTO(null, "Ventes du mois", ExportDonnees.VENTES, FormatExport.XLSX, periode, false, frequence, null, 1, null, null, false);
        }
    }

    private static AppUser utilisateur(int id) {
        AppUser utilisateur = new AppUser();
        utilisateur.setId(id);
        utilisateur.setFirstName("Prénom");
        utilisateur.setLastName("Nom" + id);
        return utilisateur;
    }
}
