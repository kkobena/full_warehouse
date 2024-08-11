package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Privilege;
import com.kobe.warehouse.service.dto.ActionPrivilegeDTO;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, String> {
    @Query("select new com.kobe.warehouse.service.dto.ActionPrivilegeDTO(p.name,p.libelle) from Privilege p order by p.libelle")
    List<ActionPrivilegeDTO> findAllPrivilege();
}
