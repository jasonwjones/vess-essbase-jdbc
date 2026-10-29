/*
# The cube as a fact table

FACTS holds just the values loaded at level 0, so they add up the way SQL expects. Joined to DIM_PRODUCT, they can be grouped by anything it has: here by category and whether the drink has caffeine.
*/
SELECT p.CATEGORY, p.CAFFEINATED, SUM(f.VALUE) AS SALES
FROM Sample.Basic.FACTS f
JOIN Sample.Basic.DIM_PRODUCT p ON f.PRODUCT = p.NAME
WHERE f.MEASURES = 'Sales' AND f.SCENARIO = 'Actual'
GROUP BY p.CATEGORY, p.CAFFEINATED
ORDER BY p.CATEGORY, p.CAFFEINATED
