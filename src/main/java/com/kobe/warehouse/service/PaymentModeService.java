package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.repository.PaymentModeRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class PaymentModeService {

    private final PaymentModeRepository paymentModeRepository;

    public PaymentModeService(PaymentModeRepository paymentModeRepository) {
        this.paymentModeRepository = paymentModeRepository;
    }

    @Cacheable(EntityConstant.APP_MODE_PAYMENTS)
    public List<PaymentMode> fetch() {
        return this.paymentModeRepository.findAllByEnableTrue().stream().sorted(Comparator.comparing(PaymentMode::getOrder)).toList();
    }
}
