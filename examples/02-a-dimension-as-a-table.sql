/*
# A dimension as a table

Each dimension is a table of its own, a row for each member: DIM_PRODUCT for Product. Besides the alias, parent, level and generation, there's a column for each named generation, holding the member's ancestor there, and one for each attribute, with its type: CAFFEINATED is a boolean, OUNCES a number and INTRO_DATE a date.
*/
SELECT NAME, ALIAS, CATEGORY, CAFFEINATED, OUNCES, PKG_TYPE, INTRO_DATE
FROM Sample.Basic.DIM_PRODUCT
WHERE LEVEL = 0
ORDER BY OUTLINE_ORDER
