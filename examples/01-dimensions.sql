/*
# The cube's dimensions

Every cube is a schema, and DIMENSIONS is one of its tables: what each dimension is, how big, and how deep. The attribute dimensions are there too; this leaves them out.
*/
SELECT NAME, TYPE, DENSITY, MEMBERS, STORED_MEMBERS, GENERATIONS, LEVELS
FROM Sample.Basic.DIMENSIONS
WHERE TYPE <> 'Attribute'
ORDER BY NUMBER
