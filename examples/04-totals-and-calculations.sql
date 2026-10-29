/*
# Totals and calculations

Cells has members at every level, so it answers with Essbase's own totals and dynamic calculations: here a quarter, a region and a product family, for every scenario, Variance and Variance % included.
*/
SELECT Scenario, Value
FROM Sample.Basic.Cells
WHERE Year = 'Qtr1' AND Measures = 'Sales' AND Product = '100' AND Market = 'East'
