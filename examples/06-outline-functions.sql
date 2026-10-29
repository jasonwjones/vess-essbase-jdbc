/*
# Asking the outline

Functions such as IS_DESCENDANT, IS_CHILD and HAS_UDA answer from the outline. In a WHERE clause they narrow what's read, since Vess tries each against the members first. Diet shares products from other families, and they count as under it.
*/
SELECT Product, MEMBER_ALIAS(Product) AS Alias, SUM(Value) AS Sales
FROM Sample.Basic.Facts
WHERE IS_DESCENDANT(Product, 'Diet') AND HAS_UDA(Market, 'Major Market')
  AND Measures = 'Sales' AND Scenario = 'Actual'
GROUP BY Product
ORDER BY Product
