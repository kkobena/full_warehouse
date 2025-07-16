package com.kobe.warehouse.service.referential.magasin;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.dto.MagasinDTO;
import java.util.List;

public interface MagasinService {
    Magasin save(Magasin magasin);

    MagasinDTO currentUserMagasin();

    MagasinDTO findById(Long id);

    void delete(Long id);

    List<MagasinDTO> findAll();
}
