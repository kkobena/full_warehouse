package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppConfiguration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppConfigurationRepository  extends JpaRepository<AppConfiguration, String> {
}
