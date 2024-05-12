package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Poste;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosteRepository extends JpaRepository<Poste, Long> {
  Optional<Poste> findFirstByAddress(String address);
}
