package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link StoreInventoryLineResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class StoreInventoryLineResourceIT {

    private static final Integer DEFAULT_QUANTITY_ON_HAND = 1;
    private static final Integer UPDATED_QUANTITY_ON_HAND = 2;

    private static final Integer DEFAULT_QUANTITY_INIT = 1;
    private static final Integer UPDATED_QUANTITY_INIT = 2;

    private static final Integer DEFAULT_QUANTITY_SOLD = 1;
    private static final Integer UPDATED_QUANTITY_SOLD = 2;

    private static final Integer DEFAULT_INVENTORY_VALUE_COST = 1;
    private static final Integer UPDATED_INVENTORY_VALUE_COST = 2;

    private static final Integer DEFAULT_INVENTORY_VALUE_LATEST_SELLING_PRICE = 1;
    private static final Integer UPDATED_INVENTORY_VALUE_LATEST_SELLING_PRICE = 2;

    @Autowired
    private StoreInventoryLineRepository storeInventoryLineRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restStoreInventoryLineMockMvc;

    private StoreInventoryLine storeInventoryLine;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StoreInventoryLine createEntity(EntityManager em) {
        StoreInventoryLine storeInventoryLine = new StoreInventoryLine()
            .quantityOnHand(DEFAULT_QUANTITY_ON_HAND)
            .quantityInit(DEFAULT_QUANTITY_INIT)
            .quantitySold(DEFAULT_QUANTITY_SOLD)
            .inventoryValueCost(DEFAULT_INVENTORY_VALUE_COST)
            .inventoryValueLatestSellingPrice(DEFAULT_INVENTORY_VALUE_LATEST_SELLING_PRICE);
        return storeInventoryLine;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StoreInventoryLine createUpdatedEntity(EntityManager em) {
        StoreInventoryLine storeInventoryLine = new StoreInventoryLine()
            .quantityOnHand(UPDATED_QUANTITY_ON_HAND)
            .quantityInit(UPDATED_QUANTITY_INIT)
            .quantitySold(UPDATED_QUANTITY_SOLD)
            .inventoryValueCost(UPDATED_INVENTORY_VALUE_COST)
            .inventoryValueLatestSellingPrice(UPDATED_INVENTORY_VALUE_LATEST_SELLING_PRICE);
        return storeInventoryLine;
    }

    @BeforeEach
    public void initTest() {
        storeInventoryLine = createEntity(em);
    }

    @Test
    @Transactional
    public void createStoreInventoryLine() throws Exception {
        int databaseSizeBeforeCreate = storeInventoryLineRepository.findAll().size();
        // Create the StoreInventoryLine
        restStoreInventoryLineMockMvc.perform(post("/api/store-inventory-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventoryLine)))
            .andExpect(status().isCreated());

        // Validate the StoreInventoryLine in the database
        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeCreate + 1);
        StoreInventoryLine testStoreInventoryLine = storeInventoryLineList.get(storeInventoryLineList.size() - 1);
        assertThat(testStoreInventoryLine.getQuantityOnHand()).isEqualTo(DEFAULT_QUANTITY_ON_HAND);
        assertThat(testStoreInventoryLine.getQuantityInit()).isEqualTo(DEFAULT_QUANTITY_INIT);
        assertThat(testStoreInventoryLine.getQuantitySold()).isEqualTo(DEFAULT_QUANTITY_SOLD);
        assertThat(testStoreInventoryLine.getInventoryValueCost()).isEqualTo(DEFAULT_INVENTORY_VALUE_COST);
        assertThat(testStoreInventoryLine.getInventoryValueLatestSellingPrice()).isEqualTo(DEFAULT_INVENTORY_VALUE_LATEST_SELLING_PRICE);
    }

    @Test
    @Transactional
    public void createStoreInventoryLineWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = storeInventoryLineRepository.findAll().size();

        // Create the StoreInventoryLine with an existing ID
        storeInventoryLine.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restStoreInventoryLineMockMvc.perform(post("/api/store-inventory-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventoryLine)))
            .andExpect(status().isBadRequest());

        // Validate the StoreInventoryLine in the database
        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkQuantityOnHandIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryLineRepository.findAll().size();
        // set the field null
        storeInventoryLine.setQuantityOnHand(null);

        // Create the StoreInventoryLine, which fails.


        restStoreInventoryLineMockMvc.perform(post("/api/store-inventory-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventoryLine)))
            .andExpect(status().isBadRequest());

        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantityInitIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryLineRepository.findAll().size();
        // set the field null
        storeInventoryLine.setQuantityInit(null);

        // Create the StoreInventoryLine, which fails.


        restStoreInventoryLineMockMvc.perform(post("/api/store-inventory-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventoryLine)))
            .andExpect(status().isBadRequest());

        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantitySoldIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryLineRepository.findAll().size();
        // set the field null
        storeInventoryLine.setQuantitySold(null);

        // Create the StoreInventoryLine, which fails.


        restStoreInventoryLineMockMvc.perform(post("/api/store-inventory-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventoryLine)))
            .andExpect(status().isBadRequest());

        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkInventoryValueCostIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryLineRepository.findAll().size();
        // set the field null
        storeInventoryLine.setInventoryValueCost(null);

        // Create the StoreInventoryLine, which fails.


        restStoreInventoryLineMockMvc.perform(post("/api/store-inventory-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventoryLine)))
            .andExpect(status().isBadRequest());

        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkInventoryValueLatestSellingPriceIsRequired() throws Exception {
        int databaseSizeBeforeTest = storeInventoryLineRepository.findAll().size();
        // set the field null
        storeInventoryLine.setInventoryValueLatestSellingPrice(null);

        // Create the StoreInventoryLine, which fails.


        restStoreInventoryLineMockMvc.perform(post("/api/store-inventory-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventoryLine)))
            .andExpect(status().isBadRequest());

        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllStoreInventoryLines() throws Exception {
        // Initialize the database
        storeInventoryLineRepository.saveAndFlush(storeInventoryLine);

        // Get all the storeInventoryLineList
        restStoreInventoryLineMockMvc.perform(get("/api/store-inventory-lines?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(storeInventoryLine.getId().intValue())))
            .andExpect(jsonPath("$.[*].quantityOnHand").value(hasItem(DEFAULT_QUANTITY_ON_HAND)))
            .andExpect(jsonPath("$.[*].quantityInit").value(hasItem(DEFAULT_QUANTITY_INIT)))
            .andExpect(jsonPath("$.[*].quantitySold").value(hasItem(DEFAULT_QUANTITY_SOLD)))
            .andExpect(jsonPath("$.[*].inventoryValueCost").value(hasItem(DEFAULT_INVENTORY_VALUE_COST)))
            .andExpect(jsonPath("$.[*].inventoryValueLatestSellingPrice").value(hasItem(DEFAULT_INVENTORY_VALUE_LATEST_SELLING_PRICE)));
    }
    
    @Test
    @Transactional
    public void getStoreInventoryLine() throws Exception {
        // Initialize the database
        storeInventoryLineRepository.saveAndFlush(storeInventoryLine);

        // Get the storeInventoryLine
        restStoreInventoryLineMockMvc.perform(get("/api/store-inventory-lines/{id}", storeInventoryLine.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(storeInventoryLine.getId().intValue()))
            .andExpect(jsonPath("$.quantityOnHand").value(DEFAULT_QUANTITY_ON_HAND))
            .andExpect(jsonPath("$.quantityInit").value(DEFAULT_QUANTITY_INIT))
            .andExpect(jsonPath("$.quantitySold").value(DEFAULT_QUANTITY_SOLD))
            .andExpect(jsonPath("$.inventoryValueCost").value(DEFAULT_INVENTORY_VALUE_COST))
            .andExpect(jsonPath("$.inventoryValueLatestSellingPrice").value(DEFAULT_INVENTORY_VALUE_LATEST_SELLING_PRICE));
    }
    @Test
    @Transactional
    public void getNonExistingStoreInventoryLine() throws Exception {
        // Get the storeInventoryLine
        restStoreInventoryLineMockMvc.perform(get("/api/store-inventory-lines/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateStoreInventoryLine() throws Exception {
        // Initialize the database
        storeInventoryLineRepository.saveAndFlush(storeInventoryLine);

        int databaseSizeBeforeUpdate = storeInventoryLineRepository.findAll().size();

        // Update the storeInventoryLine
        StoreInventoryLine updatedStoreInventoryLine = storeInventoryLineRepository.findById(storeInventoryLine.getId()).get();
        // Disconnect from session so that the updates on updatedStoreInventoryLine are not directly saved in db
        em.detach(updatedStoreInventoryLine);
        updatedStoreInventoryLine
            .quantityOnHand(UPDATED_QUANTITY_ON_HAND)
            .quantityInit(UPDATED_QUANTITY_INIT)
            .quantitySold(UPDATED_QUANTITY_SOLD)
            .inventoryValueCost(UPDATED_INVENTORY_VALUE_COST)
            .inventoryValueLatestSellingPrice(UPDATED_INVENTORY_VALUE_LATEST_SELLING_PRICE);

        restStoreInventoryLineMockMvc.perform(put("/api/store-inventory-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedStoreInventoryLine)))
            .andExpect(status().isOk());

        // Validate the StoreInventoryLine in the database
        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeUpdate);
        StoreInventoryLine testStoreInventoryLine = storeInventoryLineList.get(storeInventoryLineList.size() - 1);
        assertThat(testStoreInventoryLine.getQuantityOnHand()).isEqualTo(UPDATED_QUANTITY_ON_HAND);
        assertThat(testStoreInventoryLine.getQuantityInit()).isEqualTo(UPDATED_QUANTITY_INIT);
        assertThat(testStoreInventoryLine.getQuantitySold()).isEqualTo(UPDATED_QUANTITY_SOLD);
        assertThat(testStoreInventoryLine.getInventoryValueCost()).isEqualTo(UPDATED_INVENTORY_VALUE_COST);
        assertThat(testStoreInventoryLine.getInventoryValueLatestSellingPrice()).isEqualTo(UPDATED_INVENTORY_VALUE_LATEST_SELLING_PRICE);
    }

    @Test
    @Transactional
    public void updateNonExistingStoreInventoryLine() throws Exception {
        int databaseSizeBeforeUpdate = storeInventoryLineRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStoreInventoryLineMockMvc.perform(put("/api/store-inventory-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(storeInventoryLine)))
            .andExpect(status().isBadRequest());

        // Validate the StoreInventoryLine in the database
        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteStoreInventoryLine() throws Exception {
        // Initialize the database
        storeInventoryLineRepository.saveAndFlush(storeInventoryLine);

        int databaseSizeBeforeDelete = storeInventoryLineRepository.findAll().size();

        // Delete the storeInventoryLine
        restStoreInventoryLineMockMvc.perform(delete("/api/store-inventory-lines/{id}", storeInventoryLine.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<StoreInventoryLine> storeInventoryLineList = storeInventoryLineRepository.findAll();
        assertThat(storeInventoryLineList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
