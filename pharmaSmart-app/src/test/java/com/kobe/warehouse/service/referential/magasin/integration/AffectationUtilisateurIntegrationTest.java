package com.kobe.warehouse.service.referential.magasin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.repository.AuthorityRepository;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.PersistentTokenRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dashboard.integration.AbstractDashboardIntegrationTest;
import com.kobe.warehouse.service.dto.AdminUserDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Le magasin d'affectation d'un utilisateur, éprouvé sur PostgreSQL.
 *
 * <p>Le test unitaire ({@code AffectationUtilisateurTest}) fixe les règles ; celui-ci vérifie ce que
 * les mocks ne voient pas : la remontée de la hiérarchie à travers les proxys paresseux de
 * {@code parent_id}, la requête dérivée qui détecte une caisse ouverte, et le magasin réellement
 * écrit dans {@code app_user} — relu après vidage du contexte de persistance.
 *
 * <p>Le créateur est l'utilisateur {@code system} des migrations, rattaché au magasin 1.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Utilisateur — le magasin d'affectation, sur base réelle")
class AffectationUtilisateurIntegrationTest extends AbstractDashboardIntegrationTest {

    private UserService userService;
    private UserRepository userRepository;

    @BeforeEach
    void preparer() {
        userRepository = IntegrationPostgresDatabase.bean(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        // password_hash est un CHAR(60) borné : un hachage factice doit en avoir la longueur.
        when(passwordEncoder.encode(anyString())).thenReturn("x".repeat(60));

        userService = new UserService(
            userRepository,
            passwordEncoder,
            IntegrationPostgresDatabase.bean(PersistentTokenRepository.class),
            IntegrationPostgresDatabase.bean(AuthorityRepository.class),
            IntegrationPostgresDatabase.bean(MagasinRepository.class),
            IntegrationPostgresDatabase.bean(CashRegisterRepository.class)
        );
    }

    // ===== création =====

    @Test
    @DisplayName("sans magasin demandé, le compte est écrit dans le magasin du créateur")
    void sansDemandeOnHerite() {
        AppUser cree = userService.createUser(dto(unique("herite"), null));

        assertThat(magasinEnBase(cree)).isEqualTo(MAGASIN_ID);
    }

    @Test
    @DisplayName("une parapharmacie rattachée est écrite comme magasin du compte")
    void unRattacheEstEcrit() {
        Magasin parapharmacie = magasin(TypeMagasin.UNITE_GESTION, magasin);

        AppUser cree = userService.createUser(dto(unique("para"), parapharmacie.getId()));

        assertThat(magasinEnBase(cree)).isEqualTo(parapharmacie.getId());
    }

    /**
     * La remontée traverse plusieurs niveaux de {@code parent_id}, chargés paresseusement : un
     * petit-enfant du magasin du créateur reste dans sa portée.
     */
    @Test
    @DisplayName("un magasin rattaché indirectement reste dans la portée")
    void remonteeSurPlusieursNiveaux() {
        Magasin intermediaire = magasin(TypeMagasin.UNITE_GESTION, magasin);
        Magasin petitEnfant = magasin(TypeMagasin.UNITE_GESTION, intermediaire);
        viderLeCache();

        AppUser cree = userService.createUser(dto(unique("niveau"), petitEnfant.getId()));

        assertThat(magasinEnBase(cree)).isEqualTo(petitEnfant.getId());
    }

    @Test
    @DisplayName("une autre officine, sans lien avec le créateur, est refusée")
    void horsPorteeRefuse() {
        Magasin etrangere = magasin(TypeMagasin.OFFICINE, null);

        assertThatThrownBy(() -> userService.createUser(dto(unique("etranger"), etrangere.getId())))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("n'est pas rattaché");
    }

    @Test
    @DisplayName("un dépôt rattaché est refusé")
    void depotRefuse() {
        Magasin depot = magasin(TypeMagasin.DEPOT, magasin);

        assertThatThrownBy(() -> userService.createUser(dto(unique("depot"), depot.getId())))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("ni caisse ni saisie");
    }

    @Test
    @DisplayName("un magasin absent de la base est refusé")
    void magasinInconnu() {
        assertThatThrownBy(() -> userService.createUser(dto(unique("inconnu"), 987_654)))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("inconnu");
    }

    // ===== réaffectation =====

    @Test
    @DisplayName("sans magasin demandé, la modification ne déplace pas le compte")
    void modificationSansMagasin() {
        Magasin parapharmacie = magasin(TypeMagasin.UNITE_GESTION, magasin);
        AppUser caissier = compte(parapharmacie);

        userService.updateUser(dtoDeModification(caissier, null));

        assertThat(magasinEnBase(caissier)).isEqualTo(parapharmacie.getId());
    }

    @Test
    @DisplayName("une réaffectation dans la portée est écrite en base")
    void reaffectationEcrite() {
        Magasin parapharmacie = magasin(TypeMagasin.UNITE_GESTION, magasin);
        AppUser caissier = compte(magasin);

        userService.updateUser(dtoDeModification(caissier, parapharmacie.getId()));

        assertThat(magasinEnBase(caissier)).isEqualTo(parapharmacie.getId());
    }

    /** La requête dérivée {@code existsByUserIdAndStatut} s'exécute ici contre la vraie table. */
    @Test
    @DisplayName("une caisse ouverte bloque la réaffectation et le compte ne bouge pas")
    void caisseOuverteBloque() {
        Magasin parapharmacie = magasin(TypeMagasin.UNITE_GESTION, magasin);
        AppUser caissier = compte(magasin);
        caisseDe(caissier, CashRegisterStatut.OPEN);

        assertThatThrownBy(() -> userService.updateUser(dtoDeModification(caissier, parapharmacie.getId())))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("billetage");
        assertThat(magasinEnBase(caissier)).isEqualTo(MAGASIN_ID);
    }

    @Test
    @DisplayName("une caisse fermée n'empêche pas la réaffectation")
    void caisseFermeeNEmpechePas() {
        Magasin parapharmacie = magasin(TypeMagasin.UNITE_GESTION, magasin);
        AppUser caissier = compte(magasin);
        caisseDe(caissier, CashRegisterStatut.CLOSED);

        userService.updateUser(dtoDeModification(caissier, parapharmacie.getId()));

        assertThat(magasinEnBase(caissier)).isEqualTo(parapharmacie.getId());
    }

    /** La caisse ouverte d'un AUTRE utilisateur ne concerne pas celui qu'on déplace. */
    @Test
    @DisplayName("la caisse ouverte d'un collègue ne bloque pas la réaffectation")
    void caisseDUnCollegueNeBloquePas() {
        Magasin parapharmacie = magasin(TypeMagasin.UNITE_GESTION, magasin);
        AppUser caissier = compte(magasin);
        caisseDe(compte(magasin), CashRegisterStatut.OPEN);

        userService.updateUser(dtoDeModification(caissier, parapharmacie.getId()));

        assertThat(magasinEnBase(caissier)).isEqualTo(parapharmacie.getId());
    }

    // ===== jeu d'essai =====

    private Magasin magasin(TypeMagasin type, Magasin parent) {
        String nom = unique("MAG-AFFECT");
        Magasin m = new Magasin();
        m.setName(nom);
        m.setFullName(nom + " COMPLET");
        m.setTypeMagasin(type);
        m.setParent(parent);
        em.persist(m);
        em.flush();
        return m;
    }

    private AppUser compte(Magasin affectation) {
        AppUser user = new AppUser();
        user.setLogin(unique("caissier").toLowerCase());
        user.setPassword("x".repeat(60));
        user.setFirstName("Awa");
        user.setLastName("Kone");
        user.setActivated(true);
        user.setLangKey("fr");
        user.setMagasin(affectation);
        em.persist(user);
        em.flush();
        return user;
    }

    private void caisseDe(AppUser titulaire, CashRegisterStatut statut) {
        CashFund fond = new CashFund();
        fond.setUser(titulaire);
        fond.setAmount(0);
        fond.setCreated(LocalDateTime.now());
        fond.setUpdated(LocalDateTime.now());
        fond.setCashFundType(CashFundType.AUTO);
        fond.setStatut(CashFundStatut.VALIDETED);
        em.persist(fond);

        CashRegister caisse = new CashRegister();
        caisse.setCashFund(fond);
        caisse.setUser(titulaire);
        caisse.setMagasin(titulaire.getMagasin());
        caisse.setInitAmount(0L);
        caisse.setBeginTime(aujourdHui().atTime(8, 0));
        caisse.setEndTime(statut == CashRegisterStatut.OPEN ? null : aujourdHui().atTime(18, 0));
        caisse.setCreated(LocalDateTime.now());
        caisse.setUpdated(LocalDateTime.now());
        caisse.setStatut(statut);
        em.persist(caisse);
        em.flush();
    }

    private AdminUserDTO dto(String login, Integer magasinId) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setLogin(login);
        dto.setFirstName("Awa");
        dto.setLastName("Kone");
        dto.setMagasinId(magasinId);
        return dto;
    }

    private AdminUserDTO dtoDeModification(AppUser existant, Integer magasinId) {
        AdminUserDTO dto = dto(existant.getLogin(), magasinId);
        dto.setId(existant.getId());
        dto.setAuthorities(Set.of());
        return dto;
    }

    /** Le magasin tel que la base l'a reçu, et non tel que l'entité en mémoire le croit. */
    private Integer magasinEnBase(AppUser user) {
        viderLeCache();
        return ((Number) em
                .createNativeQuery("SELECT magasin_id FROM app_user WHERE id = :id")
                .setParameter("id", user.getId())
                .getSingleResult()).intValue();
    }
}
