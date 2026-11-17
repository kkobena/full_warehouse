package com.kobe.warehouse.service.referential.magasin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.repository.AuthorityRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.PersistentTokenRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.AdminUserDTO;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Un nouvel utilisateur héritait du magasin de celui qui le crée, sans qu'aucun écran ne permette de
 * choisir. Une parapharmacie pouvait donc exister sans que personne puisse y travailler : tous les
 * comptes naissaient à l'officine, et seul un compte de la parapharmacie aurait pu en créer un
 * autre. L'œuf et la poule.
 *
 * <p>Le magasin demandé est désormais accepté, mais seulement <b>dans la portée du créateur</b> :
 * le sien ou l'un de ses rattachés. Sans ce contrôle, un compte pourrait être affecté à un magasin
 * qui ne le concerne pas — le champ vient du client, il ne se croit pas sur parole.
 */
@DisplayName("Utilisateur — le magasin d'affectation")
class AffectationUtilisateurTest {

    private CashRegisterRepository cashRegisterRepository;
    private UserRepository userRepository;
    private MagasinRepository magasinRepository;
    private UserService service;

    @BeforeEach
    void preparer() {
        userRepository = mock(UserRepository.class);
        magasinRepository = mock(MagasinRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("chiffre");

        cashRegisterRepository = mock(CashRegisterRepository.class);
        service = new UserService(
            userRepository,
            passwordEncoder,
            mock(PersistentTokenRepository.class),
            mock(AuthorityRepository.class),
            magasinRepository,
            cashRegisterRepository
        );

        SecurityContextHolder.setContext(
            new SecurityContextImpl(new UsernamePasswordAuthenticationToken("titulaire", "x"))
        );
        when(userRepository.save(any(AppUser.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    /**
     * {@code SecurityContextHolder} est un {@code ThreadLocal} et Surefire réutilise le fil : sans
     * ce nettoyage, les classes suivantes héritent de l'authentification posée ici. Oublié une
     * première fois, cela faisait tomber {@code SaleServiceImplTest} sur un tout autre sujet.
     */
    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sans magasin demandé, on reprend celui du créateur")
    void sansDemandeOnHerite() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));

        AppUser cree = service.createUser(dto(null));

        assertThat(cree.getMagasin().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("une parapharmacie rattachée à l'officine est un choix valide")
    void unRattacheEstAccepte() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        Magasin parapharmacie = magasin(7, TypeMagasin.UNITE_GESTION).setParent(officine);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));
        when(magasinRepository.findById(7)).thenReturn(Optional.of(parapharmacie));

        AppUser cree = service.createUser(dto(7));

        assertThat(cree.getMagasin().getId()).isEqualTo(7);
    }

    @Test
    @DisplayName("son propre magasin reste accepté quand il est demandé explicitement")
    void sonPropreMagasin() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));

        assertThat(service.createUser(dto(1)).getMagasin().getId()).isEqualTo(1);
    }

    /**
     * Le cas que le contrôle existe pour empêcher : un magasin qui n'est pas dans la portée du
     * créateur, demandé par le client.
     */
    @Test
    @DisplayName("un magasin hors de la portée du créateur est refusé")
    void horsPorteeRefuse() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        Magasin autre = magasin(9, TypeMagasin.OFFICINE);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));
        when(magasinRepository.findById(9)).thenReturn(Optional.of(autre));

        assertThatThrownBy(() -> service.createUser(dto(9)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("n'est pas rattaché");
    }

    @Test
    @DisplayName("un magasin inconnu est refusé")
    void magasinInconnu() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));
        when(magasinRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createUser(dto(404)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("inconnu");
    }

    /**
     * Un parent mal posé à la main ne doit pas faire tourner la remontée indéfiniment.
     */
    @Test
    @DisplayName("un cycle posé à la main ne fait pas tourner la remontée")
    void cycleBorne() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        Magasin a = magasin(20, TypeMagasin.UNITE_GESTION);
        Magasin b = magasin(21, TypeMagasin.UNITE_GESTION);
        a.setParent(b);
        b.setParent(a);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));
        when(magasinRepository.findById(20)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.createUser(dto(20))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un dépôt est refusé, même rattaché à l'officine du créateur")
    void unDepotEstRefuse() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        Magasin depot = magasin(5, TypeMagasin.DEPOT).setParent(officine);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));
        when(magasinRepository.findById(5)).thenReturn(Optional.of(depot));

        assertThatThrownBy(() -> service.createUser(dto(5)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ni caisse ni saisie");
    }

    /**
     * Le contrôle porte sur le magasin retenu, pas sur celui qui est demandé : sans cela, un
     * créateur lui-même affecté à un dépôt y ferait naître tous ses comptes sans rien demander.
     */
    @Test
    @DisplayName("l'héritage ne contourne pas la règle")
    void lHeritageNeContournePas() {
        Magasin depot = magasin(5, TypeMagasin.DEPOT);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(depot)));

        assertThatThrownBy(() -> service.createUser(dto(null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ni caisse ni saisie");
    }

    // ===== la réaffectation d'un compte existant =====

    /**
     * Le cas courant : un écran qui n'envoie pas le champ ne doit déplacer personne. C'est ce qui
     * protège la modification d'un profil, où le magasin n'a rien à faire.
     */
    @Test
    @DisplayName("sans magasin demandé, la modification ne déplace personne")
    void modificationSansMagasinNeDeplacePas() {
        AppUser existant = aReaffecter(magasin(7, TypeMagasin.UNITE_GESTION));

        assertThat(service.updateUser(dtoDeModification(existant, null)).orElseThrow().getMagasinId()).isEqualTo(7);
    }

    @Test
    @DisplayName("un magasin de la portée du demandeur réaffecte le compte")
    void reaffectationAcceptee() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        Magasin parapharmacie = magasin(7, TypeMagasin.UNITE_GESTION).setParent(officine);
        AppUser existant = aReaffecter(officine);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));
        when(magasinRepository.findById(7)).thenReturn(Optional.of(parapharmacie));

        assertThat(service.updateUser(dtoDeModification(existant, 7)).orElseThrow().getMagasinId()).isEqualTo(7);
    }

    /**
     * La réaffectation passe par les mêmes contrôles que la création : ce n'est pas une porte
     * dérobée vers un magasin hors portée, ni vers un dépôt.
     */
    @Test
    @DisplayName("la réaffectation n'ouvre pas ce que la création refuse")
    void reaffectationSoumiseAuxMemesRegles() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        Magasin depot = magasin(5, TypeMagasin.DEPOT).setParent(officine);
        Magasin etranger = magasin(9, TypeMagasin.OFFICINE);
        AppUser existant = aReaffecter(officine);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));
        when(magasinRepository.findById(5)).thenReturn(Optional.of(depot));
        when(magasinRepository.findById(9)).thenReturn(Optional.of(etranger));

        assertThatThrownBy(() -> service.updateUser(dtoDeModification(existant, 5)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ni caisse ni saisie");
        assertThatThrownBy(() -> service.updateUser(dtoDeModification(existant, 9)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("n'est pas rattaché");
    }

    /**
     * Découle d'une règle déjà posée : une caisse ne se réaffecte pas. Déplacer son titulaire
     * laisserait une caisse ouverte dans le magasin qu'il vient de quitter, encaissant sous une
     * identité qui n'y travaille plus.
     */
    @Test
    @DisplayName("un caissier dont la caisse est ouverte ne se réaffecte pas")
    void caisseOuverteBloqueLaReaffectation() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        Magasin parapharmacie = magasin(7, TypeMagasin.UNITE_GESTION).setParent(officine);
        AppUser existant = aReaffecter(officine);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));
        when(magasinRepository.findById(7)).thenReturn(Optional.of(parapharmacie));
        when(cashRegisterRepository.existsByUserIdAndStatut(42, CashRegisterStatut.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> service.updateUser(dtoDeModification(existant, 7)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("billetage");
    }

    /**
     * Le verrou ne doit pas enfermer : une caisse fermée n'empêche rien, sans quoi un caissier
     * resterait prisonnier de son magasin pour avoir encaissé une fois.
     */
    @Test
    @DisplayName("une caisse fermée n'empêche pas la réaffectation")
    void caisseFermeeNEmpechePas() {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        Magasin parapharmacie = magasin(7, TypeMagasin.UNITE_GESTION).setParent(officine);
        AppUser existant = aReaffecter(officine);
        when(userRepository.findOneByLogin("titulaire")).thenReturn(Optional.of(utilisateur(officine)));
        when(magasinRepository.findById(7)).thenReturn(Optional.of(parapharmacie));
        when(cashRegisterRepository.existsByUserIdAndStatut(42, CashRegisterStatut.OPEN)).thenReturn(false);

        assertThat(service.updateUser(dtoDeModification(existant, 7)).orElseThrow().getMagasinId()).isEqualTo(7);
    }

    // ===== jeu d'essai =====

    private AdminUserDTO dto(Integer magasinId) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setLogin("nouveau");
        dto.setFirstName("Awa");
        dto.setLastName("Kone");
        dto.setMagasinId(magasinId);
        return dto;
    }

    private AppUser utilisateur(Magasin magasin) {
        AppUser user = new AppUser();
        user.setId(1);
        user.setLogin("titulaire");
        user.setMagasin(magasin);
        return user;
    }

    /** Un compte existant, distinct du demandeur : c'est lui qu'on déplace. */
    private AppUser aReaffecter(Magasin magasin) {
        AppUser user = new AppUser();
        user.setId(42);
        user.setLogin("caissier");
        user.setMagasin(magasin);
        when(userRepository.findById(42)).thenReturn(Optional.of(user));
        return user;
    }

    private AdminUserDTO dtoDeModification(AppUser existant, Integer magasinId) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(existant.getId());
        dto.setLogin(existant.getLogin());
        dto.setFirstName("Awa");
        dto.setLastName("Kone");
        dto.setMagasinId(magasinId);
        dto.setAuthorities(java.util.Set.of());
        return dto;
    }

    private Magasin magasin(int id, TypeMagasin type) {
        Magasin magasin = new Magasin();
        magasin.setId(id);
        magasin.setName("MAGASIN " + id);
        magasin.setFullName("MAGASIN COMPLET " + id);
        magasin.setTypeMagasin(type);
        return magasin;
    }
}
