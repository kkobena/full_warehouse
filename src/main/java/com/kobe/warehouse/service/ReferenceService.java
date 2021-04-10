package com.kobe.warehouse.service;

import java.time.LocalDate;
import java.util.Optional;



import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Reference;
import com.kobe.warehouse.repository.ReferenceRepository;

@Service
@Transactional
public class ReferenceService {
	private final ReferenceRepository referenceRepository;

	public ReferenceService(ReferenceRepository referenceRepository) {

		this.referenceRepository = referenceRepository;
	}

	public String buildNumCommande() {
		Optional<Reference> op = referenceRepository.findOneBymvtDateAndType(LocalDate.now(),
				Constants.REFERENCE_TYPE_COMMANDE);
		if (op.isPresent()) {
			Reference reference = op.get();
			reference.setNumberTransac(reference.getNumberTransac() + 1);
			reference.setNum(StringUtils.leftPad(String.valueOf(reference.getNumberTransac()), 3, '0'));
			referenceRepository.save(reference);
			return reference.getNum();

		}else {
			Reference reference =new Reference();
			reference.setType(Constants.REFERENCE_TYPE_COMMANDE);
			reference.setMvtDate(LocalDate.now());
			reference.setNumberTransac(1);
			reference.setNum(StringUtils.leftPad(String.valueOf(reference.getNumberTransac()), 3, '0'));
			referenceRepository.save(reference);
			return reference.getNum();
		}
	}
	public String buildNumSale() {
		Optional<Reference> op = referenceRepository.findOneBymvtDateAndType(LocalDate.now(),
				Constants.REFERENCE_TYPE_VENTE);
		if (op.isPresent()) {
			Reference reference = op.get();
			reference.setNumberTransac(reference.getNumberTransac() + 1);
			reference.setNum(StringUtils.leftPad(String.valueOf(reference.getNumberTransac()), 3, '0'));
			referenceRepository.save(reference);
			return reference.getNum();

		}else {
			Reference reference =new Reference();
			reference.setType(Constants.REFERENCE_TYPE_VENTE);
			reference.setMvtDate(LocalDate.now());
			reference.setNumberTransac(1);
			reference.setNum(StringUtils.leftPad(String.valueOf(reference.getNumberTransac()), 3, '0'));
			referenceRepository.save(reference);
			return reference.getNum();
		}
	}
}
