package com.kobe.warehouse.repository.timezone;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "jhi_date_time_wrapper")
public class DateTimeWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;


    @Column(name = "local_date_time")
    private LocalDateTime localDateTime;

    @Column(name = "offset_date_time")
    private OffsetDateTime offsetDateTime;

    @Column(name = "zoned_date_time")
    private ZonedDateTime zonedDateTime;

    @Column(name = "local_time")
    private LocalTime localTime;

    @Column(name = "offset_time")
    private OffsetTime offsetTime;

    @Column(name = "local_date")
    private LocalDate localDate;

  public void setId(Long id) {
        this.id = id;
    }



  public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
        this.offsetDateTime = offsetDateTime;
    }

  public void setZonedDateTime(ZonedDateTime zonedDateTime) {
        this.zonedDateTime = zonedDateTime;
    }

  public void setLocalTime(LocalTime localTime) {
        this.localTime = localTime;
    }

  public void setOffsetTime(OffsetTime offsetTime) {
        this.offsetTime = offsetTime;
    }

  public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DateTimeWrapper dateTimeWrapper = (DateTimeWrapper) o;
        return !(dateTimeWrapper.getId() == null || getId() == null) && Objects.equals(getId(), dateTimeWrapper.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TimeZoneTest{" +
            "id=" + id +
            ", offsetDateTime=" + offsetDateTime +
            ", zonedDateTime=" + zonedDateTime +
            '}';
    }
}
