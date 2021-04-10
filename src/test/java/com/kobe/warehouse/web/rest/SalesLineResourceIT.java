package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.repository.SalesLineRepository;

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
 * Integration tests for the {@link SalesLineResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class SalesLineResourceIT {

    private static final Integer DEFAULT_QUANTITY_SOLD = 1;
    private static final Integer UPDATED_QUANTITY_SOLD = 2;

    private static final Integer DEFAULT_REGULAR_UNIT_PRICE = 1;
    private static final Integer UPDATED_REGULAR_UNIT_PRICE = 2;

    private static final Integer DEFAULT_DISCOUNT_UNIT_PRICE = 1;
    private static final Integer UPDATED_DISCOUNT_UNIT_PRICE = 2;

    private static final Integer DEFAULT_NET_UNIT_PRICE = 1;
    private static final Integer UPDATED_NET_UNIT_PRICE = 2;

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

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private SalesLineRepository salesLineRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSalesLineMockMvc;

    private SalesLine salesLine;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SalesLine createEntity(EntityManager em) {
        SalesLine salesLine = new SalesLine()
            .quantitySold(DEFAULT_QUANTITY_SOLD)
            .regularUnitPrice(DEFAULT_REGULAR_UNIT_PRICE)
            .discountUnitPrice(DEFAULT_DISCOUNT_UNIT_PRICE)
            .netUnitPrice(DEFAULT_NET_UNIT_PRICE)
            .discountAmount(DEFAULT_DISCOUNT_AMOUNT)
            .salesAmount(DEFAULT_SALES_AMOUNT)
            .grossAmount(DEFAULT_GROSS_AMOUNT)
            .netAmount(DEFAULT_NET_AMOUNT)
            .taxAmount(DEFAULT_TAX_AMOUNT)
            .costAmount(DEFAULT_COST_AMOUNT)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
        return salesLine;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SalesLine createUpdatedEntity(EntityManager em) {
        SalesLine salesLine = new SalesLine()
            .quantitySold(UPDATED_QUANTITY_SOLD)
            .regularUnitPrice(UPDATED_REGULAR_UNIT_PRICE)
            .discountUnitPrice(UPDATED_DISCOUNT_UNIT_PRICE)
            .netUnitPrice(UPDATED_NET_UNIT_PRICE)
            .discountAmount(UPDATED_DISCOUNT_AMOUNT)
            .salesAmount(UPDATED_SALES_AMOUNT)
            .grossAmount(UPDATED_GROSS_AMOUNT)
            .netAmount(UPDATED_NET_AMOUNT)
            .taxAmount(UPDATED_TAX_AMOUNT)
            .costAmount(UPDATED_COST_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        return salesLine;
    }

    @BeforeEach
    public void initTest() {
        salesLine = createEntity(em);
    }

    @Test
    @Transactional
    public void createSalesLine() throws Exception {
        int databaseSizeBeforeCreate = salesLineRepository.findAll().size();
        // Create the SalesLine
        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isCreated());

        // Validate the SalesLine in the database
        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeCreate + 1);
        SalesLine testSalesLine = salesLineList.get(salesLineList.size() - 1);
        assertThat(testSalesLine.getQuantitySold()).isEqualTo(DEFAULT_QUANTITY_SOLD);
        assertThat(testSalesLine.getRegularUnitPrice()).isEqualTo(DEFAULT_REGULAR_UNIT_PRICE);
        assertThat(testSalesLine.getDiscountUnitPrice()).isEqualTo(DEFAULT_DISCOUNT_UNIT_PRICE);
        assertThat(testSalesLine.getNetUnitPrice()).isEqualTo(DEFAULT_NET_UNIT_PRICE);
        assertThat(testSalesLine.getDiscountAmount()).isEqualTo(DEFAULT_DISCOUNT_AMOUNT);
        assertThat(testSalesLine.getSalesAmount()).isEqualTo(DEFAULT_SALES_AMOUNT);
        assertThat(testSalesLine.getGrossAmount()).isEqualTo(DEFAULT_GROSS_AMOUNT);
        assertThat(testSalesLine.getNetAmount()).isEqualTo(DEFAULT_NET_AMOUNT);
        assertThat(testSalesLine.getTaxAmount()).isEqualTo(DEFAULT_TAX_AMOUNT);
        assertThat(testSalesLine.getCostAmount()).isEqualTo(DEFAULT_COST_AMOUNT);
        assertThat(testSalesLine.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testSalesLine.getUpdatedAt()).isEqualTo(DEFAULT_UPDATED_AT);
    }

    @Test
    @Transactional
    public void createSalesLineWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = salesLineRepository.findAll().size();

        // Create the SalesLine with an existing ID
        salesLine.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        // Validate the SalesLine in the database
        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkQuantitySoldIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setQuantitySold(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkRegularUnitPriceIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setRegularUnitPrice(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkDiscountUnitPriceIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setDiscountUnitPrice(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNetUnitPriceIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setNetUnitPrice(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkDiscountAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setDiscountAmount(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkSalesAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setSalesAmount(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGrossAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setGrossAmount(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNetAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setNetAmount(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTaxAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setTaxAmount(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCostAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setCostAmount(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setCreatedAt(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUpdatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = salesLineRepository.findAll().size();
        // set the field null
        salesLine.setUpdatedAt(null);

        // Create the SalesLine, which fails.


        restSalesLineMockMvc.perform(post("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllSalesLines() throws Exception {
        // Initialize the database
        salesLineRepository.saveAndFlush(salesLine);

        // Get all the salesLineList
        restSalesLineMockMvc.perform(get("/api/sales-lines?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(salesLine.getId().intValue())))
            .andExpect(jsonPath("$.[*].quantitySold").value(hasItem(DEFAULT_QUANTITY_SOLD)))
            .andExpect(jsonPath("$.[*].regularUnitPrice").value(hasItem(DEFAULT_REGULAR_UNIT_PRICE)))
            .andExpect(jsonPath("$.[*].discountUnitPrice").value(hasItem(DEFAULT_DISCOUNT_UNIT_PRICE)))
            .andExpect(jsonPath("$.[*].netUnitPrice").value(hasItem(DEFAULT_NET_UNIT_PRICE)))
            .andExpect(jsonPath("$.[*].discountAmount").value(hasItem(DEFAULT_DISCOUNT_AMOUNT)))
            .andExpect(jsonPath("$.[*].salesAmount").value(hasItem(DEFAULT_SALES_AMOUNT)))
            .andExpect(jsonPath("$.[*].grossAmount").value(hasItem(DEFAULT_GROSS_AMOUNT)))
            .andExpect(jsonPath("$.[*].netAmount").value(hasItem(DEFAULT_NET_AMOUNT)))
            .andExpect(jsonPath("$.[*].taxAmount").value(hasItem(DEFAULT_TAX_AMOUNT)))
            .andExpect(jsonPath("$.[*].costAmount").value(hasItem(DEFAULT_COST_AMOUNT)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }
    
    @Test
    @Transactional
    public void getSalesLine() throws Exception {
        // Initialize the database
        salesLineRepository.saveAndFlush(salesLine);

        // Get the salesLine
        restSalesLineMockMvc.perform(get("/api/sales-lines/{id}", salesLine.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(salesLine.getId().intValue()))
            .andExpect(jsonPath("$.quantitySold").value(DEFAULT_QUANTITY_SOLD))
            .andExpect(jsonPath("$.regularUnitPrice").value(DEFAULT_REGULAR_UNIT_PRICE))
            .andExpect(jsonPath("$.discountUnitPrice").value(DEFAULT_DISCOUNT_UNIT_PRICE))
            .andExpect(jsonPath("$.netUnitPrice").value(DEFAULT_NET_UNIT_PRICE))
            .andExpect(jsonPath("$.discountAmount").value(DEFAULT_DISCOUNT_AMOUNT))
            .andExpect(jsonPath("$.salesAmount").value(DEFAULT_SALES_AMOUNT))
            .andExpect(jsonPath("$.grossAmount").value(DEFAULT_GROSS_AMOUNT))
            .andExpect(jsonPath("$.netAmount").value(DEFAULT_NET_AMOUNT))
            .andExpect(jsonPath("$.taxAmount").value(DEFAULT_TAX_AMOUNT))
            .andExpect(jsonPath("$.costAmount").value(DEFAULT_COST_AMOUNT))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }
    @Test
    @Transactional
    public void getNonExistingSalesLine() throws Exception {
        // Get the salesLine
        restSalesLineMockMvc.perform(get("/api/sales-lines/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateSalesLine() throws Exception {
        // Initialize the database
        salesLineRepository.saveAndFlush(salesLine);

        int databaseSizeBeforeUpdate = salesLineRepository.findAll().size();

        // Update the salesLine
        SalesLine updatedSalesLine = salesLineRepository.findById(salesLine.getId()).get();
        // Disconnect from session so that the updates on updatedSalesLine are not directly saved in db
        em.detach(updatedSalesLine);
        updatedSalesLine
            .quantitySold(UPDATED_QUANTITY_SOLD)
            .regularUnitPrice(UPDATED_REGULAR_UNIT_PRICE)
            .discountUnitPrice(UPDATED_DISCOUNT_UNIT_PRICE)
            .netUnitPrice(UPDATED_NET_UNIT_PRICE)
            .discountAmount(UPDATED_DISCOUNT_AMOUNT)
            .salesAmount(UPDATED_SALES_AMOUNT)
            .grossAmount(UPDATED_GROSS_AMOUNT)
            .netAmount(UPDATED_NET_AMOUNT)
            .taxAmount(UPDATED_TAX_AMOUNT)
            .costAmount(UPDATED_COST_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restSalesLineMockMvc.perform(put("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedSalesLine)))
            .andExpect(status().isOk());

        // Validate the SalesLine in the database
        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeUpdate);
        SalesLine testSalesLine = salesLineList.get(salesLineList.size() - 1);
        assertThat(testSalesLine.getQuantitySold()).isEqualTo(UPDATED_QUANTITY_SOLD);
        assertThat(testSalesLine.getRegularUnitPrice()).isEqualTo(UPDATED_REGULAR_UNIT_PRICE);
        assertThat(testSalesLine.getDiscountUnitPrice()).isEqualTo(UPDATED_DISCOUNT_UNIT_PRICE);
        assertThat(testSalesLine.getNetUnitPrice()).isEqualTo(UPDATED_NET_UNIT_PRICE);
        assertThat(testSalesLine.getDiscountAmount()).isEqualTo(UPDATED_DISCOUNT_AMOUNT);
        assertThat(testSalesLine.getSalesAmount()).isEqualTo(UPDATED_SALES_AMOUNT);
        assertThat(testSalesLine.getGrossAmount()).isEqualTo(UPDATED_GROSS_AMOUNT);
        assertThat(testSalesLine.getNetAmount()).isEqualTo(UPDATED_NET_AMOUNT);
        assertThat(testSalesLine.getTaxAmount()).isEqualTo(UPDATED_TAX_AMOUNT);
        assertThat(testSalesLine.getCostAmount()).isEqualTo(UPDATED_COST_AMOUNT);
        assertThat(testSalesLine.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testSalesLine.getUpdatedAt()).isEqualTo(UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    public void updateNonExistingSalesLine() throws Exception {
        int databaseSizeBeforeUpdate = salesLineRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSalesLineMockMvc.perform(put("/api/sales-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(salesLine)))
            .andExpect(status().isBadRequest());

        // Validate the SalesLine in the database
        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteSalesLine() throws Exception {
        // Initialize the database
        salesLineRepository.saveAndFlush(salesLine);

        int databaseSizeBeforeDelete = salesLineRepository.findAll().size();

        // Delete the salesLine
        restSalesLineMockMvc.perform(delete("/api/sales-lines/{id}", salesLine.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<SalesLine> salesLineList = salesLineRepository.findAll();
        assertThat(salesLineList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
