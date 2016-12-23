# Versions

## 0.0.3

 * Started tracking versions
 * APPLICATION in VESS_SCHEMA.APPLICATIONS is now PK
 * Character lengths for NAME/PARENT_NAME in DIM tables are now set to 80 chars (helps ODI et al)
 * Remove DATA tables to prepare for rework
 * Generic properties tables now enforce that prop name is key, non-nullable, and has length limits

## 0.0.2

 * Substitution variable tables are in and fully symmetric (CRUD oeprations)
 * Viewing outline/dim tables works
 * New PROPS tables model each app/cubes inherent Essbase properties
 * Tried to test with ODI 11.1.1.7 but no go for now. The Vess drivers are compiled with Java
   1.8 and although ODI 11.1.1.7 can be made to run with this, it seems to ignore JAR files
   that are strictly compiled for 1.8+. Next best bet may be to try ODI 12c or try to refactor
   Vess back down to Java 1.7

## 0.0.1

 * Initial outside release. Things work but are pretty rough.
