/*
# Every alias table

A dimension's table has the alias from one alias table: the cube's default, or the one the connection's alias.table setting names. ALIASES has every alias in every alias table, a row each. Here are four of Sample.Basic's six.
*/
SELECT MEMBER, ALIAS_TABLE, ALIAS
FROM Sample.Basic.ALIASES
WHERE MEMBER IN ('100-10', 'Qtr1') AND ALIAS_TABLE IN ('Default', 'Long Names', 'GermanNames', 'RussianNames')
ORDER BY MEMBER, ALIAS_TABLE
