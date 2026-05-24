package com.kobe.warehouse.service.financiel_transaction;

import static com.kobe.warehouse.service.financiel_transaction.TableauPharmacienConstants.*;

import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class GroupeFournisseurManager {

    private final FournisseurRepository fournisseurRepository;

    public GroupeFournisseurManager(FournisseurRepository fournisseurRepository) {
        this.fournisseurRepository = fournisseurRepository;
    }

    public List<GroupeFournisseurDTO> getDisplayedSupplierGroups() {
        List<GroupeFournisseurDTO> allGroups = fournisseurRepository.findByParentIsNullOrderByOdreAsc()
            .stream()
            .map(GroupeFournisseurDTO::new)
            .sorted(Comparator.comparing(GroupeFournisseurDTO::getOdre))
            .toList();

        if (allGroups.size() > MAX_DISPLAYED_GROUPS) {
            return buildTopGroupsWithOthers(allGroups);
        }
        return allGroups;
    }

    public Set<Integer> getDisplayedGroupIds() {
        return getDisplayedSupplierGroups()
            .stream()
            .map(GroupeFournisseurDTO::getId)
            .filter(id -> id != GROUP_OTHER_ID)
            .collect(Collectors.toSet());
    }

    private List<GroupeFournisseurDTO> buildTopGroupsWithOthers(List<GroupeFournisseurDTO> allGroups) {
        List<GroupeFournisseurDTO> displayedGroups = new ArrayList<>(allGroups.subList(0, MAX_DISPLAYED_GROUPS));
        displayedGroups.add(new GroupeFournisseurDTO()
            .setId(GROUP_OTHER_ID)
            .setLibelle(GROUP_OTHER_LABEL)
            .setOdre(GROUP_OTHER_ORDER));
        displayedGroups.sort(Comparator.comparing(GroupeFournisseurDTO::getOdre));
        return displayedGroups;
    }
}
