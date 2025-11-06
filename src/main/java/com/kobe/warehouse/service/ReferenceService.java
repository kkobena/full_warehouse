package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Reference;
import com.kobe.warehouse.repository.ReferenceRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReferenceService {

    private static final int REFERENCE_PADDING = 3;
    private static final char PADDING_CHAR = '0';
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReferenceRepository referenceRepository;

    public ReferenceService(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
    }

    public String buildNumCommande() {
        return buildReference(Constants.REFERENCE_TYPE_COMMANDE, this::formatWithDatePrefix);
    }
    public String buildNumTransaction() {
        return buildReference(Constants.REFERENCE_TYPE_TRANSACTION, this::formatWithDatePrefix);
    }

    public String buildNumSale() {
        return buildReference(Constants.REFERENCE_TYPE_VENTE, this::formatSimple);
    }

    public String buildNumPreventeSale() {
        return buildReference(Constants.REFERENCE_PREVENTE_VENTE, this::formatSimple);
    }

    public String buildSuggestionReference() {
        return buildReference(Constants.REFERENCE_TYPE_SUGGESTION, this::formatSimple);
    }

    /**
     * Generic method to build a reference number for a given type
     *
     * @param referenceType The type of reference (from Constants)
     * @param formatter Function to format the reference number
     * @return The generated reference number
     */
    private String buildReference(int referenceType, Function<Reference, String> formatter) {
        LocalDate currentDate = LocalDate.now();
        Optional<Reference> optionalReference = referenceRepository.findOneBymvtDateAndType(currentDate, referenceType);

        Reference reference = optionalReference
            .map(ref -> {
                ref.setNumberTransac(ref.getNumberTransac() + 1);
                return ref;
            })
            .orElseGet(() -> createNewReference(referenceType, currentDate));

        reference.setNum(formatter.apply(reference));
        referenceRepository.save(reference);
        return reference.getNum();
    }

    /**
     * Creates a new reference with initial values
     *
     * @param type The reference type
     * @param mvtDate The movement date
     * @return A new Reference instance
     */
    private Reference createNewReference(int type, LocalDate mvtDate) {
        Reference reference = new Reference();
        reference.setType(type);
        reference.setMvtDate(mvtDate);
        reference.setNumberTransac(1);
        return reference;
    }

    /**
     * Formats reference with date prefix (yyyyMMdd + padded number)
     */
    private String formatWithDatePrefix(Reference reference) {
        String datePrefix = reference.getMvtDate().format(DATE_FORMATTER);
        String paddedNumber = padTransactionNumber(reference.getNumberTransac());
        return datePrefix.concat(paddedNumber);
    }

    /**
     * Formats reference as simple padded number
     */
    private String formatSimple(Reference reference) {
        return padTransactionNumber(reference.getNumberTransac());
    }

    /**
     * Pads the transaction number with zeros
     */
    private String padTransactionNumber(int transactionNumber) {
        return StringUtils.leftPad(String.valueOf(transactionNumber), REFERENCE_PADDING, PADDING_CHAR);
    }
}
