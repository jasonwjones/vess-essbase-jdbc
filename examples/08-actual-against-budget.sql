/*
# Actual against budget

Scenarios side by side, the way a report wants them, by turning rows into columns with conditional sums.
*/
SELECT MARKET,
       SUM(CASE WHEN SCENARIO = 'Actual' THEN VALUE END) AS ACTUAL,
       SUM(CASE WHEN SCENARIO = 'Budget' THEN VALUE END) AS BUDGET
FROM Sample.Basic.CELLS
WHERE YEAR = 'Year' AND MEASURES = 'Sales' AND PRODUCT = 'Product'
  AND IS_CHILD(MARKET, 'Market')
GROUP BY MARKET
ORDER BY MARKET
