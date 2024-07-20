package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Privilege;
import com.kobe.warehouse.service.dto.PrivilegeDTO;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, String> {
  @Query(
      "select new com.kobe.warehouse.service.dto.PrivilegeDTO(p.name,p.libelle) from Privilege p where p.menu.id = :menuId and p.authority.name = :authorityName")
  Set<PrivilegeDTO> findByMenuIdAndAuthorityName(
      @Param("menuId") Long menuId, @Param("authorityName") String authorityName);
}
