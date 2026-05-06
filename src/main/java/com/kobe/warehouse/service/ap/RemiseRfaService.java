package com.kobe.warehouse.service.ap;

import com.kobe.warehouse.service.dto.AvoirFournisseurRfaDTO;
import com.kobe.warehouse.service.dto.RemiseRfaFournisseurDTO;
import java.util.List;

public interface RemiseRfaService {

    List<RemiseRfaFournisseurDTO> getRfaFournisseurs();

    List<AvoirFournisseurRfaDTO> getAvoirsFournisseurs();
}
