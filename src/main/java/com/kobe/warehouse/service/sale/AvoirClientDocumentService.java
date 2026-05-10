package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.service.sale.dto.AvoirClientDocumentDTO;
import com.kobe.warehouse.service.sale.dto.CloturerAvoirRequest;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AvoirClientDocumentService {


    void createAvoirsFromSale(SalesLine salesLine, Customer customer);
    void cancelAvoirsFromSale(Long salesLineId);

    void linkCommandeToAvoirs(Commande commande);

    AvoirClientDocumentDTO cloturerAvoir(Integer avoirId, CloturerAvoirRequest request);

    Page<AvoirClientDocumentDTO> findAll(
        String search, LocalDate fromDate, LocalDate toDate,
        AvoirClientStatut statut, Pageable pageable
    );
}
