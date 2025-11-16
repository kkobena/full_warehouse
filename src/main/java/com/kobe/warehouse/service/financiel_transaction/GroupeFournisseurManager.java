package com.kobe.warehouse.service.financiel_transaction;

import static com.kobe.warehouse.service.financiel_transaction.TableauPharmacienConstants.*;

import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.repository.GroupeFournisseurRepository;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Manages supplier group display logic
 */
@Component
public class GroupeFournisseurManager {

    private final GroupeFournisseurRepository groupeFournisseurRepository;

    public GroupeFournisseurManager(GroupeFournisseurRepository groupeFournisseurRepository) {
        this.groupeFournisseurRepository = groupeFournisseurRepository;
    }

    /**
     * Get the list of supplier groups to display in the tableau.
     * If more than MAX_DISPLAYED_GROUPS, shows top groups + "Autres" category.
     */
    public List<GroupeFournisseurDTO> getDisplayedSupplierGroups() {
        List<GroupeFournisseur> allGroups = groupeFournisseurRepository.findAllByOrderByOdreAsc();

        if (allGroups.size() > MAX_DISPLAYED_GROUPS) {
            return buildTopGroupsWithOthers(allGroups);
        } else {
            return allGroups.stream().sorted(Comparator.comparing(GroupeFournisseur::getOdre)).map(GroupeFournisseurDTO::new).toList();
        }
    }

    /**
     * Get IDs of groups to display individually (not grouped as "Autres")
     */
    public Set<Integer> getDisplayedGroupIds() {
        return getDisplayedSupplierGroups()
            .stream()
            .map(GroupeFournisseurDTO::getId)
            .filter(id -> id != GROUP_OTHER_ID) // Exclude "Autres" virtual group
            .collect(Collectors.toSet());
    }

    /**
     * Build list with top N groups + "Autres" category
     */
    private List<GroupeFournisseurDTO> buildTopGroupsWithOthers(List<GroupeFournisseur> allGroups) {
        List<GroupeFournisseurDTO> displayedGroups = new ArrayList<>();

        // Add top N groups
        allGroups.stream().limit(MAX_DISPLAYED_GROUPS).map(GroupeFournisseurDTO::new).forEach(displayedGroups::add);

        // Add "Autres" virtual group
        GroupeFournisseurDTO othersGroup = new GroupeFournisseurDTO()
            .setId(GROUP_OTHER_ID)
            .setLibelle(GROUP_OTHER_LABEL)
            .setOdre(GROUP_OTHER_ORDER);
        displayedGroups.add(othersGroup);

        // Sort by display order
        displayedGroups.sort(Comparator.comparing(GroupeFournisseurDTO::getOdre));

        return displayedGroups;
    }
}
