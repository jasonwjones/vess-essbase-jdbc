/*
# Calc scripts

SCRIPTS holds the cube's calc and MDX scripts, text and all; here each line break is shown as a space. Updating CONTENT saves a script, and each calc script is also a procedure: CALL Sample.Basic.CalcAll() runs this one.
*/
SELECT TYPE, NAME, TRIM(REPLACE(CONTENT, U&'\000A', ' ')) AS CONTENT
FROM Sample.Basic.SCRIPTS
WHERE TYPE = 'Calc'
