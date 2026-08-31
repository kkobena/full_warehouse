package com.kobe.warehouse.repository;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.enumeration.AuthorityEnum;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link AppUser} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<AppUser, Integer>, JpaSpecificationExecutor<AppUser> {
    Optional<AppUser> findOneByActivationKey(String activationKey);

    Optional<AppUser> findOneByResetKey(String resetKey);

    Optional<AppUser> findOneByEmailIgnoreCase(String email);

    Optional<AppUser> findOneByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    Optional<AppUser> findOneWithAuthoritiesByLogin(String login);

    Optional<AppUser> findOneByActionAuthorityKey(String actionAuthorityKey);

    /**
     * Find all users with a specific authority (role).
     * Used for mobile push notifications targeting specific roles.
     *
     * @param authority The authority enum
     * @return List of users with the specified authority
     */
    @Query("SELECT u FROM AppUser u JOIN u.authorities a WHERE a.name = :authorityName AND u.activated = true")
    List<AppUser> findByAuthority(@Param("authorityName") AuthorityEnum authority);

    /**
     * Les comptes proposés aux écrans métier : ni comptes techniques, ni comptes désactivés.
     *
     * <p>C'est la liste des personnes à qui l'on peut attribuer une vente ou un mouvement —
     * un compte désactivé n'a rien à y faire.
     */
    default Specification<AppUser> findspecialisation() {
        return (root, query, cb) -> cb.and(sansComptesTechniques(root, cb), cb.isTrue(root.get(AppUser_.activated)));
    }

    /**
     * Les comptes vus par l'ADMINISTRATION : les désactivés compris.
     *
     * <p>La liste d'administration filtrait elle aussi sur `activated` : un compte désactivé
     * disparaissait donc de l'écran qui sert à le gérer, et son bouton « Activer » — pourtant
     * présent dans le gabarit — ne pouvait jamais être atteint. Désactiver un compte revenait
     * à le perdre.
     */
    default Specification<AppUser> findAdminSpecialisation() {
        return (root, query, cb) -> sansComptesTechniques(root, cb);
    }

    private static jakarta.persistence.criteria.Predicate sansComptesTechniques(
        jakarta.persistence.criteria.Root<AppUser> root,
        jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        return cb.and(
            cb.notEqual(root.get(AppUser_.login), Constants.SYSTEM),
            cb.notEqual(root.get(AppUser_.login), Constants.ANONYMOUS_USER),
            cb.notEqual(root.get(AppUser_.login), Constants.ANONYMOUS_USER_2)
        );
    }
}
