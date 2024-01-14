package com.kobe.warehouse.service.stat;

import com.kobe.warehouse.service.dto.OrderBy;
import com.kobe.warehouse.service.dto.ProduitRecordParamDTO;
import com.kobe.warehouse.service.dto.builder.ProductStatQueryBuilder;
import com.kobe.warehouse.service.dto.builder.QueryBuilderConstant;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

public interface ProductStatService extends CommonStatService {
  List<ProduitAuditingState> fetchProduitDailyTransaction(
      ProduitAuditingParam produitAuditingParam);

  List<ProductStatRecord> fetchProductStat(ProduitRecordParamDTO produitRecordParam);

  List<ProductStatParetoRecord> fetch20x80(ProduitRecordParamDTO produitRecordParam);

  default String buildLikeStatement(ProduitRecordParamDTO produitRecordParam) {
    if (StringUtils.hasLength(produitRecordParam.getSearch())) {
      String search = produitRecordParam.getSearch() + "%";
      return String.format(ProductStatQueryBuilder.LIKE_STATEMENT, search, search, search);
    }
    return "";
  }

  default String buildOrderByStatement(ProduitRecordParamDTO produitRecordParam) {
    if (Objects.nonNull(produitRecordParam.getOrder())
        && produitRecordParam.getOrder() == OrderBy.AMOUNT) {
      return String.format(ProductStatQueryBuilder.ORDER_BY_STATEMENT, OrderBy.AMOUNT.getValue());
    }
    return String.format(
        ProductStatQueryBuilder.ORDER_BY_STATEMENT, OrderBy.QUANTITY_SOLD.getValue());
  }

  default String buildLimitStatement(ProduitRecordParamDTO produitRecordParam) {
    return String.format(
        ProductStatQueryBuilder.LIMIT_STATEMENT,
        produitRecordParam.getStart(),
        produitRecordParam.getLimit());
  }

  default String buildPrduduitQuery(ProduitRecordParamDTO produitRecordParam) {
    String query =
        ProductStatQueryBuilder.PRODUIT_QUERY
            .replace(QueryBuilderConstant.LIKE_STATEMENT, buildLikeStatement(produitRecordParam))
            .replace(
                QueryBuilderConstant.ORDER_BY_STATEMENT, buildOrderByStatement(produitRecordParam))
            .replace(QueryBuilderConstant.LIMIT_STATEMENT, buildLimitStatement(produitRecordParam));
    return buildQuery(query, produitRecordParam);
  }

  default String buildPerotoQuery(ProduitRecordParamDTO produitRecordParam) {
    String query =
        ProductStatQueryBuilder.PARETO_20x80_QUERY.replace(
                QueryBuilderConstant.QUANTITY_QUERY_STATEMENT,
                buildParetoQuantityQuey(produitRecordParam))
            .replace(
                QueryBuilderConstant.AMOUNT_QUERY_STATEMENT,
                buildParetoAmountQuey(produitRecordParam));

    return buildQuery(query, produitRecordParam);
  }

  default String buildParetoQuantityQuey(ProduitRecordParamDTO produitRecordParam) {
    return this.buildQuery(ProductStatQueryBuilder.TOTAL_QUNATITY_QUERY, produitRecordParam);
  }

  default String buildParetoAmountQuey(ProduitRecordParamDTO produitRecordParam) {
    return this.buildQuery(ProductStatQueryBuilder.TOTAL_AMOUNT_QUERY, produitRecordParam);
  }

  default String buildPCountQuey(ProduitRecordParamDTO produitRecordParam) {
    return this.buildQuery(ProductStatQueryBuilder.COUNT_QUERY, produitRecordParam);
  }

  Resource printToPdf(ProduitAuditingParam produitAuditingParam) throws MalformedURLException;
}
