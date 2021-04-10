package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.repository.OrderLineRepository;

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
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link OrderLineResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class OrderLineResourceIT {

    private static final LocalDate DEFAULT_RECEIPT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_RECEIPT_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final Integer DEFAULT_QUANTITY_RECEIVED = 1;
    private static final Integer UPDATED_QUANTITY_RECEIVED = 2;

    private static final Integer DEFAULT_QUANTITY_REQUESTED = 1;
    private static final Integer UPDATED_QUANTITY_REQUESTED = 2;

    private static final Integer DEFAULT_QUANTITY_RETURNED = 1;
    private static final Integer UPDATED_QUANTITY_RETURNED = 2;

    private static final Integer DEFAULT_DISCOUNT_AMOUNT = 1;
    private static final Integer UPDATED_DISCOUNT_AMOUNT = 2;

    private static final Integer DEFAULT_ORDER_AMOUNT = 1;
    private static final Integer UPDATED_ORDER_AMOUNT = 2;

    private static final Integer DEFAULT_GROSS_AMOUNT = 1;
    private static final Integer UPDATED_GROSS_AMOUNT = 2;

    private static final Integer DEFAULT_NET_AMOUNT = 1;
    private static final Integer UPDATED_NET_AMOUNT = 2;

    private static final Integer DEFAULT_TAX_AMOUNT = 1;
    private static final Integer UPDATED_TAX_AMOUNT = 2;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_COST_AMOUNT = 1;
    private static final Integer UPDATED_COST_AMOUNT = 2;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrderLineMockMvc;

    private OrderLine orderLine;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderLine createEntity(EntityManager em) {
        OrderLine orderLine = new OrderLine()
            .receiptDate(DEFAULT_RECEIPT_DATE)
            .quantityReceived(DEFAULT_QUANTITY_RECEIVED)
            .quantityRequested(DEFAULT_QUANTITY_REQUESTED)
            .quantityReturned(DEFAULT_QUANTITY_RETURNED)
            .discountAmount(DEFAULT_DISCOUNT_AMOUNT)
            .orderAmount(DEFAULT_ORDER_AMOUNT)
            .grossAmount(DEFAULT_GROSS_AMOUNT)
            .netAmount(DEFAULT_NET_AMOUNT)
            .taxAmount(DEFAULT_TAX_AMOUNT)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT)
            .costAmount(DEFAULT_COST_AMOUNT);
        return orderLine;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderLine createUpdatedEntity(EntityManager em) {
        OrderLine orderLine = new OrderLine()
            .receiptDate(UPDATED_RECEIPT_DATE)
            .quantityReceived(UPDATED_QUANTITY_RECEIVED)
            .quantityRequested(UPDATED_QUANTITY_REQUESTED)
            .quantityReturned(UPDATED_QUANTITY_RETURNED)
            .discountAmount(UPDATED_DISCOUNT_AMOUNT)
            .orderAmount(UPDATED_ORDER_AMOUNT)
            .grossAmount(UPDATED_GROSS_AMOUNT)
            .netAmount(UPDATED_NET_AMOUNT)
            .taxAmount(UPDATED_TAX_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .costAmount(UPDATED_COST_AMOUNT);
        return orderLine;
    }

    @BeforeEach
    public void initTest() {
        orderLine = createEntity(em);
    }

    @Test
    @Transactional
    public void createOrderLine() throws Exception {
        int databaseSizeBeforeCreate = orderLineRepository.findAll().size();
        // Create the OrderLine
        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isCreated());

        // Validate the OrderLine in the database
        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeCreate + 1);
        OrderLine testOrderLine = orderLineList.get(orderLineList.size() - 1);
        assertThat(testOrderLine.getReceiptDate()).isEqualTo(DEFAULT_RECEIPT_DATE);
        assertThat(testOrderLine.getQuantityReceived()).isEqualTo(DEFAULT_QUANTITY_RECEIVED);
        assertThat(testOrderLine.getQuantityRequested()).isEqualTo(DEFAULT_QUANTITY_REQUESTED);
        assertThat(testOrderLine.getQuantityReturned()).isEqualTo(DEFAULT_QUANTITY_RETURNED);
        assertThat(testOrderLine.getDiscountAmount()).isEqualTo(DEFAULT_DISCOUNT_AMOUNT);
        assertThat(testOrderLine.getOrderAmount()).isEqualTo(DEFAULT_ORDER_AMOUNT);
        assertThat(testOrderLine.getGrossAmount()).isEqualTo(DEFAULT_GROSS_AMOUNT);
        assertThat(testOrderLine.getNetAmount()).isEqualTo(DEFAULT_NET_AMOUNT);
        assertThat(testOrderLine.getTaxAmount()).isEqualTo(DEFAULT_TAX_AMOUNT);
        assertThat(testOrderLine.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testOrderLine.getUpdatedAt()).isEqualTo(DEFAULT_UPDATED_AT);
        assertThat(testOrderLine.getCostAmount()).isEqualTo(DEFAULT_COST_AMOUNT);
    }

    @Test
    @Transactional
    public void createOrderLineWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = orderLineRepository.findAll().size();

        // Create the OrderLine with an existing ID
        orderLine.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        // Validate the OrderLine in the database
        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkReceiptDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setReceiptDate(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantityReceivedIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setQuantityReceived(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantityRequestedIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setQuantityRequested(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantityReturnedIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setQuantityReturned(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkDiscountAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setDiscountAmount(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkOrderAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setOrderAmount(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGrossAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setGrossAmount(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNetAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setNetAmount(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTaxAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setTaxAmount(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setCreatedAt(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUpdatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setUpdatedAt(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCostAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderLineRepository.findAll().size();
        // set the field null
        orderLine.setCostAmount(null);

        // Create the OrderLine, which fails.


        restOrderLineMockMvc.perform(post("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllOrderLines() throws Exception {
        // Initialize the database
        orderLineRepository.saveAndFlush(orderLine);

        // Get all the orderLineList
        restOrderLineMockMvc.perform(get("/api/order-lines?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderLine.getId().intValue())))
            .andExpect(jsonPath("$.[*].receiptDate").value(hasItem(DEFAULT_RECEIPT_DATE.toString())))
            .andExpect(jsonPath("$.[*].quantityReceived").value(hasItem(DEFAULT_QUANTITY_RECEIVED)))
            .andExpect(jsonPath("$.[*].quantityRequested").value(hasItem(DEFAULT_QUANTITY_REQUESTED)))
            .andExpect(jsonPath("$.[*].quantityReturned").value(hasItem(DEFAULT_QUANTITY_RETURNED)))
            .andExpect(jsonPath("$.[*].discountAmount").value(hasItem(DEFAULT_DISCOUNT_AMOUNT)))
            .andExpect(jsonPath("$.[*].orderAmount").value(hasItem(DEFAULT_ORDER_AMOUNT)))
            .andExpect(jsonPath("$.[*].grossAmount").value(hasItem(DEFAULT_GROSS_AMOUNT)))
            .andExpect(jsonPath("$.[*].netAmount").value(hasItem(DEFAULT_NET_AMOUNT)))
            .andExpect(jsonPath("$.[*].taxAmount").value(hasItem(DEFAULT_TAX_AMOUNT)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].costAmount").value(hasItem(DEFAULT_COST_AMOUNT)));
    }
    
    @Test
    @Transactional
    public void getOrderLine() throws Exception {
        // Initialize the database
        orderLineRepository.saveAndFlush(orderLine);

        // Get the orderLine
        restOrderLineMockMvc.perform(get("/api/order-lines/{id}", orderLine.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(orderLine.getId().intValue()))
            .andExpect(jsonPath("$.receiptDate").value(DEFAULT_RECEIPT_DATE.toString()))
            .andExpect(jsonPath("$.quantityReceived").value(DEFAULT_QUANTITY_RECEIVED))
            .andExpect(jsonPath("$.quantityRequested").value(DEFAULT_QUANTITY_REQUESTED))
            .andExpect(jsonPath("$.quantityReturned").value(DEFAULT_QUANTITY_RETURNED))
            .andExpect(jsonPath("$.discountAmount").value(DEFAULT_DISCOUNT_AMOUNT))
            .andExpect(jsonPath("$.orderAmount").value(DEFAULT_ORDER_AMOUNT))
            .andExpect(jsonPath("$.grossAmount").value(DEFAULT_GROSS_AMOUNT))
            .andExpect(jsonPath("$.netAmount").value(DEFAULT_NET_AMOUNT))
            .andExpect(jsonPath("$.taxAmount").value(DEFAULT_TAX_AMOUNT))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()))
            .andExpect(jsonPath("$.costAmount").value(DEFAULT_COST_AMOUNT));
    }
    @Test
    @Transactional
    public void getNonExistingOrderLine() throws Exception {
        // Get the orderLine
        restOrderLineMockMvc.perform(get("/api/order-lines/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateOrderLine() throws Exception {
        // Initialize the database
        orderLineRepository.saveAndFlush(orderLine);

        int databaseSizeBeforeUpdate = orderLineRepository.findAll().size();

        // Update the orderLine
        OrderLine updatedOrderLine = orderLineRepository.findById(orderLine.getId()).get();
        // Disconnect from session so that the updates on updatedOrderLine are not directly saved in db
        em.detach(updatedOrderLine);
        updatedOrderLine
            .receiptDate(UPDATED_RECEIPT_DATE)
            .quantityReceived(UPDATED_QUANTITY_RECEIVED)
            .quantityRequested(UPDATED_QUANTITY_REQUESTED)
            .quantityReturned(UPDATED_QUANTITY_RETURNED)
            .discountAmount(UPDATED_DISCOUNT_AMOUNT)
            .orderAmount(UPDATED_ORDER_AMOUNT)
            .grossAmount(UPDATED_GROSS_AMOUNT)
            .netAmount(UPDATED_NET_AMOUNT)
            .taxAmount(UPDATED_TAX_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .costAmount(UPDATED_COST_AMOUNT);

        restOrderLineMockMvc.perform(put("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedOrderLine)))
            .andExpect(status().isOk());

        // Validate the OrderLine in the database
        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeUpdate);
        OrderLine testOrderLine = orderLineList.get(orderLineList.size() - 1);
        assertThat(testOrderLine.getReceiptDate()).isEqualTo(UPDATED_RECEIPT_DATE);
        assertThat(testOrderLine.getQuantityReceived()).isEqualTo(UPDATED_QUANTITY_RECEIVED);
        assertThat(testOrderLine.getQuantityRequested()).isEqualTo(UPDATED_QUANTITY_REQUESTED);
        assertThat(testOrderLine.getQuantityReturned()).isEqualTo(UPDATED_QUANTITY_RETURNED);
        assertThat(testOrderLine.getDiscountAmount()).isEqualTo(UPDATED_DISCOUNT_AMOUNT);
        assertThat(testOrderLine.getOrderAmount()).isEqualTo(UPDATED_ORDER_AMOUNT);
        assertThat(testOrderLine.getGrossAmount()).isEqualTo(UPDATED_GROSS_AMOUNT);
        assertThat(testOrderLine.getNetAmount()).isEqualTo(UPDATED_NET_AMOUNT);
        assertThat(testOrderLine.getTaxAmount()).isEqualTo(UPDATED_TAX_AMOUNT);
        assertThat(testOrderLine.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testOrderLine.getUpdatedAt()).isEqualTo(UPDATED_UPDATED_AT);
        assertThat(testOrderLine.getCostAmount()).isEqualTo(UPDATED_COST_AMOUNT);
    }

    @Test
    @Transactional
    public void updateNonExistingOrderLine() throws Exception {
        int databaseSizeBeforeUpdate = orderLineRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderLineMockMvc.perform(put("/api/order-lines").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(orderLine)))
            .andExpect(status().isBadRequest());

        // Validate the OrderLine in the database
        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteOrderLine() throws Exception {
        // Initialize the database
        orderLineRepository.saveAndFlush(orderLine);

        int databaseSizeBeforeDelete = orderLineRepository.findAll().size();

        // Delete the orderLine
        restOrderLineMockMvc.perform(delete("/api/order-lines/{id}", orderLine.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<OrderLine> orderLineList = orderLineRepository.findAll();
        assertThat(orderLineList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
