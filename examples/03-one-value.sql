/*
# One value

CELLS holds every value in the cube, with a column for each dimension. Name a member of each and you have one cell. Vess turns the conditions into the MDX that reads just that.
*/
SELECT VALUE
FROM Sample.Basic.CELLS
WHERE YEAR = 'Jan' AND MEASURES = 'Sales' AND PRODUCT = '100-10' AND MARKET = 'New York' AND SCENARIO = 'Actual'
