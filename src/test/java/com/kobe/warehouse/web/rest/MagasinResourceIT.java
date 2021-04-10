package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.repository.MagasinRepository;

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
 * Integration tests for the {@link MagasinResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class MagasinResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_PHONE = "AAAAAAAAAA";
    private static final String UPDATED_PHONE = "BBBBBBBBBB";

    private static final String DEFAULT_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_ADDRESS = "BBBBBBBBBB";

    private static final String DEFAULT_NOTE = "AAAAAAAAAA";
    private static final String UPDATED_NOTE = "BBBBBBBBBB";

    private static final String DEFAULT_REGISTRE = "AAAAAAAAAA";
    private static final String UPDATED_REGISTRE = "BBBBBBBBBB";

    @Autowired
    private MagasinRepository magasinRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMagasinMockMvc;

    private Magasin magasin;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Magasin createEntity(EntityManager em) {
        Magasin magasin = new Magasin()
            .name(DEFAULT_NAME)
            .phone(DEFAULT_PHONE)
            .address(DEFAULT_ADDRESS)
            .note(DEFAULT_NOTE)
            .registre(DEFAULT_REGISTRE);
        return magasin;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Magasin createUpdatedEntity(EntityManager em) {
        Magasin magasin = new Magasin()
            .name(UPDATED_NAME)
            .phone(UPDATED_PHONE)
            .address(UPDATED_ADDRESS)
            .note(UPDATED_NOTE)
            .registre(UPDATED_REGISTRE);
        return magasin;
    }

    @BeforeEach
    public void initTest() {
        magasin = createEntity(em);
    }

    @Test
    @Transactional
    public void createMagasin() throws Exception {
        int databaseSizeBeforeCreate = magasinRepository.findAll().size();
        // Create the Magasin
        restMagasinMockMvc.perform(post("/api/magasins").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(magasin)))
            .andExpect(status().isCreated());

        // Validate the Magasin in the database
        List<Magasin> magasinList = magasinRepository.findAll();
        assertThat(magasinList).hasSize(databaseSizeBeforeCreate + 1);
        Magasin testMagasin = magasinList.get(magasinList.size() - 1);
        assertThat(testMagasin.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testMagasin.getPhone()).isEqualTo(DEFAULT_PHONE);
        assertThat(testMagasin.getAddress()).isEqualTo(DEFAULT_ADDRESS);
        assertThat(testMagasin.getNote()).isEqualTo(DEFAULT_NOTE);
        assertThat(testMagasin.getRegistre()).isEqualTo(DEFAULT_REGISTRE);
    }

    @Test
    @Transactional
    public void createMagasinWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = magasinRepository.findAll().size();

        // Create the Magasin with an existing ID
        magasin.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMagasinMockMvc.perform(post("/api/magasins").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(magasin)))
            .andExpect(status().isBadRequest());

        // Validate the Magasin in the database
        List<Magasin> magasinList = magasinRepository.findAll();
        assertThat(magasinList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = magasinRepository.findAll().size();
        // set the field null
        magasin.setName(null);

        // Create the Magasin, which fails.


        restMagasinMockMvc.perform(post("/api/magasins").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(magasin)))
            .andExpect(status().isBadRequest());

        List<Magasin> magasinList = magasinRepository.findAll();
        assertThat(magasinList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkPhoneIsRequired() throws Exception {
        int databaseSizeBeforeTest = magasinRepository.findAll().size();
        // set the field null
        magasin.setPhone(null);

        // Create the Magasin, which fails.


        restMagasinMockMvc.perform(post("/api/magasins").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(magasin)))
            .andExpect(status().isBadRequest());

        List<Magasin> magasinList = magasinRepository.findAll();
        assertThat(magasinList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllMagasins() throws Exception {
        // Initialize the database
        magasinRepository.saveAndFlush(magasin);

        // Get all the magasinList
        restMagasinMockMvc.perform(get("/api/magasins?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(magasin.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE)))
            .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
            .andExpect(jsonPath("$.[*].note").value(hasItem(DEFAULT_NOTE)))
            .andExpect(jsonPath("$.[*].registre").value(hasItem(DEFAULT_REGISTRE)));
    }
    
    @Test
    @Transactional
    public void getMagasin() throws Exception {
        // Initialize the database
        magasinRepository.saveAndFlush(magasin);

        // Get the magasin
        restMagasinMockMvc.perform(get("/api/magasins/{id}", magasin.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(magasin.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.phone").value(DEFAULT_PHONE))
            .andExpect(jsonPath("$.address").value(DEFAULT_ADDRESS))
            .andExpect(jsonPath("$.note").value(DEFAULT_NOTE))
            .andExpect(jsonPath("$.registre").value(DEFAULT_REGISTRE));
    }
    @Test
    @Transactional
    public void getNonExistingMagasin() throws Exception {
        // Get the magasin
        restMagasinMockMvc.perform(get("/api/magasins/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMagasin() throws Exception {
        // Initialize the database
        magasinRepository.saveAndFlush(magasin);

        int databaseSizeBeforeUpdate = magasinRepository.findAll().size();

        // Update the magasin
        Magasin updatedMagasin = magasinRepository.findById(magasin.getId()).get();
        // Disconnect from session so that the updates on updatedMagasin are not directly saved in db
        em.detach(updatedMagasin);
        updatedMagasin
            .name(UPDATED_NAME)
            .phone(UPDATED_PHONE)
            .address(UPDATED_ADDRESS)
            .note(UPDATED_NOTE)
            .registre(UPDATED_REGISTRE);

        restMagasinMockMvc.perform(put("/api/magasins").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedMagasin)))
            .andExpect(status().isOk());

        // Validate the Magasin in the database
        List<Magasin> magasinList = magasinRepository.findAll();
        assertThat(magasinList).hasSize(databaseSizeBeforeUpdate);
        Magasin testMagasin = magasinList.get(magasinList.size() - 1);
        assertThat(testMagasin.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testMagasin.getPhone()).isEqualTo(UPDATED_PHONE);
        assertThat(testMagasin.getAddress()).isEqualTo(UPDATED_ADDRESS);
        assertThat(testMagasin.getNote()).isEqualTo(UPDATED_NOTE);
        assertThat(testMagasin.getRegistre()).isEqualTo(UPDATED_REGISTRE);
    }

    @Test
    @Transactional
    public void updateNonExistingMagasin() throws Exception {
        int databaseSizeBeforeUpdate = magasinRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMagasinMockMvc.perform(put("/api/magasins").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(magasin)))
            .andExpect(status().isBadRequest());

        // Validate the Magasin in the database
        List<Magasin> magasinList = magasinRepository.findAll();
        assertThat(magasinList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteMagasin() throws Exception {
        // Initialize the database
        magasinRepository.saveAndFlush(magasin);

        int databaseSizeBeforeDelete = magasinRepository.findAll().size();

        // Delete the magasin
        restMagasinMockMvc.perform(delete("/api/magasins/{id}", magasin.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Magasin> magasinList = magasinRepository.findAll();
        assertThat(magasinList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
