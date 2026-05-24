package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.ImportationEchoue;
import com.kobe.warehouse.domain.ImportationEchoueLigne;
import com.kobe.warehouse.repository.ImportationEchoueLigneRepository;
import com.kobe.warehouse.repository.ImportationEchoueRepository;
import com.kobe.warehouse.service.dto.OrderItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ImportationEchoueService {

    private final ImportationEchoueRepository importationEchoueRepository;
    private final ImportationEchoueLigneRepository importationEchoueLigneRepository;

    public ImportationEchoueService(
        ImportationEchoueRepository importationEchoueRepository,
        ImportationEchoueLigneRepository importationEchoueLigneRepository
    ) {
        this.importationEchoueRepository = importationEchoueRepository;
        this.importationEchoueLigneRepository = importationEchoueLigneRepository;
    }

    public void save(Integer objectId, boolean isCommande, List<OrderItem> items) {
        ImportationEchoue importationEchoue = new ImportationEchoue();
        importationEchoue.setObjectId(objectId);
        importationEchoue.setCommande(isCommande);
        items
            .stream()
            .map(this::buildFromOrderItem)
            .peek(e -> e.setImportationEchoue(importationEchoue))
            .forEach(importationEchoue.getImportationEchoueLignes()::add);
        importationEchoueRepository.save(importationEchoue);
    }

    private ImportationEchoueLigne buildFromOrderItem(OrderItem item) {
        ImportationEchoueLigne importationEchoueLigne = new ImportationEchoueLigne();
        importationEchoueLigne.setCodeTva(Objects.nonNull(item.getTva()) ? item.getTva().intValue() : null);
        importationEchoueLigne.setQuantityReceived(item.getQuantityReceived());
        importationEchoueLigne.setPrixAchat(item.getPrixAchat());
        importationEchoueLigne.setPrixUn((int) item.getPrixUn());
        importationEchoueLigne.setUg(item.getUg());
        importationEchoueLigne.setProduitCip(item.getProduitCip());
        importationEchoueLigne.setProduitEan(item.getProduitEan());
        importationEchoueLigne.setDatePeremption(
            StringUtils.hasText(item.getDatePeremption()) ? LocalDate.parse(item.getDatePeremption()) : null
        );

        return importationEchoueLigne;
    }
}
