package com.kobe.warehouse.service.activity_summary;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.*;
import com.kobe.warehouse.service.dto.ChiffreAffaireDTO;
import com.kobe.warehouse.service.dto.projection.*;
import com.kobe.warehouse.service.dto.records.ChiffreAffaireRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActivitySummaryServiceImpl implements ActivitySummaryService {

    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final DeliveryReceiptRepository deliveryReceiptRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final SalesRepository salesRepository;

    public ActivitySummaryServiceImpl(
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        DeliveryReceiptRepository deliveryReceiptRepository,
        PaymentTransactionRepository paymentTransactionRepository,
        PaymentRepository paymentRepository,
        SalesRepository salesRepository
    ) {
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.deliveryReceiptRepository = deliveryReceiptRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.salesRepository = salesRepository;
    }

    @Override
    public ChiffreAffaireDTO getChiffreAffaire(LocalDate fromDate, LocalDate toDate) {
        ChiffreAffaire chiffreAffaire = this.salesRepository.getChiffreAffaire(fromDate, toDate);
        List<Recette> recettes = this.findRecettes(fromDate, toDate);
        ChiffreAffaireAchat achats = this.fetchAchats(fromDate, toDate);
        List<MouvementCaisse> mvts = this.findMouvementsCaisse(fromDate, toDate);
        BigDecimal montantEspece = BigDecimal.ZERO;
        BigDecimal montantRegle = BigDecimal.ZERO;
        BigDecimal montantAutreModePaiement = BigDecimal.ZERO;
        for (Recette recette : recettes) {
            montantRegle = montantRegle.add(recette.getMontantReel());
            if (recette.getModePaimentCode() == ModePaimentCode.CASH) {
                montantEspece = montantEspece.add(recette.getMontantReel());
            } else {
                montantAutreModePaiement = montantAutreModePaiement.add(recette.getMontantReel());
            }
        }
        var caRecord = new ChiffreAffaireRecord(
            chiffreAffaire.getMontantTtc(),
            chiffreAffaire.getMontantTva(),
            chiffreAffaire.getMontantHt(),
            chiffreAffaire.getMontantRemise(),
            chiffreAffaire.getMontantNet(),
            montantEspece,
            chiffreAffaire.getMontantCredit(),
            montantRegle,
            montantAutreModePaiement,
            chiffreAffaire.getMarge()
        );
        return new ChiffreAffaireDTO(recettes, caRecord, achats, mvts);
    }

    @Override
    public List<Recette> findRecettes(LocalDate fromDate, LocalDate toDate) {
        return this.paymentRepository.findRecettes(fromDate, toDate);
    }

    @Override
    public Page<GroupeFournisseurAchat> fetchAchats(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return this.deliveryReceiptRepository.fetchAchats(fromDate, toDate, ReceiptStatut.CLOSE, pageable);
    }

    private ChiffreAffaireAchat fetchAchats(LocalDate fromDate, LocalDate toDate) {
        return this.deliveryReceiptRepository.fetchAchats(fromDate, toDate, ReceiptStatut.CLOSE);
    }

    @Override
    public List<MouvementCaisse> findMouvementsCaisse(LocalDate fromDate, LocalDate toDate) {
        return this.paymentTransactionRepository.findMouvementsCaisse(fromDate, toDate);
    }

    @Override
    public Page<AchatTiersPayant> fetchAchatTiersPayant(LocalDate fromDate, LocalDate toDate, String search, Pageable pageable) {
        return this.thirdPartySaleLineRepository.fetchAchatTiersPayant(fromDate, toDate, search, SalesStatut.CLOSED, pageable);
    }

    @Override
    public Page<ReglementTiersPayants> findReglementTierspayant(LocalDate fromDate, LocalDate toDate, String search, Pageable pageable) {
        return this.invoicePaymentRepository.findReglementTierspayant(fromDate, toDate, search, pageable);
    }
}
