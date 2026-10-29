/*
# Asking the outline

Functions such as IS_DESCENDANT, IS_CHILD and HAS_UDA answer from the outline. In a WHERE clause they narrow what's read, since Vess tries each against the members first. Diet shares products from other families, and they count as under it.
*/
SELECT PRODUCT, MEMBER_ALIAS(PRODUCT) AS ALIAS, SUM(VALUE) AS SALES
FROM Sample.Basic.FACTS
WHERE IS_DESCENDANT(PRODUCT, 'Diet') AND HAS_UDA(MARKET, 'Major Market')
  AND MEASURES = 'Sales' AND SCENARIO = 'Actual'
GROUP BY PRODUCT
ORDER BY PRODUCT
