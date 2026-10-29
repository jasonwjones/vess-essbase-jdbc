/*
# Ranking with SQL

All of SQL is there once the values are rows. Here each region's best-selling product, ranked by a window function, with the region from the Market table's generation column.
*/
SELECT Region, Product, Sales
FROM (
  SELECT m.Region, f.Product, SUM(f.VALUE) AS Sales,
         RANK() OVER (PARTITION BY m.Region ORDER BY SUM(f.VALUE) DESC) AS Place
  FROM Sample.Basic.FACTS f
  JOIN Sample.Basic.Market m ON f.Market = m.NAME
  WHERE f.Measures = 'Sales' AND f.Scenario = 'Actual'
  GROUP BY m.Region, f.Product
) ranked
WHERE Place = 1
ORDER BY Region
