package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.service.dto.projection.QrCodeResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class PaymentModeService {

    private final PaymentModeRepository paymentModeRepository;

    public PaymentModeService(PaymentModeRepository paymentModeRepository) {
        this.paymentModeRepository = paymentModeRepository;
    }

    @Cacheable(EntityConstant.APP_MODE_PAYMENTS)
    public List<PaymentMode> fetchAll() {
        return this.paymentModeRepository.findAllByEnableTrue().stream().sorted(Comparator.comparing(PaymentMode::getOrder)).toList();
    }

    @Cacheable(EntityConstant.APP_MODE_PAYMENTS_SANS_CH_VIR)
    public List<PaymentMode> fetch() {
        Set<String> excludedModes = Set.of("VIREMENT", "CH");
        return this.paymentModeRepository.findAllByEnableTrue().stream()
            .filter(paymentMode -> !excludedModes.contains(paymentMode.getCode()))
            .sorted(Comparator.comparing(PaymentMode::getOrder)).toList();
    }

    public QrCodeResponse getPaymentQrCode(String code) {
        return this.paymentModeRepository.findByCode(code);

    }
}
