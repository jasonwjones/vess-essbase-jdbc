/*
# A dimension as a table

Each dimension is a table of its own, a row for each member. Besides the alias, parent, level and generation, there's a column for each named generation, holding the member's ancestor there, and one for each attribute, with its type: Caffeinated is a boolean, Ounces a number and Intro Date a date.
*/
SELECT NAME, ALIAS, Category, Caffeinated, Ounces, "Pkg Type", "Intro Date"
FROM Sample.Basic.Product
WHERE LEVEL = 0
ORDER BY OUTLINE_ORDER
