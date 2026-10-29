/*
# Every alias table

Each alias table is a column of the dimension's table; ALIASES has them all as rows, for when they're wanted together. Here are four of Sample.Basic's six.
*/
SELECT MEMBER, ALIAS_TABLE, ALIAS
FROM Sample.Basic.ALIASES
WHERE MEMBER IN ('100-10', 'Qtr1') AND ALIAS_TABLE IN ('Default', 'Long Names', 'GermanNames', 'RussianNames')
ORDER BY MEMBER, ALIAS_TABLE
