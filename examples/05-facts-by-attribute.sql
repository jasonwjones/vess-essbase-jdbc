/*
# The cube as a fact table

FACTS holds just the values loaded at level 0, so they add up the way SQL expects. Joined to the Product table, they can be grouped by anything it has: here by category and whether the drink has caffeine.
*/
SELECT p.Category, p.Caffeinated, SUM(f.VALUE) AS Sales
FROM Sample.Basic.FACTS f
JOIN Sample.Basic.Product p ON f.Product = p.NAME
WHERE f.Measures = 'Sales' AND f.Scenario = 'Actual'
GROUP BY p.Category, p.Caffeinated
ORDER BY p.Category, p.Caffeinated
