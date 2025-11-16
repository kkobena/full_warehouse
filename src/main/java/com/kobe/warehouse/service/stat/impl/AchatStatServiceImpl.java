package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.stat.AchatStatService;
import java.time.LocalDate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class AchatStatServiceImpl implements AchatStatService {

    private final CommandeRepository commandeRepository;

    public AchatStatServiceImpl(CommandeRepository commandeRepository) {
        this.commandeRepository = commandeRepository;
    }

    @Override
    public AchatRecord getAchatPeriode(AchatRecordParamDTO achatRecordParam) {
        Pair<LocalDate, LocalDate> periode = buildPeriode(achatRecordParam);
        Specification<Commande> specification = Specification.where(commandeRepository.hasOrderStatut(OrderStatut.CLOSED));
        specification = specification.and(commandeRepository.between(periode.getLeft(), periode.getRight()));
        return commandeRepository.getAchatPeriode(specification);
    }
}
