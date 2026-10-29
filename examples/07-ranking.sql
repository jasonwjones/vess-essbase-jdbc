/*
# Ranking with SQL

All of SQL is there once the values are rows. Here each region's best-selling product, ranked by a window function, with the region from the REGION generation column of DIM_MARKET.
*/
SELECT REGION, PRODUCT, SALES
FROM (
  SELECT m.REGION, f.PRODUCT, SUM(f.VALUE) AS SALES,
         RANK() OVER (PARTITION BY m.REGION ORDER BY SUM(f.VALUE) DESC) AS PLACE
  FROM Sample.Basic.FACTS f
  JOIN Sample.Basic.DIM_MARKET m ON f.MARKET = m.NAME
  WHERE f.MEASURES = 'Sales' AND f.SCENARIO = 'Actual'
  GROUP BY m.REGION, f.PRODUCT
) ranked
WHERE PLACE = 1
ORDER BY REGION
