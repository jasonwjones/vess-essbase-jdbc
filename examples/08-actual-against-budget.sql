/*
# Actual against budget

Scenarios side by side, the way a report wants them, by turning rows into columns with conditional sums.
*/
SELECT Market,
       SUM(CASE WHEN Scenario = 'Actual' THEN Value END) AS Actual,
       SUM(CASE WHEN Scenario = 'Budget' THEN Value END) AS Budget
FROM Sample.Basic.Cells
WHERE Year = 'Year' AND Measures = 'Sales' AND Product = 'Product'
  AND IS_CHILD(Market, 'Market')
GROUP BY Market
ORDER BY Market
