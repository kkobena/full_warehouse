package com.kobe.warehouse.service.referential.magasin;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.MagasinDTO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MagasinServiceImpl implements MagasinService {

    private final MagasinRepository magasinRepository;
    private final UserService userService;

    public MagasinServiceImpl(MagasinRepository magasinRepository, UserService userService) {
        this.magasinRepository = magasinRepository;
        this.userService = userService;
    }

    @Override
    public Magasin save(Magasin magasin) {
        return magasinRepository.save(magasin);
    }

    @Override
    public MagasinDTO currentUserMagasin() {
        return new MagasinDTO(this.userService.getUser().getMagasin());
    }

    @Override
    public MagasinDTO findById(Long id) {
        return magasinRepository.findById(id).map(MagasinDTO::new).orElse(null);
    }

    @Override
    public void delete(Long id) {
        magasinRepository.deleteById(id);
    }

    @Override
    public List<MagasinDTO> findAll() {
        return magasinRepository.findAll().stream().map(MagasinDTO::new).toList();
    }
}
