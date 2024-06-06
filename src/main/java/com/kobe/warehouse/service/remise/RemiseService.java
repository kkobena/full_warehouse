package com.kobe.warehouse.service.remise;

import com.kobe.warehouse.service.dto.RemiseDTO;
import java.util.List;
import java.util.Optional;

public interface RemiseService {
  RemiseDTO save(RemiseDTO remiseDTO);

  RemiseDTO changeStatus(RemiseDTO remiseDTO);

  Optional<RemiseDTO> findOne(Long id);

  void delete(Long id);

  List<RemiseDTO> findAll();

  void associer(Long id, List<Long> produitIds);

  void dissocier(List<Long> produitIds);
}
