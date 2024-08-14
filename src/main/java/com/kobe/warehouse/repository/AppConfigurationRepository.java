package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppConfiguration;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppConfigurationRepository extends JpaRepository<AppConfiguration, String> {
    List<AppConfiguration> findAllByNameOrDescriptionContainingAllIgnoreCase(String name, String description, Sort sort);
}
