package com.kobe.warehouse.service.customer.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AlerteSanteDerogation;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ProduitDci;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.StatutDci;
import com.kobe.warehouse.repository.AlerteSanteDerogationRepository;
import com.kobe.warehouse.repository.CustomerAllergieRepository;
import com.kobe.warehouse.repository.CustomerDossierSanteRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.nav.NavItemRepository;
import com.kobe.warehouse.repository.nav.NavItemRoleRepository;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.customer.AlerteSanteDTO;
import com.kobe.warehouse.service.customer.DerogationAuthorizer;
import com.kobe.warehouse.service.customer.DerogationAlerteSanteDTO;
import com.kobe.warehouse.service.customer.DossierSanteDTO;
import com.kobe.warehouse.service.customer.DossierSanteService;
import com.kobe.warehouse.service.dashboard.integration.AbstractDashboardIntegrationTest;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.license.LicenseService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fiche client, lot 2 (docs/PLAN-FICHE-CLIENT.md) : dossier de sécurité, alerte d'allergie à la
 * vente, et dérogation tracée — par le droit {@code pr-forcer-alerte-sante} ou par la clé d'un
 * collègue qui le détient.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Fiche client — sécurité du patient")
class DossierSanteIntegrationTest extends AbstractDashboardIntegrationTest {

    private static final String CLE_PHARMACIEN = "4321";

    private DossierSanteService service;
    private UserRepository userRepository;
    private UninsuredCustomer client;
    private Dci amoxicilline;
    private Produit clamoxyl;
    private Produit augmentin;
    private Produit doliprane;
    private AppUser caissier;
    private AppUser pharmacien;

    @BeforeEach
    void preparer() {
        userRepository = IntegrationPostgresDatabase.bean(UserRepository.class);
        LicenseService licenseService = mock(LicenseService.class);
        when(licenseService.hasFeature(any())).thenReturn(true);
        NavAccessService navAccess = new NavAccessService(
            IntegrationPostgresDatabase.bean(NavItemRoleRepository.class),
            IntegrationPostgresDatabase.bean(NavItemRepository.class),
            userRepository,
            licenseService,
            new NoOpCacheManager()
        );
        UserService userService = mock(UserService.class);
        when(userService.getUser()).thenAnswer(i -> userRepository.findOneByLogin(SecurityContextHolder.getContext().getAuthentication().getName()).orElse(null));
        when(userService.getUserByPwdOrSecurityKey(anyString())).thenAnswer(i ->
            userRepository.findOneByActionAuthorityKey(DigestUtils.sha256Hex((String) i.getArgument(0)))
        );
        UtilisationCleSecuriteService cles = mock(UtilisationCleSecuriteService.class);
        NavItemRepository navItems = IntegrationPostgresDatabase.bean(NavItemRepository.class);
        when(cles.hasPrivilege(anyString(), anyString())).thenAnswer(i -> navItems.existByCodeAndRoleName(i.getArgument(0), i.getArgument(1)));

        service = new DossierSanteService(
            IntegrationPostgresDatabase.bean(CustomerRepository.class),
            IntegrationPostgresDatabase.bean(CustomerDossierSanteRepository.class),
            IntegrationPostgresDatabase.bean(CustomerAllergieRepository.class),
            IntegrationPostgresDatabase.bean(AlerteSanteDerogationRepository.class),
            new DerogationAuthorizer(navAccess, userService, cles),
            em
        );

        client = client(unique("SANTE"), "Awa", "0700000020");
        amoxicilline = dci(unique("AMOXICILLINE"), StatutDci.ACTIVE);
        clamoxyl = produitAvec(amoxicilline);
        augmentin = produitAvec(dci(amoxicilline.getLibelle() + "/ACIDE CLAVULANIQUE", StatutDci.COMPOSEE));
        doliprane = produitAvec(dci(unique("PARACETAMOL"), StatutDci.ACTIVE));
        caissier = utilisateur("ROLE_CAISSIER", null);
        pharmacien = utilisateur("ROLE_PHARMACIEN", CLE_PHARMACIEN);
        connecter(caissier);
    }

    @Test
    @DisplayName("le dossier s'enregistre et se relit, allergies comprises")
    void enregistrerEtRelire() {
        DossierSanteDTO enregistre = service.enregistrer(client.getId(), dossier(allergie(amoxicilline, "urticaire"), new DossierSanteDTO.Allergie(null, null, null, "Arachide", null)));

        assertThat(enregistre.allergies()).extracting(DossierSanteDTO.Allergie::dciLibelle).containsExactly(amoxicilline.getLibelle(), null);
        assertThat(enregistre.allergies()).extracting(DossierSanteDTO.Allergie::libelle).containsExactly(null, "Arachide");
        assertThat(enregistre.pathologies()).containsExactly("Diabète");
        assertThat(enregistre.poidsKg()).isEqualByComparingTo("62.5");
        assertThat(enregistre.datePesee()).isEqualTo(LocalDate.now());

        // Retirer une allergie la supprime ; ressaisir la même molécule n'en crée pas deux.
        DossierSanteDTO modifie = service.enregistrer(client.getId(), dossier(allergie(amoxicilline, null), allergie(amoxicilline, null)));
        assertThat(modifie.allergies()).hasSize(1);
    }

