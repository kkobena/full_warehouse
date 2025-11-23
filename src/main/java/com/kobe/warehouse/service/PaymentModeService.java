package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.service.dto.PaymentModeUpdateDTO;
import com.kobe.warehouse.service.dto.projection.QrCodeResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class PaymentModeService {

    private final PaymentModeRepository paymentModeRepository;

    public PaymentModeService(PaymentModeRepository paymentModeRepository) {
        this.paymentModeRepository = paymentModeRepository;
    }

    @Cacheable(EntityConstant.APP_MODE_PAYMENTS)
    @Transactional(readOnly = true)
    public List<PaymentMode> fetchAll() {
        return this.paymentModeRepository.findAllByEnableTrue().stream().sorted(Comparator.comparing(PaymentMode::getOrder)).toList();
    }

    @Cacheable(EntityConstant.APP_MODE_PAYMENTS_SANS_CH_VIR)
    @Transactional(readOnly = true)
    public List<PaymentMode> fetch() {
        Set<String> excludedModes = Set.of("VIREMENT", "CH");
        return this.paymentModeRepository.findAllByEnableTrue().stream()
            .filter(paymentMode -> !excludedModes.contains(paymentMode.getCode()))
            .sorted(Comparator.comparing(PaymentMode::getOrder)).toList();
    }

    @Transactional(readOnly = true)
    public QrCodeResponse getPaymentQrCode(String code) {
        return this.paymentModeRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentMode> findByCode(String code) {
        return this.paymentModeRepository.findById(code);
    }

    /**
     * Update a payment mode with optional QR code.
     *
     * @param dto the payment mode data
     * @param qrCodeFile the QR code image file (optional)
     * @return the updated payment mode
     * @throws IllegalArgumentException if the payment mode doesn't exist
     */
    @CacheEvict(value = {EntityConstant.APP_MODE_PAYMENTS, EntityConstant.APP_MODE_PAYMENTS_SANS_CH_VIR}, allEntries = true)
    public PaymentMode update(PaymentModeUpdateDTO dto, MultipartFile qrCodeFile) throws IOException {
        PaymentMode paymentMode = this.paymentModeRepository.findById(dto.getCode())
            .orElseThrow(() -> new IllegalArgumentException("Payment mode not found with code: " + dto.getCode()));

        paymentMode.setLibelle(dto.getLibelle());
        paymentMode.setOrder(dto.getOrder());

        if (qrCodeFile != null && !qrCodeFile.isEmpty()) {
            paymentMode.setQrCode(qrCodeFile.getBytes());
        }

        return this.paymentModeRepository.save(paymentMode);
    }
}
