package com.kobe.warehouse.service.referential.impl;

import com.kobe.warehouse.domain.Tableau;
import com.kobe.warehouse.repository.TableauRepository;
import com.kobe.warehouse.service.dto.TableauDTO;
import com.kobe.warehouse.service.referential.TableauService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TableauServiceImpl implements TableauService {
  private final TableauRepository tableauRepository;

  public TableauServiceImpl(TableauRepository tableauRepository) {
    this.tableauRepository = tableauRepository;
  }

  @Override
  @Transactional
  public TableauDTO save(TableauDTO tableauDTO) {
    return Optional.of(
            this.tableauRepository.saveAndFlush(
                new Tableau()
                    .setId(tableauDTO.getId())
                    .setCode(tableauDTO.getCode())
                    .setValue(tableauDTO.getValue())))
        .map(TableauDTO::new)
        .orElseThrow();
  }

  @Override
  public List<TableauDTO> findAll() {
    return this.tableauRepository.findAll().stream().map(TableauDTO::new).toList();
  }

  @Override
  public Optional<TableauDTO> findOne(Long id) {
    return this.tableauRepository.findById(id).map(TableauDTO::new);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    this.tableauRepository.deleteById(id);
  }
}
