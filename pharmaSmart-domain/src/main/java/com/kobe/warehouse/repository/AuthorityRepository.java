package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Authority;
import java.util.List;

import com.kobe.warehouse.service.dto.records.CodeLibelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for the {@link Authority} entity. */
@Repository
public interface AuthorityRepository extends JpaRepository<Authority, String> {
    Authority findOneByName(String name);

    List<Authority> findAll();
    @Query("SELECT o.name AS code,o.libelle AS libelle FROM  Authority  o")
    List<CodeLibelle> findNameAndLibelle();


}