    @Test
    @DisplayName("une allergie bloque le produit qui contient la molécule, y compris dans une association non décomposée")
    void alerteAllergie() {
        service.enregistrer(client.getId(), dossier(allergie(amoxicilline, "œdème")));

        assertThat(service.alertes(client.getId(), clamoxyl.getId()))
            .singleElement()
            .satisfies(a -> {
                assertThat(a.niveau()).isEqualTo(AlerteSanteDTO.BLOQUANTE);
                assertThat(a.message()).contains("AMOXICILLINE").contains("œdème");
            });
        assertThat(service.alertes(client.getId(), augmentin.getId())).extracting(AlerteSanteDTO::niveau).containsExactly(AlerteSanteDTO.BLOQUANTE);
        assertThat(service.alertes(client.getId(), doliprane.getId())).isEmpty();
    }

    @Test
    @DisplayName("grossesse et allaitement ne font que rappeler ; une grossesse à terme dépassé ne rappelle plus rien")
    void grossesseEtAllaitement() {
        service.enregistrer(client.getId(), new DossierSanteDTO(List.of(), List.of(), true, LocalDate.now().plusMonths(3), true, null, null, null, null, null));
        assertThat(service.alertes(client.getId(), doliprane.getId())).extracting(AlerteSanteDTO::type).containsExactly("GROSSESSE", "ALLAITEMENT");
        assertThat(service.alertes(client.getId(), doliprane.getId())).noneMatch(AlerteSanteDTO::bloquante);

        service.enregistrer(client.getId(), new DossierSanteDTO(List.of(), List.of(), true, LocalDate.now().minusDays(1), false, null, null, null, null, null));
        assertThat(service.alertes(client.getId(), doliprane.getId())).isEmpty();
    }

    @Test
    @DisplayName("sans le droit ni une clé valide, la dérogation est refusée")
    void derogationRefusee() {
        service.enregistrer(client.getId(), dossier(allergie(amoxicilline, null)));
        DerogationAlerteSanteDTO sansCle = new DerogationAlerteSanteDTO(clamoxyl.getId(), "prescription maintenue", null);
        DerogationAlerteSanteDTO mauvaiseCle = new DerogationAlerteSanteDTO(clamoxyl.getId(), "prescription maintenue", "0000");

        assertThatThrownBy(() -> service.deroger(client.getId(), sansCle)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.deroger(client.getId(), mauvaiseCle)).isInstanceOf(ForbiddenOperationException.class);
        assertThat(derogations()).isEmpty();
    }

    @Test
    @DisplayName("la clé du pharmacien autorise le caissier, et la trace nomme les deux")
    void derogationParCle() {
        service.enregistrer(client.getId(), dossier(allergie(amoxicilline, null)));

        service.deroger(client.getId(), new DerogationAlerteSanteDTO(clamoxyl.getId(), "prescription maintenue", CLE_PHARMACIEN));

        assertThat(derogations()).singleElement().satisfies(d -> {
            assertThat(d.getUser().getId()).isEqualTo(caissier.getId());
            assertThat(d.getAutorisePar().getId()).isEqualTo(pharmacien.getId());
            assertThat(d.getAlertes()).contains("AMOXICILLINE");
            assertThat(d.getMotif()).isEqualTo("prescription maintenue");
        });
    }

    @Test
    @DisplayName("le pharmacien, qui détient le droit, déroge sans clé")
    void derogationParDroit() {
        service.enregistrer(client.getId(), dossier(allergie(amoxicilline, null)));
        connecter(pharmacien);

        service.deroger(client.getId(), new DerogationAlerteSanteDTO(clamoxyl.getId(), "avis du médecin", null));

        assertThat(derogations()).singleElement().extracting(d -> d.getAutorisePar().getId()).isEqualTo(pharmacien.getId());
    }

    // ===== fixtures =====

    private List<AlerteSanteDerogation> derogations() {
        em.flush();
        return IntegrationPostgresDatabase.bean(AlerteSanteDerogationRepository.class).findAllByCustomerIdOrderByCreatedAtDesc(client.getId());
    }

    private static DossierSanteDTO.Allergie allergie(Dci dci, String reaction) {
        return new DossierSanteDTO.Allergie(null, dci.getId(), null, null, reaction);
    }

    private static DossierSanteDTO dossier(DossierSanteDTO.Allergie... allergies) {
        return new DossierSanteDTO(List.of(allergies), List.of("Diabète", " ", "Diabète"), false, null, false, new BigDecimal("62.5"), null, null, null, null);
    }

    private Dci dci(String libelle, StatutDci statut) {
        Dci dci = new Dci();
        dci.setCode(unique("DCI"));
        dci.setLibelle(libelle);
        dci.setStatut(statut);
        em.persist(dci);
        return dci;
    }

    private Produit produitAvec(Dci dci) {
        Produit produit = produit(unique("PRODUIT"), 0);
        em.persist(new ProduitDci().setProduit(produit).setDci(dci).setRang(1));
        em.flush();
        return produit;
    }

    private AppUser utilisateur(String role, String cle) {
        AppUser user = new AppUser();
        user.setLogin(unique("sante").toLowerCase());
        user.setPassword("x".repeat(60));
        user.setActivated(true);
        user.setLangKey("fr");
        user.setMagasin(em.find(Magasin.class, MAGASIN_ID));
        user.setAuthorities(Set.of(role).stream().map(r -> em.find(Authority.class, r)).collect(Collectors.toSet()));
        em.persist(user);
        em.flush();
        if (cle != null) {
            // Posée en SQL, comme le jeu de démonstration : l'entité valide la saisie (chiffres), la base garde son empreinte.
            em
                .createNativeQuery("UPDATE app_user SET action_authority_key = :cle WHERE id = :id")
                .setParameter("cle", DigestUtils.sha256Hex(cle))
                .setParameter("id", user.getId())
                .executeUpdate();
            em.refresh(user);
        }
        return user;
    }

    private static void connecter(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getLogin(), null, List.of()));
    }
}
