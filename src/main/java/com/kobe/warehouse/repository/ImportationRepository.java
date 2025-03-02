package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Importation;
import com.kobe.warehouse.domain.enumeration.ImportationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportationRepository extends JpaRepository<Importation, Long> {
    Importation findFirstByImportationTypeOrderByCreatedDesc(ImportationType importationType);
}
