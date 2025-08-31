package com.kobe.warehouse.repository;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link AppUser} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {
    Optional<AppUser> findOneByActivationKey(String activationKey);

    List<AppUser> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(LocalDateTime dateTime);

    Optional<AppUser> findOneByResetKey(String resetKey);

    Optional<AppUser> findOneByEmailIgnoreCase(String email);

    Optional<AppUser> findOneByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    Optional<AppUser> findOneWithAuthoritiesByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    Optional<AppUser> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    Page<AppUser> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    boolean existsByLoginEqualsAndPasswordEquals(String login, String password);

    Optional<AppUser> findOneByActionAuthorityKey(String actionAuthorityKey);

    default Specification<AppUser> findspecialisation() {
        return (root, query, cb) ->
            cb.and(
                cb.notEqual(root.get(AppUser_.login), Constants.SYSTEM),
                cb.notEqual(root.get(AppUser_.login), Constants.ANONYMOUS_USER),
                cb.notEqual(root.get(AppUser_.login), Constants.ANONYMOUS_USER_2),
                cb.isTrue(root.get(AppUser_.activated))
            );
    }
}
