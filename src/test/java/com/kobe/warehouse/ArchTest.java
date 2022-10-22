package com.kobe.warehouse;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchTest {

    @Test
    void servicesAndRepositoriesShouldNotDependOnWebLayer() {

        JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.kobe.warehouse");

        noClasses()
            .that()
                .resideInAnyPackage("com.kobe.warehouse.service..")
            .or()
                .resideInAnyPackage("com.kobe.warehouse.repository..")
            .should().dependOnClassesThat()
                .resideInAnyPackage("..com.kobe.warehouse.web..")
        .because("Services and repositories should not depend on web layer")
        .check(importedClasses);
    }
}
