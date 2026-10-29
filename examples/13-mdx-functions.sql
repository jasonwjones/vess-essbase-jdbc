/*
# What the server knows

The server's own tables are in Essbase.Server: applications, cubes, sessions, users, jobs, logs, and the MDX functions it understands, with their syntax.
*/
SELECT NAME, SYNTAX
FROM Essbase.Server.MdxFunctions
WHERE NAME LIKE 'Top%'
ORDER BY NAME
