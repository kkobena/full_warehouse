package com.kobe.warehouse.service.facturation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.id_generator.FactureIdGeneratorService;
import com.kobe.warehouse.service.id_generator.InvoiceGenerationCodeGeneratorService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

/**
 * La numérotation des factures et le regroupement des dossiers, éprouvés sans base.
 *
 * <p>Deux règles se décident ici et nulle part ailleurs : le format du numéro de facture, et la
 * façon dont l'indice repart — il se poursuit d'une année sur l'autre, sauf si le paramétrage
 * demande une remise à zéro annuelle. Cette seconde règle croise trois conditions (changement
 * d'année, présence du paramètre, valeur du paramètre) ; les combinaisons se couvrent bien plus
 * vite ici qu'en insérant des factures.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractEditionFactureService")
class AbstractEditionFactureServiceTest {

    @Mock
    private ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    @Mock
    private FacturationRepository facturationRepository;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private UserService userService;

    @Mock
    private FactureIdGeneratorService factureIdGeneratorService;

    @Mock
    private InvoiceGenerationCodeGeneratorService invoiceGenerationCodeGeneratorService;

    private TestableEditionService service;

    @BeforeEach
    void setUp() {
        service = new TestableEditionService(
            thirdPartySaleLineRepository,
            facturationRepository,
            appConfigurationService,
            userService,
            factureIdGeneratorService,
            invoiceGenerationCodeGeneratorService
        );
    }

    @Nested
    @DisplayName("numéro de facture")
    class NumeroDeFacture {

        @Test
        @DisplayName("est l'année et l'indice sur quatre chiffres")
        void format() {
            assertThat(service.numero(2026, 7)).isEqualTo("2026_0007");
            assertThat(service.numero(2026, 1234)).isEqualTo("2026_1234");
        }

        @Test
        @DisplayName("laisse déborder un indice à cinq chiffres plutôt que de le tronquer")
        void debordement() {
            assertThat(service.numero(2026, 12345)).isEqualTo("2026_12345");
        }
    }

    @Nested
    @DisplayName("indice de départ")
    class IndiceDeDepart {

        @Test
        @DisplayName("part de zéro quand aucune facture n'a encore été émise")
        void premiereFacture() {
            when(facturationRepository.findLatestFactureNumber()).thenReturn(null);

            assertThat(service.dernierNumero()).isZero();
        }

        @Test
        @DisplayName("reprend l'indice de la dernière facture de l'année en cours")
        void memeAnnee() {
            when(facturationRepository.findLatestFactureNumber()).thenReturn(Year.now() + "_0042");

            assertThat(service.dernierNumero()).isEqualTo(42);
        }

        @Test
        @DisplayName("poursuit la numérotation d'une année sur l'autre par défaut")
        void anneePrecedenteSansRemiseAZero() {
            when(facturationRepository.findLatestFactureNumber()).thenReturn(Year.now().minusYears(1) + "_0042");
            when(appConfigurationService.findParamResetInvoiceNumberEveryYear()).thenReturn(Optional.empty());

            assertThat(service.dernierNumero()).isEqualTo(42);
        }

        @Test
        @DisplayName("repart de zéro à l'année nouvelle si le paramétrage le demande")
        void remiseAZeroAnnuelle() {
            when(facturationRepository.findLatestFactureNumber()).thenReturn(Year.now().minusYears(1) + "_0042");
            when(appConfigurationService.findParamResetInvoiceNumberEveryYear()).thenReturn(Optional.of(configuration("1")));

            assertThat(service.dernierNumero()).isZero();
        }

        @Test
        @DisplayName("ne repart pas de zéro si le paramètre est présent mais désactivé")
        void parametreDesactive() {
            when(facturationRepository.findLatestFactureNumber()).thenReturn(Year.now().minusYears(1) + "_0042");
            when(appConfigurationService.findParamResetInvoiceNumberEveryYear()).thenReturn(Optional.of(configuration("0")));

            assertThat(service.dernierNumero()).isEqualTo(42);
        }

        @Test
        @DisplayName("ne consulte pas le paramétrage tant qu'on est dans la même année")
        void parametreIgnoreDansLAnnee() {
            when(facturationRepository.findLatestFactureNumber()).thenReturn(Year.now() + "_0007");

            assertThat(service.dernierNumero()).isEqualTo(7);
            // findParamResetInvoiceNumberEveryYear n'est pas stubbé : l'appeler ferait échouer le test.
        }

        private AppConfiguration configuration(String valeur) {
            AppConfiguration configuration = new AppConfiguration();
            configuration.setValue(valeur);
            return configuration;
        }
    }

    @Nested
    @DisplayName("regroupement des dossiers")
    class Regroupement {

        @Test
        @DisplayName("range chaque dossier sous l'organisme de son compte assuré")
        void parOrganisme() {
            TiersPayant cnam = tiersPayant(1, "CNAM");
            TiersPayant mugef = tiersPayant(2, "MUGEF");

            Map<TiersPayant, List<ThirdPartySaleLine>> groupes = service.grouper(
                List.of(dossier(cnam), dossier(mugef), dossier(cnam))
            );

            assertThat(groupes).hasSize(2);
            assertThat(groupes.get(cnam)).hasSize(2);
            assertThat(groupes.get(mugef)).hasSize(1);
        }

        private TiersPayant tiersPayant(int id, String nom) {
            TiersPayant tiersPayant = new TiersPayant();
            tiersPayant.setId(id);
            tiersPayant.setName(nom);
            return tiersPayant;
        }

        private ThirdPartySaleLine dossier(TiersPayant tiersPayant) {
            ClientTiersPayant compte = new ClientTiersPayant();
            compte.setTiersPayant(tiersPayant);
            compte.setAssuredCustomer(new AssuredCustomer());
            ThirdPartySaleLine dossier = new ThirdPartySaleLine();
            dossier.setClientTiersPayant(compte);
            return dossier;
        }
    }

    /** Ouvre les méthodes protégées du socle, seul moyen de les éprouver isolément. */
    private static final class TestableEditionService extends AbstractEditionFactureService {

        private TestableEditionService(
            ThirdPartySaleLineRepository thirdPartySaleLineRepository,
            FacturationRepository facturationRepository,
            AppConfigurationService appConfigurationService,
            UserService userService,
            FactureIdGeneratorService factureIdGeneratorService,
            InvoiceGenerationCodeGeneratorService invoiceGenerationCodeGeneratorService
        ) {
            super(
                thirdPartySaleLineRepository,
                facturationRepository,
                appConfigurationService,
                userService,
                factureIdGeneratorService,
                invoiceGenerationCodeGeneratorService
            );
        }

        @Override
        protected Specification<ThirdPartySaleLine> buildCriteria(EditionSearchParams editionSearchParams) {
            return null;
        }

        String numero(int annee, int indice) {
            return getFactureNumber(annee, indice);
        }

        int dernierNumero() {
            return getLastFactureNumero();
        }

        Map<TiersPayant, List<ThirdPartySaleLine>> grouper(List<ThirdPartySaleLine> dossiers) {
            return groupByTiersPayant(dossiers);
        }
    }
}
