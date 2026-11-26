package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.repository.AuthorityRepository;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.PersistentTokenRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.RandomUtil;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.AdminUserDTO;
import com.kobe.warehouse.service.dto.AuthParams;
import com.kobe.warehouse.service.dto.UserDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    private static final int PROFONDEUR_MAX = 2;

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final PersistentTokenRepository persistentTokenRepository;

    private final AuthorityRepository authorityRepository;

    private final MagasinRepository magasinRepository;
    private final CashRegisterRepository cashRegisterRepository;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PersistentTokenRepository persistentTokenRepository,
            AuthorityRepository authorityRepository,
            MagasinRepository magasinRepository,
            CashRegisterRepository cashRegisterRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.persistentTokenRepository = persistentTokenRepository;
        this.authorityRepository = authorityRepository;
        this.magasinRepository = magasinRepository;
        this.cashRegisterRepository = cashRegisterRepository;
    }

    public Optional<AppUser> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository.findOneByActivationKey(key).map(user -> {
            // activate given user for the registration key.
            user.setActivated(true);
            user.setActivationKey(null);
            log.debug("Activated user: {}", user);
            return user;
        });
    }

    public Optional<AppUser> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);
        return userRepository
                .findOneByResetKey(key)
                .filter(user -> user.getResetDate().isAfter(LocalDateTime.now().minusDays(1)))
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setResetKey(null);
                    user.setResetDate(null);
                    return user;
                });
    }

    public Optional<AppUser> requestPasswordReset(String mail) {
        return userRepository
                .findOneByEmailIgnoreCase(mail)
                .filter(AppUser::isActivated)
                .map(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(LocalDateTime.now());
                    return user;
                });
    }

    /**
     * Magasin où affecter le nouvel utilisateur.
     *
     * <p>Sans magasin demandé, on reprend celui du créateur : c'est le comportement historique, et
     * il reste juste dans une installation mono-officine. Mais il interdisait de créer quiconque
     * ailleurs — une parapharmacie pouvait donc exister sans que personne puisse y travailler.
     *
     * <p>Un magasin demandé est accepté s'il est <b>dans la portée du créateur</b> : le sien, ou
     * l'un de ses rattachés. Sans ce contrôle, n'importe qui pourrait affecter un utilisateur à un
     * magasin qui ne le concerne pas. La remontée est bornée plutôt que supposée acyclique, un
     * parent mal posé à la main ne devant pas faire tourner le serveur.
     *
     * <p><b>Jamais un dépôt.</b> Il n'y a rien à y saisir : les ventes au dépôt se font à
     * l'officine et ses reventes entrent par import. Un compte affecté là y ouvrirait des sessions
     * de caisse dans un établissement qui n'a pas de caisse. Le contrôle porte sur le magasin
     * <i>retenu</i>, quelle que soit son origine — sinon l'héritage serait une faille.
     */
    private Magasin resoudreLeMagasinDAffectation(AdminUserDTO userDTO, AppUser createur) {
        return refuserUnDepot(choisirLeMagasin(userDTO, createur));
    }

    /** Refus METIER, pas defaut du logiciel : l'exception doit arriver en 400 avec son message. */
    private Magasin refuserUnDepot(Magasin magasin) {
        if (magasin.getTypeMagasin() == TypeMagasin.DEPOT) {
            throw new BadRequestAlertException(
                    "Aucun utilisateur ne s'affecte à un dépôt : " + magasin.getName() + " n'a ni caisse ni saisie",
                    "user",
                    "affectation.depot");
        }
        return magasin;
    }

    private Magasin choisirLeMagasin(AdminUserDTO userDTO, AppUser createur) {
        Integer demande = userDTO.getMagasinId();
        Magasin duCreateur = createur.getMagasin();
        if (demande == null || demande.equals(duCreateur.getId())) {
            return duCreateur;
        }
        Magasin cible = magasinRepository
                .findById(demande)
                .orElseThrow(() -> new BadRequestAlertException(
                        "Magasin " + demande + " inconnu", "user", "affectation.magasin.inconnu"));
        Magasin remontee = cible.getParent();
        for (int profondeur = 0; remontee != null && profondeur < PROFONDEUR_MAX; profondeur++) {
            if (remontee.getId().equals(duCreateur.getId())) {
                return cible;
            }
            remontee = remontee.getParent();
        }
        throw new BadRequestAlertException(
                "Le magasin " + cible.getName() + " n'est pas rattaché à " + duCreateur.getName(),
                "user",
                "affectation.hors.portee");
    }

    private AppUser utilisateurCourant() {
        return userRepository
                .findOneByLogin(SecurityUtils.getCurrentUserLogin().orElseThrow())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    /**
     * Magasin après une demande de réaffectation.
     *
     * <p>Le magasin absent ou inchangé ne touche à rien : un écran qui n'envoie pas le champ — la
     * modification de son propre profil, par exemple — ne doit pas déplacer qui que ce soit.
     *
     * <p>Un changement réel passe par les mêmes contrôles qu'à la création : portée du demandeur,
     * et jamais un dépôt. Il s'y ajoute le refus d'une <b>caisse ouverte</b>, qui découle d'une
     * règle déjà posée — une caisse ne se réaffecte pas. Déplacer son titulaire laisserait une
     * caisse ouverte dans le magasin qu'il vient de quitter, encaissant sous une identité qui n'y
     * travaille plus. Le caissier fait son billetage, puis il change de magasin.
     *
     * <p>Ce qu'il a déjà fait ne bouge pas : ses caisses et ses commandes passées portent le
     * magasin d'alors, et c'est tout l'intérêt de l'estampille figée — l'historique dit où la
     * recette a été faite, pas où son auteur travaille aujourd'hui.
     */
    private Magasin resoudreLaReaffectation(AdminUserDTO userDTO, AppUser utilisateur) {
        Magasin actuel = utilisateur.getMagasin();
        Integer demande = userDTO.getMagasinId();
        if (demande == null || (actuel != null && demande.equals(actuel.getId()))) {
            return actuel;
        }
        refuserUneCaisseOuverte(utilisateur);
        return resoudreLeMagasinDAffectation(userDTO, utilisateurCourant());
    }

    private void refuserUneCaisseOuverte(AppUser utilisateur) {
        if (cashRegisterRepository.existsByUserIdAndStatut(utilisateur.getId(), CashRegisterStatut.OPEN)) {
            throw new BadRequestAlertException(
                    "Caisse ouverte : " + utilisateur.getLogin()
                            + " doit faire son billetage avant de changer de magasin",
                    "user",
                    "affectation.caisse.ouverte");
        }
    }

    public AppUser createUser(AdminUserDTO userDTO) {
        AppUser user = new AppUser();
        user.setLogin(userDTO.getLogin().toLowerCase());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        if (StringUtils.hasText(userDTO.getEmail())) {
            user.setEmail(userDTO.getEmail().toLowerCase());
        }
        user.setMagasin(resoudreLeMagasinDAffectation(userDTO, utilisateurCourant()));
        user.setImageUrl(userDTO.getImageUrl());
        user.setLangKey(Constants.DEFAULT_LANGUAGE);
        String encryptedPassword = passwordEncoder.encode(userDTO.getLogin().toLowerCase());
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(LocalDateTime.now());
        user.setActivated(true);
        if (userDTO.getAuthorities() != null) {
            Set<Authority> authorities = userDTO.getAuthorities().stream()
                    .map(authorityRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        if (StringUtils.hasText(userDTO.getActionAuthorityKey())) {
            user.setActionAuthorityKey(passwordEncoder.encode(userDTO.getActionAuthorityKey()));
        }
        userRepository.save(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update.
     * @return updated user.
     */
    public Optional<AdminUserDTO> updateUser(AdminUserDTO userDTO) {
        return Optional.of(userRepository.findById(userDTO.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(user -> {
                    user.setLogin(userDTO.getLogin().toLowerCase());
                    user.setFirstName(userDTO.getFirstName());
                    user.setLastName(userDTO.getLastName());
                    if (userDTO.getEmail() != null) {
                        user.setEmail(userDTO.getEmail().toLowerCase());
                    }
                    user.setImageUrl(userDTO.getImageUrl());
                    user.setMagasin(resoudreLaReaffectation(userDTO, user));
                    if (userDTO.isActivated() != null) {
                        user.setActivated(userDTO.isActivated());
                    }
                    Set<Authority> managedAuthorities = user.getAuthorities();
                    managedAuthorities.clear();
                    userDTO.getAuthorities().stream()
                            .map(authorityRepository::findById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .forEach(managedAuthorities::add);
                    return userRepository.save(user);
                })
                .map(AdminUserDTO::new);
    }

    public void deleteUser(String login) {
        userRepository.findOneByLogin(login).ifPresent(user -> {
            userRepository.delete(user);
            log.debug("Deleted User: {}", user);
        });
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName first name of user.
     * @param lastName  last name of user.
     * @param email     email id of user.
     * @param langKey   language key.
     * @param imageUrl  image URL of user.
     */
    public void updateUser(String firstName, String lastName, String email, String langKey, String imageUrl) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .ifPresent(user -> {
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    if (email != null) {
                        user.setEmail(email.toLowerCase());
                    }
                    user.setLangKey(langKey);
                    user.setImageUrl(imageUrl);
                    log.debug("Changed Information for User: {}", user);
                });
    }

    @Transactional
    public void changePassword(String currentClearTextPassword, String newPassword) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .ifPresent(user -> {
                    String currentEncryptedPassword = user.getPassword();
                    if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                        throw new BadRequestAlertException(
                                "Le mot de passe n'est pas correct", "user", "invalidpassword");
                    }
                    String encryptedPassword = passwordEncoder.encode(newPassword);
                    user.setPassword(encryptedPassword);
                    log.debug("Changed password for User: {}", user);
                });
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getAllManagedUsers(Pageable pageable) {
        // Spécification d'ADMINISTRATION : elle inclut les comptes désactivés, seul moyen de
        // les retrouver — et de les réactiver.
        return userRepository
                .findAll(userRepository.findAdminSpecialisation(), pageable)
                .map(AdminUserDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllPublicUsers(Pageable pageable) {
        return userRepository
                .findAll(userRepository.findspecialisation(), pageable)
                .map(UserDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
    }

    /**
     * Persistent Token are used for providing automatic authentication, they should be
     * automatically deleted after 30 days.
     *
     * <p>This is scheduled to get fired everyday, at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void removeOldPersistentTokens() {
        LocalDate now = LocalDate.now();
        persistentTokenRepository.findByTokenDateBefore(now.minusMonths(1)).forEach(token -> {
            log.debug("Deleting token {}", token.getSeries());
            AppUser user = token.getUser();
            user.getPersistentTokens().remove(token);
            persistentTokenRepository.delete(token);
        });
    }

    /**
     * Gets a list of all the authorities.
     *
     * @return a list of all the authorities.
     */
    @Transactional(readOnly = true)
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).toList();
    }

    @Cacheable(
            value = EntityConstant.CURRENT_USER_CACHE,
            key = "T(com.kobe.warehouse.security.SecurityUtils).getCurrentUserLogin().orElse('anonymous')")
    public AppUser getUser() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<AdminUserDTO> getUserConnectedWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneWithAuthoritiesByLogin)
                .map(user -> new AdminUserDTO(
                        user,
                        user.getAuthorities().stream()
                                .map(authority -> authorityRepository.findOneByName(authority.getName()))
                                .collect(Collectors.toSet())));
    }

    public Optional<AppUser> getUserByPwdOrSecurityKey(String input) {
        return userRepository.findOneByActionAuthorityKey(DigestUtils.sha256Hex(input));
    }

    @Transactional(readOnly = true)
    public Optional<AdminUserDTO> getUsers(AuthParams authParams) {
        return userRepository
                .findOneWithAuthoritiesByLogin(authParams.username())
                .filter(e -> passwordEncoder.matches(authParams.password(), e.getPassword()))
                .map(user -> new AdminUserDTO(
                        user,
                        user.getAuthorities().stream()
                                .map(authority -> authorityRepository.findOneByName(authority.getName()))
                                .collect(Collectors.toSet())))
                .filter(e -> e.getAuthorities().stream().anyMatch(SecurityUtils::hasMobileAccess));
    }
}
