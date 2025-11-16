# Test Troubleshooting Guide

## TableauPharmacienServiceRefactoredTest Issues

### Fixes Applied

1. **Changed from Mocked to Real Calculator/Aggregator**

   - Calculator and Aggregator are now real instances (not mocked)
   - They execute actual business logic during tests

2. **Manual Service Construction**

   - Service is manually constructed in `setUp()` instead of using `@InjectMocks`
   - This ensures all dependencies are properly injected

3. **Fixed ObjectMapper Mock**
   - Changed from `any()` to `any(TypeReference.class)`
   - Added missing TypeReference import

### Common Test Failures

#### 1. NullPointerException

**Cause:** Missing mock setup
**Solution:** Ensure all repository methods are mocked before calling service

```java
when(salesRepository.fetchTableauPharmacienReport(...)).thenReturn("[]");
when(objectMapper.readValue(...)).thenReturn(new ArrayList<>());
when(commandeDataService.fetchReportTableauPharmacienData(any())).thenReturn(new ArrayList<>());
when(reponseRetourBonItemRepository.findByDateRange(any(), any())).thenReturn(new ArrayList<>());
when(appConfigurationService.excludeFreeUnit()).thenReturn(false);
when(groupeFournisseurManager.getDisplayedGroupIds()).thenReturn(Set.of(1, 2));
```

#### 2. Type Mismatch in ObjectMapper

**Cause:** Mockito can't match TypeReference parameter
**Fix Applied:** Use `any(TypeReference.class)` instead of `any()`

#### 3. Empty or Null Data Not Handled

**Cause:** Real aggregator needs proper data structures
**Solution:** Ensure all DTOs have required fields initialized

### Running Tests

**Skip frontend build to speed up tests:**

```bash
mvnw.cmd test -Dtest=TableauPharmacienServiceRefactoredTest -Dskip.npm=true
```

**Run with detailed output:**

```bash
mvnw.cmd test -Dtest=TableauPharmacienServiceRefactoredTest -X
```

**Run single test method:**

```bash
mvnw.cmd test -Dtest=TableauPharmacienServiceRefactoredTest#testGetTableauPharmacien_emptyData
```

### Quick Validation Test

Run the simple test first to verify calculator/aggregator work:

```bash
mvnw.cmd test -Dtest=TableauPharmacienServiceRefactoredSimpleTest -Dskip.npm=true
```

This test has no mocking and verifies:

- Calculator can compute ratios
- Aggregator can calculate totals
- Aggregator can aggregate sales to wrapper

### Expected Test Coverage

| Test                                         | Purpose                         | Expected Result |
| -------------------------------------------- | ------------------------------- | --------------- |
| testGetTableauPharmacien_completFlow         | Full integration with all data  | PASS            |
| testGetTableauPharmacien_onlySalesData       | Only sales, no purchases/avoirs | PASS            |
| testGetTableauPharmacien_onlyPurchasesData   | Only purchases, no sales/avoirs | PASS            |
| testGetTableauPharmacien_onlyAvoirsData      | Only supplier returns           | PASS            |
| testGetTableauPharmacien_emptyData           | No data at all                  | PASS            |
| testGetTableauPharmacien_errorHandling       | Database error handling         | PASS            |
| testGetTableauPharmacien_excludeFreeUnitTrue | Configuration test              | PASS            |
| testFetchGroupGrossisteToDisplay             | Group display delegation        | PASS            |

### Debugging Failed Tests

1. **Check Stack Trace**

   ```bash
   mvnw.cmd test -Dtest=TableauPharmacienServiceRefactoredTest 2>&1 | grep -A 20 "FAILURE"
   ```

2. **Enable Debug Logging**
   Add to `src/test/resources/logback-test.xml`:

   ```xml
   <logger name="com.kobe.warehouse.service.financiel_transaction" level="DEBUG"/>
   ```

3. **Run Tests in IDE**
   - Right-click test class → Run with Coverage
   - Set breakpoints in service methods
   - Inspect mock call history

### Known Issues

1. **Frontend Build Takes Long**

   - Solution: Use `-Dskip.npm=true` flag

2. **JsonMapper TypeReference Matching**

   - Ensure using `any(TypeReference.class)` in mocks

3. **Real Aggregator Needs Valid Data**
   - Can't use null for lists
   - Must initialize all DTO fields used in calculations

### If All Tests Still Fail

1. **Verify Compilation**

   ```bash
   mvnw.cmd test-compile -Dskip.npm=true
   ```

2. **Check for Syntax Errors**

   - Look for compilation errors in test output
   - Verify all imports are correct

3. **Run Other Tests**

   ```bash
   mvnw.cmd test -Dtest=TableauPharmacienCalculatorTest -Dskip.npm=true
   mvnw.cmd test -Dtest=TableauPharmacienAggregatorTest -Dskip.npm=true
   ```

4. **Provide Error Output**
   - Copy the full stack trace
   - Note which specific test(s) failed
   - Check the error message

### Test File Locations

- Main test: `src/test/java/com/kobe/warehouse/service/financiel_transaction/TableauPharmacienServiceRefactoredTest.java`
- Simple test: `src/test/java/com/kobe/warehouse/service/financiel_transaction/TableauPharmacienServiceRefactoredSimpleTest.java`
- Calculator test: `src/test/java/com/kobe/warehouse/service/financiel_transaction/TableauPharmacienCalculatorTest.java`
- Aggregator test: `src/test/java/com/kobe/warehouse/service/financiel_transaction/TableauPharmacienAggregatorTest.java`
