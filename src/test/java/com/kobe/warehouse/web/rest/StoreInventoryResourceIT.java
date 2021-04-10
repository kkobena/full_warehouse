package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.repository.StoreInventoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link StoreInventoryResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class StoreInventoryResourceIT {

    private static final Long DEFAULT_INVENTORY_VALUE_COST_BEGIN = 1L;
    private static final Long UPDATED_INVENTORY_VALUE_COST_BEGIN = 2L;

    private static final Long DEFAULT_INVENTORY_AMOUNT_BEGIN = 1L;
    private static final Long UPDATED_INVENTORY_AMOUNT_BEGIN = 2L;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Long DEFAULT_INVENTORY_VALUE_COST_AFTER = 1L;
    private static final Long UPDATED_INVENTORY_VALUE_COST_AFTER = 2L;

    private static final Long DEFAULT_INVENTORY_AMOUNT_AFTER = 1L;
    private static final Long UPDATED_INVENTORY_AMOUNT_AFTER = 2L;

    @Autowired
    private StoreInventoryRepository storeInventoryRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restStoreInventoryMockMvc;

    private StoreInventory storeInventory;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StoreInventory createEntity(EntityManager em) {
        StoreInventory storeInventory = new StoreInventory()
            .inventoryValueCostBegin(DEFAULT_INVENTORY_VALUE_COST_BEGIN)
            .inventoryAmountBegin(DEFAULT_INVENTORY_AMOUNT_BEGIN)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT)
            .inventoryValueCostAfter(DEFAULT_INVENTORY_VALUE_COST_AFTER)
            .inventoryAmountAfter(DEFAULT_INVENTORY_AMOUNT_AFTER);
        return storeInventory;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StoreInventory createUpdatedEntity(EntityManager em) {
        StoreInventory storeInventory = new StoreInventory()
            .inventoryValueCostBegin(UPDATED_INVENTORY_VALUE_COST_BEGIN)
            .inventoryAmountBegin(UPDATED_INVENTORY_AMOUNT_BEGIN)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .inventoryValueCostAfter(UPDATED_INVENTORY_VALUE_COST_AFTER)
            .inventoryAmountAfter(UPDATED_INVENTORY_AMOUNT_AFTER);
        return storeInventory;
    }

    @BeforeEach
    public void initTest() {
        storeInventory = createEntity(em);
    }

    @Test
    @Transactional
    public void createStoreInventory() throws Exception {
        int databaseSizeBeforeCreate = storeInventoryRepository.findAll().size();
        // Create the StoreInventory
        restStoreInventoryMockMvc.perform(post("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventory)))
            .andExpect(status().isCreated());

        // Validate the StoreInventory in the database
        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeCreate + 1);
        StoreInventory testStoreInventory = storeInventoryList.get(storeInventoryList.size() - 1);
        assertThat(testStoreInventory.getInventoryValueCostBegin()).isEqualTo(DEFAULT_INVENTORY_VALUE_COST_BEGIN);
        assertThat(testStoreInventory.getInventoryAmountBegin()).isEqualTo(DEFAULT_INVENTORY_AMOUNT_BEGIN);
        assertThat(testStoreInventory.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testStoreInventory.getUpdatedAt()).isEqualTo(DEFAULT_UPDATED_AT);
        assertThat(testStoreInventory.getInventoryValueCostAfter()).isEqualTo(DEFAULT_INVENTORY_VALUE_COST_AFTER);
        assertThat(testStoreInventory.getInventoryAmountAfter()).isEqualTo(DEFAULT_INVENTORY_AMOUNT_AFTER);
    }

    @Test
    @Transactional
    public void createStoreInventoryWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = storeInventoryRepository.findAll().size();

        // Create the StoreInventory with an existing ID
        storeInventory.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restStoreInventoryMockMvc.perform(post("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventory)))
            .andExpect(status().isBadRequest());

        // Validate the StoreInventory in the database
        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkInventoryValueCostBeginIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryRepository.findAll().size();
        // set the field null
        storeInventory.setInventoryValueCostBegin(null);

        // Create the StoreInventory, which fails.


        restStoreInventoryMockMvc.perform(post("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventory)))
            .andExpect(status().isBadRequest());

        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkInventoryAmountBeginIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryRepository.findAll().size();
        // set the field null
        storeInventory.setInventoryAmountBegin(null);

        // Create the StoreInventory, which fails.


        restStoreInventoryMockMvc.perform(post("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventory)))
            .andExpect(status().isBadRequest());

        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryRepository.findAll().size();
        // set the field null
        storeInventory.setCreatedAt(null);

        // Create the StoreInventory, which fails.


        restStoreInventoryMockMvc.perform(post("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventory)))
            .andExpect(status().isBadRequest());

        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUpdatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryRepository.findAll().size();
        // set the field null
        storeInventory.setUpdatedAt(null);

        // Create the StoreInventory, which fails.


        restStoreInventoryMockMvc.perform(post("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventory)))
            .andExpect(status().isBadRequest());

        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkInventoryValueCostAfterIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryRepository.findAll().size();
        // set the field null
        storeInventory.setInventoryValueCostAfter(null);

        // Create the StoreInventory, which fails.


        restStoreInventoryMockMvc.perform(post("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventory)))
            .andExpect(status().isBadRequest());

        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkInventoryAmountAfterIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryRepository.findAll().size();
        // set the field null
        storeInventory.setInventoryAmountAfter(null);

        // Create the StoreInventory, which fails.


        restStoreInventoryMockMvc.perform(post("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventory)))
            .andExpect(status().isBadRequest());

        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllStoreInventories() throws Exception {
        // Initialize the database
        storeInventoryRepository.saveAndFlush(storeInventory);

        // Get all the storeInventoryList
        restStoreInventoryMockMvc.perform(get("/api/store-inventories?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(storeInventory.getId().intValue())))
            .andExpect(jsonPath("$.[*].inventoryValueCostBegin").value(hasItem(DEFAULT_INVENTORY_VALUE_COST_BEGIN.intValue())))
            .andExpect(jsonPath("$.[*].inventoryAmountBegin").value(hasItem(DEFAULT_INVENTORY_AMOUNT_BEGIN.intValue())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].inventoryValueCostAfter").value(hasItem(DEFAULT_INVENTORY_VALUE_COST_AFTER.intValue())))
            .andExpect(jsonPath("$.[*].inventoryAmountAfter").value(hasItem(DEFAULT_INVENTORY_AMOUNT_AFTER.intValue())));
    }
    
    @Test
    @Transactional
    public void getStoreInventory() throws Exception {
        // Initialize the database
        storeInventoryRepository.saveAndFlush(storeInventory);

        // Get the storeInventory
        restStoreInventoryMockMvc.perform(get("/api/store-inventories/{id}", storeInventory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(storeInventory.getId().intValue()))
            .andExpect(jsonPath("$.inventoryValueCostBegin").value(DEFAULT_INVENTORY_VALUE_COST_BEGIN.intValue()))
            .andExpect(jsonPath("$.inventoryAmountBegin").value(DEFAULT_INVENTORY_AMOUNT_BEGIN.intValue()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()))
            .andExpect(jsonPath("$.inventoryValueCostAfter").value(DEFAULT_INVENTORY_VALUE_COST_AFTER.intValue()))
            .andExpect(jsonPath("$.inventoryAmountAfter").value(DEFAULT_INVENTORY_AMOUNT_AFTER.intValue()));
    }
    @Test
    @Transactional
    public void getNonExistingStoreInventory() throws Exception {
        // Get the storeInventory
        restStoreInventoryMockMvc.perform(get("/api/store-inventories/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateStoreInventory() throws Exception {
        // Initialize the database
        storeInventoryRepository.saveAndFlush(storeInventory);

        int databaseSizeBeforeUpdate = storeInventoryRepository.findAll().size();

        // Update the storeInventory
        StoreInventory updatedStoreInventory = storeInventoryRepository.findById(storeInventory.getId()).get();
        // Disconnect from session so that the updates on updatedStoreInventory are not directly saved in db
        em.detach(updatedStoreInventory);
        updatedStoreInventory
            .inventoryValueCostBegin(UPDATED_INVENTORY_VALUE_COST_BEGIN)
            .inventoryAmountBegin(UPDATED_INVENTORY_AMOUNT_BEGIN)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .inventoryValueCostAfter(UPDATED_INVENTORY_VALUE_COST_AFTER)
            .inventoryAmountAfter(UPDATED_INVENTORY_AMOUNT_AFTER);

        restStoreInventoryMockMvc.perform(put("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedStoreInventory)))
            .andExpect(status().isOk());

        // Validate the StoreInventory in the database
        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeUpdate);
        StoreInventory testStoreInventory = storeInventoryList.get(storeInventoryList.size() - 1);
        assertThat(testStoreInventory.getInventoryValueCostBegin()).isEqualTo(UPDATED_INVENTORY_VALUE_COST_BEGIN);
        assertThat(testStoreInventory.getInventoryAmountBegin()).isEqualTo(UPDATED_INVENTORY_AMOUNT_BEGIN);
        assertThat(testStoreInventory.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testStoreInventory.getUpdatedAt()).isEqualTo(UPDATED_UPDATED_AT);
        assertThat(testStoreInventory.getInventoryValueCostAfter()).isEqualTo(UPDATED_INVENTORY_VALUE_COST_AFTER);
        assertThat(testStoreInventory.getInventoryAmountAfter()).isEqualTo(UPDATED_INVENTORY_AMOUNT_AFTER);
    }

    @Test
    @Transactional
    public void updateNonExistingStoreInventory() throws Exception {
        int databaseSizeBeforeUpdate = storeInventoryRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStoreInventoryMockMvc.perform(put("/api/store-inventories").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventory)))
            .andExpect(status().isBadRequest());

        // Validate the StoreInventory in the database
        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteStoreInventory() throws Exception {
        // Initialize the database
        storeInventoryRepository.saveAndFlush(storeInventory);

        int databaseSizeBeforeDelete = storeInventoryRepository.findAll().size();

        // Delete the storeInventory
        restStoreInventoryMockMvc.perform(delete("/api/store-inventories/{id}", storeInventory.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<StoreInventory> storeInventoryList = storeInventoryRepository.findAll();
        assertThat(storeInventoryList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
