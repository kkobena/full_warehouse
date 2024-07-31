package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.repository.GroupeTiersPayantRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.web.rest.errors.GenericError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class GroupeTiersPayantService {

    private final Logger log = LoggerFactory.getLogger(GroupeTiersPayantService.class);
    private final GroupeTiersPayantRepository groupeTiersPayantRepository;
    private final TiersPayantRepository tiersPayantRepository;

    public GroupeTiersPayantService(GroupeTiersPayantRepository groupeTiersPayantRepository, TiersPayantRepository tiersPayantRepository) {
        this.groupeTiersPayantRepository = groupeTiersPayantRepository;
        this.tiersPayantRepository = tiersPayantRepository;
    }

    public GroupeTiersPayant create(GroupeTiersPayant groupeTiersPayant) throws GenericError {
        Optional<GroupeTiersPayant> groupeTiersPayantOptional = groupeTiersPayantRepository.findOneByName(groupeTiersPayant.getName());
        if (groupeTiersPayantOptional.isPresent()) {
            throw new GenericError("Il existe dejà  un groupe avec le même nom",
                "groupeTiersPayantExistant");
        }
        GroupeTiersPayant tiersPayant = new GroupeTiersPayant();
        tiersPayant.setAdresse(groupeTiersPayant.getAdresse());
        tiersPayant.setName(groupeTiersPayant.getName());
        tiersPayant.setTelephone(groupeTiersPayant.getTelephone());
        tiersPayant.setTelephoneFixe(groupeTiersPayant.getTelephoneFixe());
        return groupeTiersPayantRepository.save(tiersPayant);
    }

    public GroupeTiersPayant update(GroupeTiersPayant groupeTiersPayant) throws GenericError {
        GroupeTiersPayant tiersPayant = groupeTiersPayantRepository.getReferenceById(groupeTiersPayant.getId());
        Optional<GroupeTiersPayant> groupeTiersPayantOptional = groupeTiersPayantRepository.findOneByName(groupeTiersPayant.getName());
        if (groupeTiersPayantOptional.isPresent() && groupeTiersPayantOptional.get().getId() != tiersPayant.getId()) {
            throw new GenericError("Il existe dejà  un groupe avec le même nom",
                "groupeTiersPayantExistant");
        }
        tiersPayant.setAdresse(groupeTiersPayant.getAdresse());
        tiersPayant.setName(groupeTiersPayant.getName());
        tiersPayant.setTelephone(groupeTiersPayant.getTelephone());
        tiersPayant.setTelephoneFixe(groupeTiersPayant.getTelephoneFixe());
        return groupeTiersPayantRepository.save(tiersPayant);
    }

    @Transactional(readOnly = true)
    public List<GroupeTiersPayant> list(String search) {
        if (StringUtils.isEmpty(search)) {
            return groupeTiersPayantRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }
        return groupeTiersPayantRepository.findAll(
            groupeTiersPayantRepository.specialisationQueryString(search + "%"),
            Sort.by(Sort.Direction.ASC, "name")
        );
    }

    public void delete(Long id) throws GenericError {
        List<TiersPayant> tiersPayants = tiersPayantRepository.findAllByGroupeTiersPayantId(id);
        if (!tiersPayants.isEmpty()) {
            throw new GenericError("Il  y'a des tierspants associés à ce groupe",
                "groupeTiersPayantAssocies");
        }
        groupeTiersPayantRepository.deleteById(id);
    }

    public ResponseDTO importation(InputStream inputStream) {
        AtomicInteger count = new AtomicInteger();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader().parse(br);
            records.forEach(record -> {
                GroupeTiersPayant tiersPayant = new GroupeTiersPayant();
                tiersPayant.setName(record.get(0));
                tiersPayant.setAdresse(record.get(1));
                tiersPayant.setTelephone(record.get(2));
                create(tiersPayant);
                count.incrementAndGet();
            });
        } catch (IOException e) {
            log.debug("importation : {}", e);
        }

        return new ResponseDTO().size(count.get());
    }

    public Optional<GroupeTiersPayant> getOne(Long id) {
        return groupeTiersPayantRepository.findById(id);
    }

    public Optional<GroupeTiersPayant> getOneByName(String name) {
        return groupeTiersPayantRepository.findOneByName(name);
    }
}
