/*
# Totals and calculations

CELLS has members at every level, so it answers with Essbase's own totals and dynamic calculations: here a quarter, a region and a product family, for every scenario, Variance and Variance % included.
*/
SELECT SCENARIO, VALUE
FROM Sample.Basic.CELLS
WHERE YEAR = 'Qtr1' AND MEASURES = 'Sales' AND PRODUCT = '100' AND MARKET = 'East'
