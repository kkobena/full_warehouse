package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AuthorityPrivilege;
import com.kobe.warehouse.service.dto.ActionPrivilegeDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityPrivilegeRepository extends JpaRepository<AuthorityPrivilege, Long> {
    @Query(
        "select distinct new com.kobe.warehouse.service.dto.ActionPrivilegeDTO(p.privilege.name,p.privilege.libelle) from AuthorityPrivilege p WHERE p.authority.name=:authorityName order by p.privilege.libelle"
    )
    List<ActionPrivilegeDTO> findAllAuthorityName(@Param("authorityName") String authorityName);

    List<AuthorityPrivilege> findAllAuthorityPrivilegeByAuthorityName(String authorityName);

    Optional<AuthorityPrivilege> findOneByPrivilegeNameAndAuthorityName(String privilegeName, String authorityName);

    boolean existsByPrivilegeNameAndAuthorityName(String privilegeName, String authorityName);
}
