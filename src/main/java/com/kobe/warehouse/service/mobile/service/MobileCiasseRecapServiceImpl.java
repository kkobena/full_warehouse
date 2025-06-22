package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.mobile.dto.ListItem;
import com.kobe.warehouse.service.mobile.dto.RecapCaisse;
import com.kobe.warehouse.service.mobile.dto.UserCaisseRecap;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZData;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import com.kobe.warehouse.service.tiketz.dto.TicketZRecap;
import com.kobe.warehouse.service.tiketz.service.TicketZService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Service
public class MobileCiasseRecapServiceImpl implements MobileCiasseRecapService {
    private final TicketZService ticketZService;

    public MobileCiasseRecapServiceImpl(TicketZService ticketZService) {
        this.ticketZService = ticketZService;
    }

    @Override
    public RecapCaisse getRecapCaisse(TicketZParam param) {
        TicketZ ticket = ticketZService.getTicketZ(param);

        List<UserCaisseRecap> items = new ArrayList<>();
        if (nonNull(ticket)) {
            buildResume(ticket.summaries(), items);
            items.addAll(buildUserRecap(ticket.datas()));
        }
        return new RecapCaisse( items);
    }

    private List<ListItem> buildItems(List<TicketZData> data) {
        if (CollectionUtils.isEmpty(data)) {
            return new ArrayList<>();
        }
        return data.stream()
            .map(d -> new ListItem(d.libelle(), d.montant(), null))
            .toList();
    }

    private List<UserCaisseRecap> buildUserRecap(List<TicketZRecap> data) {
        if (CollectionUtils.isEmpty(data)) {
            return new ArrayList<>();
        }
        return data.stream()
            .map(d -> new UserCaisseRecap(d.userName(), buildItems(d.datas()), buildItems(d.summary())))
            .toList();
    }

    private void buildResume(List<TicketZData> datas,List<UserCaisseRecap> items) {
        if (!CollectionUtils.isEmpty(datas)) {
            items.add(new UserCaisseRecap("Recapitulatif général", buildItems(datas), List.of()));
        }

    }
}
