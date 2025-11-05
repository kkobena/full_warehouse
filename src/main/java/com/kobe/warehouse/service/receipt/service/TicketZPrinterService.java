package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketZPrinterService extends AbstractTicketZService {


    protected TicketZPrinterService(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }


    @Override
    protected List<HeaderFooterItem> getFooterItems() {
        return List.of();
    }

    @Override
    protected List<? extends AbstractItem> getItems() {
        return List.of();
    }


}
