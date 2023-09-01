package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.service.dto.records.AchatRecord;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import javax.persistence.Tuple;

public final class AchatStatQueryBuilder {
  public static final String ACHAT_QUERY =
      """
SELECT SUM(d.discount_amount) AS discount_amount, SUM(d.receipt_amount) AS receipt_amount,SUM(d.net_amount) AS net_amount,SUM(d.tax_amount) AS tax_amount,COUNT(d.id) AS achat_count FROM delivery_receipt d
WHERE d.receipt_date BETWEEN  ?1 AND ?2 AND
d.receipt_status IN (?3)
""";

  public static AchatRecord build(Tuple tuple) {
    if (Objects.isNull(tuple.get("receipt_amount", BigDecimal.class))) return null;
    return new AchatRecord(
        tuple.get("receipt_amount", BigDecimal.class),
        tuple.get("discount_amount", BigDecimal.class),
        tuple.get("net_amount", BigDecimal.class),
        tuple.get("tax_amount", BigDecimal.class),
        tuple.get("achat_count", BigInteger.class));
  }
}
