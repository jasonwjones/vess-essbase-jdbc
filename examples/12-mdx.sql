/*
# MDX too

A statement that's MDX goes straight to Essbase, through the Thriller driver, so one connection answers both.
*/
SELECT {[Jan], [Feb], [Mar]} ON COLUMNS,
[Market].Children ON ROWS
FROM Sample.Basic
WHERE ([Sales], [Actual], [Product])
