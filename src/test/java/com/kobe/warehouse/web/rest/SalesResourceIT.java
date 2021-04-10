package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.repository.SalesRepository;

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

import com.kobe.warehouse.domain.enumeration.SalesStatut;
/**
 * Integration tests for the {@link SalesResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class SalesResourceIT {

    private static final Integer DEFAULT_DISCOUNT_AMOUNT = 1;
    private static final Integer UPDATED_DISCOUNT_AMOUNT = 2;

    private static final Integer DEFAULT_SALES_AMOUNT = 1;
    private static final Integer UPDATED_SALES_AMOUNT = 2;

    private static final Integer DEFAULT_GROSS_AMOUNT = 1;
    private static final Integer UPDATED_GROSS_AMOUNT = 2;

    private static final Integer DEFAULT_NET_AMOUNT = 1;
    private static final Integer UPDATED_NET_AMOUNT = 2;

    private static final Integer DEFAULT_TAX_AMOUNT = 1;
    private static final Integer UPDATED_TAX_AMOUNT = 2;

    private static final Integer DEFAULT_COST_AMOUNT = 1;
    private static final Integer UPDATED_COST_AMOUNT = 2;

    private static final SalesStatut DEFAULT_STATUT = SalesStatut.PROCESSING;
    private static final SalesStatut UPDATED_STATUT = SalesStatut.PENDING;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSalesMockMvc;

    private Sales sales;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Sales createEntity(EntityManager em) {
        Sales sales = new Sales()
            .discountAmount(DEFAULT_DISCOUNT_AMOUNT)
            .salesAmount(DEFAULT_SALES_AMOUNT)
            .grossAmount(DEFAULT_GROSS_AMOUNT)
            .netAmount(DEFAULT_NET_AMOUNT)
            .taxAmount(DEFAULT_TAX_AMOUNT)
            .costAmount(DEFAULT_COST_AMOUNT)
            .statut(DEFAULT_STATUT)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
        return sales;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Sales createUpdatedEntity(EntityManager em) {
        Sales sales = new Sales()
            .discountAmount(UPDATED_DISCOUNT_AMOUNT)
            .salesAmount(UPDATED_SALES_AMOUNT)
            .grossAmount(UPDATED_GROSS_AMOUNT)
            .netAmount(UPDATED_NET_AMOUNT)
            .taxAmount(UPDATED_TAX_AMOUNT)
            .costAmount(UPDATED_COST_AMOUNT)
            .statut(UPDATED_STATUT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        return sales;
    }

    @BeforeEach
    public void initTest() {
        sales = createEntity(em);
    }

    @Test
    @Transactional
    public void createSales() throws Exception {
        int databaseSizeBeforeCreate = salesRepository.findAll().size();
        // Create the Sales
        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isCreated());

        // Validate the Sales in the database
        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeCreate + 1);
        Sales testSales = salesList.get(salesList.size() - 1);
        assertThat(testSales.getDiscountAmount()).isEqualTo(DEFAULT_DISCOUNT_AMOUNT);
        assertThat(testSales.getSalesAmount()).isEqualTo(DEFAULT_SALES_AMOUNT);
        assertThat(testSales.getGrossAmount()).isEqualTo(DEFAULT_GROSS_AMOUNT);
        assertThat(testSales.getNetAmount()).isEqualTo(DEFAULT_NET_AMOUNT);
        assertThat(testSales.getTaxAmount()).isEqualTo(DEFAULT_TAX_AMOUNT);
        assertThat(testSales.getCostAmount()).isEqualTo(DEFAULT_COST_AMOUNT);
        assertThat(testSales.getStatut()).isEqualTo(DEFAULT_STATUT);
        assertThat(testSales.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testSales.getUpdatedAt()).isEqualTo(DEFAULT_UPDATED_AT);
    }

    @Test
    @Transactional
    public void createSalesWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = salesRepository.findAll().size();

        // Create the Sales with an existing ID
        sales.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        // Validate the Sales in the database
        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkDiscountAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesRepository.findAll().size();
        // set the field null
        sales.setDiscountAmount(null);

        // Create the Sales, which fails.


        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkSalesAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesRepository.findAll().size();
        // set the field null
        sales.setSalesAmount(null);

        // Create the Sales, which fails.


        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGrossAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesRepository.findAll().size();
        // set the field null
        sales.setGrossAmount(null);

        // Create the Sales, which fails.


        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNetAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesRepository.findAll().size();
        // set the field null
        sales.setNetAmount(null);

        // Create the Sales, which fails.


        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTaxAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesRepository.findAll().size();
        // set the field null
        sales.setTaxAmount(null);

        // Create the Sales, which fails.


        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCostAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesRepository.findAll().size();
        // set the field null
        sales.setCostAmount(null);

        // Create the Sales, which fails.


        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkStatutIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesRepository.findAll().size();
        // set the field null
        sales.setStatut(null);

        // Create the Sales, which fails.


        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesRepository.findAll().size();
        // set the field null
        sales.setCreatedAt(null);

        // Create the Sales, which fails.


        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUpdatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesRepository.findAll().size();
        // set the field null
        sales.setUpdatedAt(null);

        // Create the Sales, which fails.


        restSalesMockMvc.perform(post("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllSales() throws Exception {
        // Initialize the database
        salesRepository.saveAndFlush(sales);

        // Get all the salesList
        restSalesMockMvc.perform(get("/api/sales?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(sales.getId().intValue())))
            .andExpect(jsonPath("$.[*].discountAmount").value(hasItem(DEFAULT_DISCOUNT_AMOUNT)))
            .andExpect(jsonPath("$.[*].salesAmount").value(hasItem(DEFAULT_SALES_AMOUNT)))
            .andExpect(jsonPath("$.[*].grossAmount").value(hasItem(DEFAULT_GROSS_AMOUNT)))
            .andExpect(jsonPath("$.[*].netAmount").value(hasItem(DEFAULT_NET_AMOUNT)))
            .andExpect(jsonPath("$.[*].taxAmount").value(hasItem(DEFAULT_TAX_AMOUNT)))
            .andExpect(jsonPath("$.[*].costAmount").value(hasItem(DEFAULT_COST_AMOUNT)))
            .andExpect(jsonPath("$.[*].statut").value(hasItem(DEFAULT_STATUT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }
    
    @Test
    @Transactional
    public void getSales() throws Exception {
        // Initialize the database
        salesRepository.saveAndFlush(sales);

        // Get the sales
        restSalesMockMvc.perform(get("/api/sales/{id}", sales.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(sales.getId().intValue()))
            .andExpect(jsonPath("$.discountAmount").value(DEFAULT_DISCOUNT_AMOUNT))
            .andExpect(jsonPath("$.salesAmount").value(DEFAULT_SALES_AMOUNT))
            .andExpect(jsonPath("$.grossAmount").value(DEFAULT_GROSS_AMOUNT))
            .andExpect(jsonPath("$.netAmount").value(DEFAULT_NET_AMOUNT))
            .andExpect(jsonPath("$.taxAmount").value(DEFAULT_TAX_AMOUNT))
            .andExpect(jsonPath("$.costAmount").value(DEFAULT_COST_AMOUNT))
            .andExpect(jsonPath("$.statut").value(DEFAULT_STATUT.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }
    @Test
    @Transactional
    public void getNonExistingSales() throws Exception {
        // Get the sales
        restSalesMockMvc.perform(get("/api/sales/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateSales() throws Exception {
        // Initialize the database
        salesRepository.saveAndFlush(sales);

        int databaseSizeBeforeUpdate = salesRepository.findAll().size();

        // Update the sales
        Sales updatedSales = salesRepository.findById(sales.getId()).get();
        // Disconnect from session so that the updates on updatedSales are not directly saved in db
        em.detach(updatedSales);
        updatedSales
            .discountAmount(UPDATED_DISCOUNT_AMOUNT)
            .salesAmount(UPDATED_SALES_AMOUNT)
            .grossAmount(UPDATED_GROSS_AMOUNT)
            .netAmount(UPDATED_NET_AMOUNT)
            .taxAmount(UPDATED_TAX_AMOUNT)
            .costAmount(UPDATED_COST_AMOUNT)
            .statut(UPDATED_STATUT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restSalesMockMvc.perform(put("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedSales)))
            .andExpect(status().isOk());

        // Validate the Sales in the database
        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeUpdate);
        Sales testSales = salesList.get(salesList.size() - 1);
        assertThat(testSales.getDiscountAmount()).isEqualTo(UPDATED_DISCOUNT_AMOUNT);
        assertThat(testSales.getSalesAmount()).isEqualTo(UPDATED_SALES_AMOUNT);
        assertThat(testSales.getGrossAmount()).isEqualTo(UPDATED_GROSS_AMOUNT);
        assertThat(testSales.getNetAmount()).isEqualTo(UPDATED_NET_AMOUNT);
        assertThat(testSales.getTaxAmount()).isEqualTo(UPDATED_TAX_AMOUNT);
        assertThat(testSales.getCostAmount()).isEqualTo(UPDATED_COST_AMOUNT);
        assertThat(testSales.getStatut()).isEqualTo(UPDATED_STATUT);
        assertThat(testSales.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testSales.getUpdatedAt()).isEqualTo(UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    public void updateNonExistingSales() throws Exception {
        int databaseSizeBeforeUpdate = salesRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSalesMockMvc.perform(put("/api/sales").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(sales)))
            .andExpect(status().isBadRequest());

        // Validate the Sales in the database
        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteSales() throws Exception {
        // Initialize the database
        salesRepository.saveAndFlush(sales);

        int databaseSizeBeforeDelete = salesRepository.findAll().size();

        // Delete the sales
        restSalesMockMvc.perform(delete("/api/sales/{id}", sales.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Sales> salesList = salesRepository.findAll();
        assertThat(salesList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
