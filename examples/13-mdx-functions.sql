/*
# What the server knows

The server's own tables are in ESSBASE.SERVER: applications, cubes, sessions, users, jobs, logs, and the MDX functions it understands, with their syntax.
*/
SELECT NAME, SYNTAX
FROM ESSBASE.SERVER.MDX_FUNCTIONS
WHERE NAME LIKE 'Top%'
ORDER BY NAME
