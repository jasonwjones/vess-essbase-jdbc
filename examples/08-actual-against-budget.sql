/*
# Actual against budget

Scenarios side by side, the way a report wants them, by turning rows into columns with conditional sums.
*/
SELECT Market,
       SUM(CASE WHEN Scenario = 'Actual' THEN VALUE END) AS Actual,
       SUM(CASE WHEN Scenario = 'Budget' THEN VALUE END) AS Budget
FROM Sample.Basic.CELLS
WHERE Year = 'Year' AND Measures = 'Sales' AND Product = 'Product'
  AND IS_CHILD(Market, 'Market')
GROUP BY Market
ORDER BY Market
