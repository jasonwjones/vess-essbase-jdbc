/*
# One value

Cells holds every value in the cube, with a column for each dimension. Name a member of each and you have one cell. Vess turns the conditions into the MDX that reads just that.
*/
SELECT Value
FROM Sample.Basic.Cells
WHERE Year = 'Jan' AND Measures = 'Sales' AND Product = '100-10' AND Market = 'New York' AND Scenario = 'Actual'
