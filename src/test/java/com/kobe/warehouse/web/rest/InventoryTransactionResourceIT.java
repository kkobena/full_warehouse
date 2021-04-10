package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.repository.InventoryTransactionRepository;

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

import com.kobe.warehouse.domain.enumeration.TransactionType;
/**
 * Integration tests for the {@link InventoryTransactionResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class InventoryTransactionResourceIT {

    private static final TransactionType DEFAULT_TRANSACTION_TYPE = TransactionType.SALE;
    private static final TransactionType UPDATED_TRANSACTION_TYPE = TransactionType.REAPPRO;

    private static final Integer DEFAULT_AMOUNT = 1;
    private static final Integer UPDATED_AMOUNT = 2;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_QUANTITY = 1;
    private static final Integer UPDATED_QUANTITY = 2;

    private static final Integer DEFAULT_QUANTITY_BEFOR = 1;
    private static final Integer UPDATED_QUANTITY_BEFOR = 2;

    private static final Integer DEFAULT_QUANTITY_AFTER = 1;
    private static final Integer UPDATED_QUANTITY_AFTER = 2;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restInventoryTransactionMockMvc;

    private InventoryTransaction inventoryTransaction;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static InventoryTransaction createEntity(EntityManager em) {
        InventoryTransaction inventoryTransaction = new InventoryTransaction()
            .transactionType(DEFAULT_TRANSACTION_TYPE)
            .amount(DEFAULT_AMOUNT)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT)
            .quantity(DEFAULT_QUANTITY)
            .quantityBefor(DEFAULT_QUANTITY_BEFOR)
            .quantityAfter(DEFAULT_QUANTITY_AFTER);
        return inventoryTransaction;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static InventoryTransaction createUpdatedEntity(EntityManager em) {
        InventoryTransaction inventoryTransaction = new InventoryTransaction()
            .transactionType(UPDATED_TRANSACTION_TYPE)
            .amount(UPDATED_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .quantity(UPDATED_QUANTITY)
            .quantityBefor(UPDATED_QUANTITY_BEFOR)
            .quantityAfter(UPDATED_QUANTITY_AFTER);
        return inventoryTransaction;
    }

    @BeforeEach
    public void initTest() {
        inventoryTransaction = createEntity(em);
    }

    @Test
    @Transactional
    public void createInventoryTransaction() throws Exception {
        int databaseSizeBeforeCreate = inventoryTransactionRepository.findAll().size();
        // Create the InventoryTransaction
        restInventoryTransactionMockMvc.perform(post("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isCreated());

        // Validate the InventoryTransaction in the database
        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeCreate + 1);
        InventoryTransaction testInventoryTransaction = inventoryTransactionList.get(inventoryTransactionList.size() - 1);
        assertThat(testInventoryTransaction.getTransactionType()).isEqualTo(DEFAULT_TRANSACTION_TYPE);
        assertThat(testInventoryTransaction.getAmount()).isEqualTo(DEFAULT_AMOUNT);
        assertThat(testInventoryTransaction.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testInventoryTransaction.getUpdatedAt()).isEqualTo(DEFAULT_UPDATED_AT);
        assertThat(testInventoryTransaction.getQuantity()).isEqualTo(DEFAULT_QUANTITY);
        assertThat(testInventoryTransaction.getQuantityBefor()).isEqualTo(DEFAULT_QUANTITY_BEFOR);
        assertThat(testInventoryTransaction.getQuantityAfter()).isEqualTo(DEFAULT_QUANTITY_AFTER);
    }

    @Test
    @Transactional
    public void createInventoryTransactionWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = inventoryTransactionRepository.findAll().size();

        // Create the InventoryTransaction with an existing ID
        inventoryTransaction.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restInventoryTransactionMockMvc.perform(post("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isBadRequest());

        // Validate the InventoryTransaction in the database
        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkTransactionTypeIsRequired() throws Exception {
        int databaseSizeBeforeTest = inventoryTransactionRepository.findAll().size();
        // set the field null
        inventoryTransaction.setTransactionType(null);

        // Create the InventoryTransaction, which fails.


        restInventoryTransactionMockMvc.perform(post("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isBadRequest());

        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = inventoryTransactionRepository.findAll().size();
        // set the field null
        inventoryTransaction.setAmount(null);

        // Create the InventoryTransaction, which fails.


        restInventoryTransactionMockMvc.perform(post("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isBadRequest());

        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = inventoryTransactionRepository.findAll().size();
        // set the field null
        inventoryTransaction.setCreatedAt(null);

        // Create the InventoryTransaction, which fails.


        restInventoryTransactionMockMvc.perform(post("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isBadRequest());

        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUpdatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = inventoryTransactionRepository.findAll().size();
        // set the field null
        inventoryTransaction.setUpdatedAt(null);

        // Create the InventoryTransaction, which fails.


        restInventoryTransactionMockMvc.perform(post("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isBadRequest());

        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantityIsRequired() throws Exception {
        int databaseSizeBeforeTest = inventoryTransactionRepository.findAll().size();
        // set the field null
        inventoryTransaction.setQuantity(null);

        // Create the InventoryTransaction, which fails.


        restInventoryTransactionMockMvc.perform(post("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isBadRequest());

        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantityBeforIsRequired() throws Exception {
        int databaseSizeBeforeTest = inventoryTransactionRepository.findAll().size();
        // set the field null
        inventoryTransaction.setQuantityBefor(null);

        // Create the InventoryTransaction, which fails.


        restInventoryTransactionMockMvc.perform(post("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isBadRequest());

        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantityAfterIsRequired() throws Exception {
        int databaseSizeBeforeTest = inventoryTransactionRepository.findAll().size();
        // set the field null
        inventoryTransaction.setQuantityAfter(null);

        // Create the InventoryTransaction, which fails.


        restInventoryTransactionMockMvc.perform(post("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isBadRequest());

        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllInventoryTransactions() throws Exception {
        // Initialize the database
        inventoryTransactionRepository.saveAndFlush(inventoryTransaction);

        // Get all the inventoryTransactionList
        restInventoryTransactionMockMvc.perform(get("/api/inventory-transactions?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(inventoryTransaction.getId().intValue())))
            .andExpect(jsonPath("$.[*].transactionType").value(hasItem(DEFAULT_TRANSACTION_TYPE.toString())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].quantityBefor").value(hasItem(DEFAULT_QUANTITY_BEFOR)))
            .andExpect(jsonPath("$.[*].quantityAfter").value(hasItem(DEFAULT_QUANTITY_AFTER)));
    }
    
    @Test
    @Transactional
    public void getInventoryTransaction() throws Exception {
        // Initialize the database
        inventoryTransactionRepository.saveAndFlush(inventoryTransaction);

        // Get the inventoryTransaction
        restInventoryTransactionMockMvc.perform(get("/api/inventory-transactions/{id}", inventoryTransaction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(inventoryTransaction.getId().intValue()))
            .andExpect(jsonPath("$.transactionType").value(DEFAULT_TRANSACTION_TYPE.toString()))
            .andExpect(jsonPath("$.amount").value(DEFAULT_AMOUNT))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY))
            .andExpect(jsonPath("$.quantityBefor").value(DEFAULT_QUANTITY_BEFOR))
            .andExpect(jsonPath("$.quantityAfter").value(DEFAULT_QUANTITY_AFTER));
    }
    @Test
    @Transactional
    public void getNonExistingInventoryTransaction() throws Exception {
        // Get the inventoryTransaction
        restInventoryTransactionMockMvc.perform(get("/api/inventory-transactions/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateInventoryTransaction() throws Exception {
        // Initialize the database
        inventoryTransactionRepository.saveAndFlush(inventoryTransaction);

        int databaseSizeBeforeUpdate = inventoryTransactionRepository.findAll().size();

        // Update the inventoryTransaction
        InventoryTransaction updatedInventoryTransaction = inventoryTransactionRepository.findById(inventoryTransaction.getId()).get();
        // Disconnect from session so that the updates on updatedInventoryTransaction are not directly saved in db
        em.detach(updatedInventoryTransaction);
        updatedInventoryTransaction
            .transactionType(UPDATED_TRANSACTION_TYPE)
            .amount(UPDATED_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .quantity(UPDATED_QUANTITY)
            .quantityBefor(UPDATED_QUANTITY_BEFOR)
            .quantityAfter(UPDATED_QUANTITY_AFTER);

        restInventoryTransactionMockMvc.perform(put("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedInventoryTransaction)))
            .andExpect(status().isOk());

        // Validate the InventoryTransaction in the database
        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeUpdate);
        InventoryTransaction testInventoryTransaction = inventoryTransactionList.get(inventoryTransactionList.size() - 1);
        assertThat(testInventoryTransaction.getTransactionType()).isEqualTo(UPDATED_TRANSACTION_TYPE);
        assertThat(testInventoryTransaction.getAmount()).isEqualTo(UPDATED_AMOUNT);
        assertThat(testInventoryTransaction.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testInventoryTransaction.getUpdatedAt()).isEqualTo(UPDATED_UPDATED_AT);
        assertThat(testInventoryTransaction.getQuantity()).isEqualTo(UPDATED_QUANTITY);
        assertThat(testInventoryTransaction.getQuantityBefor()).isEqualTo(UPDATED_QUANTITY_BEFOR);
        assertThat(testInventoryTransaction.getQuantityAfter()).isEqualTo(UPDATED_QUANTITY_AFTER);
    }

    @Test
    @Transactional
    public void updateNonExistingInventoryTransaction() throws Exception {
        int databaseSizeBeforeUpdate = inventoryTransactionRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restInventoryTransactionMockMvc.perform(put("/api/inventory-transactions").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(inventoryTransaction)))
            .andExpect(status().isBadRequest());

        // Validate the InventoryTransaction in the database
        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteInventoryTransaction() throws Exception {
        // Initialize the database
        inventoryTransactionRepository.saveAndFlush(inventoryTransaction);

        int databaseSizeBeforeDelete = inventoryTransactionRepository.findAll().size();

        // Delete the inventoryTransaction
        restInventoryTransactionMockMvc.perform(delete("/api/inventory-transactions/{id}", inventoryTransaction.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<InventoryTransaction> inventoryTransactionList = inventoryTransactionRepository.findAll();
        assertThat(inventoryTransactionList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
