package com.kobe.warehouse.service.remise;

import com.kobe.warehouse.service.dto.RemiseDTO;
import com.kobe.warehouse.service.dto.TypeRemise;
import java.util.List;
import java.util.Optional;

public interface RemiseService {
    RemiseDTO save(RemiseDTO remiseDTO);

    RemiseDTO changeStatus(RemiseDTO remiseDTO);

    Optional<RemiseDTO> findOne(Long id);

    void delete(Long id);

    List<RemiseDTO> findAll(TypeRemise typeRemise);
}
