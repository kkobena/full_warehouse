package com.kobe.warehouse.service.financiel_transaction;

import static com.kobe.warehouse.service.financiel_transaction.TableauPharmacienConstants.GROUP_OTHER_ID;
import static com.kobe.warehouse.service.financiel_transaction.TableauPharmacienConstants.MAX_DISPLAYED_GROUPS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.repository.GroupeFournisseurRepository;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupeFournisseurManagerTest {

    @Mock
    private GroupeFournisseurRepository groupeFournisseurRepository;

    @InjectMocks
    private GroupeFournisseurManager groupeFournisseurManager;

    // ===== Get Displayed Supplier Groups Tests =====

    @Test
    void testGetDisplayedSupplierGroups_lessThanMaxGroups() {
        List<GroupeFournisseur> groups = createGroupeFournisseurs(3);
        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(groups);

        List<GroupeFournisseurDTO> result = groupeFournisseurManager.getDisplayedSupplierGroups();

        assertEquals(3, result.size());
        assertFalse(result.stream().anyMatch(g -> g.getId() == GROUP_OTHER_ID));
        assertEquals(1, result.get(0).getId());
        assertEquals(2, result.get(1).getId());
        assertEquals(3, result.get(2).getId());
    }

    @Test
    void testGetDisplayedSupplierGroups_equalToMaxGroups() {
        List<GroupeFournisseur> groups = createGroupeFournisseurs(MAX_DISPLAYED_GROUPS);
        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(groups);

        List<GroupeFournisseurDTO> result = groupeFournisseurManager.getDisplayedSupplierGroups();

        assertEquals(MAX_DISPLAYED_GROUPS, result.size());
        assertFalse(result.stream().anyMatch(g -> g.getId() == GROUP_OTHER_ID));
    }

    @Test
    void testGetDisplayedSupplierGroups_moreThanMaxGroups() {
        List<GroupeFournisseur> groups = createGroupeFournisseurs(6); // More than MAX_DISPLAYED_GROUPS (4)
        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(groups);

        List<GroupeFournisseurDTO> result = groupeFournisseurManager.getDisplayedSupplierGroups();

        // Should have MAX_DISPLAYED_GROUPS + 1 (for "Autres")
        assertEquals(MAX_DISPLAYED_GROUPS + 1, result.size());

        // Check that "Autres" group is present
        assertTrue(result.stream().anyMatch(g -> g.getId() == GROUP_OTHER_ID));

        // Check that only top MAX_DISPLAYED_GROUPS are included (plus "Autres")
        long actualGroupCount = result.stream().filter(g -> g.getId() != GROUP_OTHER_ID).count();
        assertEquals(MAX_DISPLAYED_GROUPS, actualGroupCount);

        // Verify top groups are included
        assertTrue(result.stream().anyMatch(g -> g.getId() == 1));
        assertTrue(result.stream().anyMatch(g -> g.getId() == 2));
        assertTrue(result.stream().anyMatch(g -> g.getId() == 3));
        assertTrue(result.stream().anyMatch(g -> g.getId() == 4));

        // Verify groups 5 and 6 are NOT individually displayed
        assertFalse(result.stream().anyMatch(g -> g.getId() == 5));
        assertFalse(result.stream().anyMatch(g -> g.getId() == 6));
    }

    @Test
    void testGetDisplayedSupplierGroups_emptyList() {
        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(new ArrayList<>());

        List<GroupeFournisseurDTO> result = groupeFournisseurManager.getDisplayedSupplierGroups();

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetDisplayedSupplierGroups_sortedByOdre() {
        List<GroupeFournisseur> groups = new ArrayList<>();
        groups.add(createGroupeFournisseur(3, "Supplier C", 30));
        groups.add(createGroupeFournisseur(1, "Supplier A", 10));
        groups.add(createGroupeFournisseur(2, "Supplier B", 20));

        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(groups);

        List<GroupeFournisseurDTO> result = groupeFournisseurManager.getDisplayedSupplierGroups();

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getId()); // ordre 10
        assertEquals(2, result.get(1).getId()); // ordre 20
        assertEquals(3, result.get(2).getId()); // ordre 30
    }

    @Test
    void testGetDisplayedSupplierGroups_othersGroupHasCorrectOrder() {
        List<GroupeFournisseur> groups = createGroupeFournisseurs(6);
        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(groups);

        List<GroupeFournisseurDTO> result = groupeFournisseurManager.getDisplayedSupplierGroups();

        GroupeFournisseurDTO othersGroup = result.stream().filter(g -> g.getId() == GROUP_OTHER_ID).findFirst().orElseThrow();

        assertNotNull(othersGroup);
        assertEquals("Autres", othersGroup.getLibelle());
        // "Autres" should be sorted by its order (999)
        assertEquals(result.get(result.size() - 1).getId(), GROUP_OTHER_ID);
    }

    // ===== Get Displayed Group IDs Tests =====

    @Test
    void testGetDisplayedGroupIds_lessThanMaxGroups() {
        List<GroupeFournisseur> groups = createGroupeFournisseurs(3);
        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(groups);

        Set<Integer> result = groupeFournisseurManager.getDisplayedGroupIds();

        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
        assertFalse(result.contains(GROUP_OTHER_ID));
    }

    @Test
    void testGetDisplayedGroupIds_moreThanMaxGroups() {
        List<GroupeFournisseur> groups = createGroupeFournisseurs(6);
        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(groups);

        Set<Integer> result = groupeFournisseurManager.getDisplayedGroupIds();

        // Should only include top MAX_DISPLAYED_GROUPS IDs
        assertEquals(MAX_DISPLAYED_GROUPS, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
        assertTrue(result.contains(4));
        assertFalse(result.contains(5));
        assertFalse(result.contains(6));
        assertFalse(result.contains(GROUP_OTHER_ID)); // "Autres" should not be in the ID set
    }

    @Test
    void testGetDisplayedGroupIds_emptyList() {
        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(new ArrayList<>());

        Set<Integer> result = groupeFournisseurManager.getDisplayedGroupIds();

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetDisplayedGroupIds_excludesOthersGroupId() {
        List<GroupeFournisseur> groups = createGroupeFournisseurs(6);
        when(groupeFournisseurRepository.findAllByOrderByOdreAsc()).thenReturn(groups);

        Set<Integer> result = groupeFournisseurManager.getDisplayedGroupIds();

        // "Autres" group (ID = -1) should never be in the ID set
        assertFalse(result.contains(GROUP_OTHER_ID));

        // Verify it's explicitly filtered out
        List<GroupeFournisseurDTO> displayedGroups = groupeFournisseurManager.getDisplayedSupplierGroups();
        assertTrue(displayedGroups.stream().anyMatch(g -> g.getId() == GROUP_OTHER_ID));
        assertFalse(result.contains(GROUP_OTHER_ID));
    }

    // ===== Helper Methods =====

    private List<GroupeFournisseur> createGroupeFournisseurs(int count) {
        List<GroupeFournisseur> groups = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            groups.add(createGroupeFournisseur(i, "Supplier " + i, i * 10));
        }
        return groups;
    }

    private GroupeFournisseur createGroupeFournisseur(int id, String libelle, int ordre) {
        GroupeFournisseur groupe = new GroupeFournisseur();
        groupe.setId(id);
        groupe.setLibelle(libelle);
        groupe.setOdre(ordre);
        return groupe;
    }
}
