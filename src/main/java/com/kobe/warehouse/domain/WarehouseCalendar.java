package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "warehouse_calendar")
public class WarehouseCalendar implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id private LocalDate workDay;
  private int workMonth;
  private int workYear;

  public LocalDate getWorkDay() {
    return workDay;
  }

  public WarehouseCalendar setWorkDay(LocalDate workDay) {
    this.workDay = workDay;
    return this;
  }

  public int getWorkMonth() {
    return workMonth;
  }

  public WarehouseCalendar setWorkMonth(int workMonth) {
    this.workMonth = workMonth;
    return this;
  }

  public int getWorkYear() {
    return workYear;
  }

  public WarehouseCalendar setWorkYear(int workYear) {
    this.workYear = workYear;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WarehouseCalendar that = (WarehouseCalendar) o;
    return workDay.equals(that.workDay);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workDay);
  }

  @Override
  public String toString() {
      String sb = "WarehouseCalendar{" + "workDay=" + workDay
          + ", workMonth=" + workMonth
          + ", workYear=" + workYear
          + '}';
    return sb;
  }
}
