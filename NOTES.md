# Vess - Virtual Essbase JDBC Driver Developer Notes

Also known as "It works, it works, I can't fucking believe that it actually
fucking works."

Vess is a JDBC driver that provides a relational facade on top of an Essbase 
server. It is JDBC-compliant, if not strictly JDBC-compatible. You can drop it 
into a generic JDBC tool and use it to perform various operations.

Vess maps Essbase OLAP concepts into a traditional relational database view of 
the world. This is also known as squishing a round peg into a square hole. Or 
perhaps more like squishing a cube into cylinder.

Wherever possible, Vess attempts to sensibly use relational database idioms 
to model Essbase behavior.     

# Notes on how JDBC Works

JDBC is a robust model/framework that accommodates most major database systems.
Given a connection to a typical database via JDBC, such as a SQL Server database
or an Oracle database, JDBC provides all manner of necessary functionality in 
order for those tools to get the list of catalogs, schemas, tables, columns in
those tables, to run queries, inserts, procedures, and more.

When you are navigating through a database using your preferred JDBC SQL tool, 
it is making calls to the database via JDBC to ask it for this various 
information, usually so it can show it to you with its GUI. For example, it's
common to look at the metadata of a table (its columns) and see it presented
in tabular format, showing the names of columns, data types, whether they are
nullable, and more. 

This is all made possible by those database systems returning the proper 
information about their tables.

Again, Vess attempts to provide a relational facade that resembles a JDBC
database experience. To that end, it presents schemas, tables, and columns
based on the contents of the connected Essbase server.

# Limitations

Vess currently requires that a particular cube be connected to upon startup.
This is because the cube will be opened to have its dimensionality inspected
in order to determine what the fact/data table should look like. Reading the 
outline requires that the cube be started, so rather than firing up every single
cube on the server in order to read it, just the main cube is connected to for
now.

# H2 Database

Vess is bolted on top of a 100% Java database known as H2. Unless otherwise 
noted, Vess supports H2 SQL query syntax. 

Notably, String concatenation is performed with the CONCAT function or double
pipe (||) characters, not "+" based concatenation.

## H2 Database Links

H2 (and therefore Vess) supports the notion of table database links. Whereas
the Essbase tables in the VESS_SCHEMA schema are "magic" tables, the linked
table is another special table that connects to a remote/disparate database
via JDBC. You simply need to have the proper driver available on the classpath.

A link can be created with the following syntax:

	CREATE LINKED TABLE SNP_COL('com.mysql.jdbc.Driver', 
	'jdbc:mysql://45.55.168.93:3306/dev_odi_work?noDatetimeStringSync=true',
	'root', '9nOZ6BfsES', 'SNP_COL')

Where the syntax example says "SNP_COL", replace with the name you would like
the table to be known as inside your Vess database. The rest of the parameters
being passed to the function are as follows:

 * JDBC driver class name
 * JDBC URL
 * Username
 * Password
 * Table name in remote schema

The driver class must be loaded or otherwise on the classpath.

# Mapped Concepts

## Catalogs

Vess is entirely contained inside of a single catalog, named VESS. A 

## Schemas

Vess currently has three+ schemas: INFORMATION_SCHEMA, PUBLIC, and VESS_SCHEMA.
The first two can be safely ignored for now, they are internal to the SQL 
database underpinning Vess. The custom VESS_SCHEMA is populated with 
"interesting" Vess things. Another schema will be created for the cube you connect to.
For example, if you connect to Sample.Basic, there will be a SAMPLE schema.

## Tables

Inside the VESS_SCHEMA schema resides custom tables based on the current 
connection. A "data" table is created for the connected cube. For example, if
the connected cube is Sample.Basic, then a table will be present inside of
VESS_SCHEMA named SAMPLE_BASIC_DATA (this may change in the future)

Upon startup, Vess connects to the given cube and reads its non-attribute/non-
attribute-calculation dimensions (in other words, its stored dimensions). The
names of the dimensions are used to generate a fact table. This will look very
similar to what an ODI RKM might generate for the data in a table. For example,
a stock Sample.Basic database results in the following table structure:

	Year, Varchar(80), non-nullable
	Measures, Varchar(80), non-nullable 
	Product, Varchar(80), non-nullable
	Market, Varchar(80), non-nullable
	Scenario, Varchar(80), non-nullable
	DATA, Double, nullable
	
Going back to the concept of trying to map Essbase concepts to a relational
world, note the following qualities that this table and its columns exhibit:

* Dimension names correspond to column names
* The value of a given dimension column will ostensibly be populated with a 
  member from that dimension
* Member names are capped at 80 characters, the Essbase limit for member names
* Member name columns may not be null (every piece of data in Essbase requires
  that it be the intersection of a member from every single dimension)
* The data for a given member-tuple is numeric (64 bit floating point) and may
  be nullable (null corresponds to the Essbase #Missing value)	

At the moment, data tables appear to have zero rows in them -- a SELECT will
not bring any rows back. Data can be inserted to this table, however. Note
the following syntax:

	INSERT INTO VESS_SCHEMA.SAMPLE_BASIC_DATA 
	VALUES
		('Jan', 'Sales', '100-10', 'New York', 'Actual', 100.12),
		('Feb', 'Sales', '100-10', 'New York', 'Actual', 200.12)

In the preceding example, a standard INSERT statement is constructed. Note that
the columns of the table are not specified (although they could be), such as
this example:

	INSERT INTO
		VESS_SCHEMA.SAMPLE_BASIC_DATA ("Year", "Measures", "Product", 
		"Market", "Scenario", "DATA")
	VALUES
		('Jan', 'Sales', '100-10', 'New York', 'Actual', 100.12)
		
Note that column names are surrounded by double-quotes to qualify them. String/
VARCHAR column values are surrounded by single quotes. The numeric value being
inserted does not have any quotes around it.

Data that is inserted to a special data table as in the previous example will be
uploaded to the cube via the only available mechanism that doesn't require a 
load rule: raw update strings. Raw update strings have a character limit of 
about 32K. Vess will chunk inserts into blocks of 30K characters and upload
them sequentially, as needed.

The first example showed an example of using multiple insert tuples. This allows
for inserting more than one record at one time (as opposed to issuing a series
of INSERT statements). In the second example you can see that just one tuple
has been written.

## Rejected Records

Unfortunately, the sendString() Essbase Java API method does not seem to have 
a provision for returning the list of rejected records, as the other data load
methods (ones using a load rule) do. This appears to cause data loads to simply
fail or throw an exception if there is an unknown member. Hopefully I can 
improve the behavior on this a bit in the future by simply prechecking the 
member names before attempting to load.  

# Table Reference

The following tables are created:

 * [App]_[DB]_DATA
 * VARS
 * SESSIONS

## [APP]_[DB]_DATA

This table is created based on the database you connect to on startup. You can insert to it but
at the moment you cannot SELECT from it

## VARS

This table shows variables with a server-wide scope. It does not show variables that are specific
to a particular app/cube (despite the current presence of those columns). It's a proof of concept
table that will get filled out soon.


# Other Concepts

## Essbase JDBC URLS

Vess introduces a JDBC URL format for connecting to an Essbase server/cube:

	jdbc:essbase://192.168.1.231:9000/Sample.Basic;app=Sample;cube=Basic

Or more generally:

	jdbc:essbase://[server][:port]/[Application].[Cube][;property=value[;property=value]]

User name and password values are specified separately (usually a JDBC GUI will
have text boxes for you to enter these in).

Recognized parameters:

	loadApps=ASOSamp,SampEast,etc
	
LoadApps can be a comma-delimited list of apps to specifically load and create
a namespace for

# Installation

Vess does not seem to work with the RazorSQL tool at this time due to a class
loading problem and fuckup in the Essbase Java API. There may be a workaround
in the future.

The freely available Squirrel SQL tool appears to work just fine, provided
that the library files are properly added/configured.

At this time, Vess requires Java 1.8! Squirrel SQL appears to run with my 
default Java 1.8 just fine. I may try and tweak some of the libraries being
used to get it back to 1.6 compatibility but it's kind of a good excuse to push
for a newer Java, finally.

Squirrel SQL does not ship with JDBC drivers but they can be installed fairly
easily. Moreover, new drivers can be setup (which is what you need to do for
Vess). 

To configure Vess inside of Squirrel SQL, do the following:

 1. Configure Vess Driver
 	1. Go to the Drivers panel/tab
 	2. Click + to add a new entry
 	3. Fill in the dialog:
 		1. Name: Vess Virtual Essbase JDBC Driver
 		2. Example URL: jdbc:essbase://192.168.1.231:9000/Sample.Basic;app=Sample;cube=Basic
 		3. Class name: com.saxifrages.vess.VessDriver
 		4. Click on Extra Class Path tab
 		5. All all Vess JAR files, including the following:
 			1. essbase-es-server-11.1.2.3.jar
 			2. essbase-japi-11.1.2.3.jar
 			3. odl-logging-11.1.2.3.jar
 			4. log4j-over-slf4j-1.7.8.jar
 			5. slf4j-api-1.7.9.jar
 			6. essbase-jdbc-0.0.1-SNAPSHOT.jar
 			7. h2-1.4.187.jar
 			8. esbase-utils-1.2.0.jar
 			9. logback-classic-1.1.3.jar
 			10 logback-core-1.1.3.jar
 			11. Add additional JDBC drivers for 3rd party databases if you want
 			   to try and create an external table link to them (for example,
 			   you could add a MySQL driver)
	   6. Press OK to save the driver
 2. Create entry to conncet to your Essbase server 
 	1. Go to the Aliases tab (to create a new connection entry)
    2. Click + icon to add new entry
    3. Give it a name
    4. Select the Vess driver you configured earlier
    5. Put in a Vess JDBC URL, such as jdbc:essbase://SERVER/APP.DB
    6. Put in Essbase user name (admin/essadmin/whatever)
    7. Put in Essbase password
 3. You should now be able to connect to your Essbase server via the Vess driver
    1. You can now browse the tables in this virtual database
    
Instructions will vary by SQL tool. Some tools are built such that instead of
specifying a list of Java JAR files, you put their file paths into a single
string and delimit them with semi-colons. As mentioned earlier, RazorSQL doesn't
seem to work right now because of an issue with the way the geniuses at Oracle
coded the Essbase API. I'm looking into a workaround. 

## Updating the Vess driver 

If you are provided an updated Vess jar file (essbase-jdbc*.jar), you can overwrite the existing
file in your Squirrel SQL driver, relaunch Squirrel SQL, and should be good to go. If the file has
a new name (like a version bump) then you either need to change it and overwrite the old file or
drop in the new file and update the driver entry on the Driver tab, similar to the instructions on
how to set it up in the first place. 

# Sample Queries

## Insert data to database using multiple tuples

At the moment, dimension names must be specified and be enclosed in quotes.

	INSERT INTO VESS_SCHEMA.SAMPLE_BASIC_DATA ("Year", "Measures", "Product", "Market", "Scenario", "DATA") 
	VALUES ('100-10', 'Jan', 'Actual', 'Washington', 'Foo', 100.126),  ('100-10', 'Jan', 'Actual', 'Washington', 'Foo2', 100.127),
	('100-10', 'Jan', 'Actual', 'Washington', 'Foo2', 100.128);
	
	// this works
	
## Insert with single tuple
	
	INSERT INTO VESS_SCHEMA.DRILLING_DRILLING_DATA ("Years", "Time", "Location", "Account", "DATA")
	VALUES ('FY14', 'Jan', '100-200', '0170100', 2.12);
	
## Insert and use CSVREAD H2 function

CSVREAD is an H2 function that reads a CSV file. The first row should be the
column names (delimited with commas, naturally)	
	
	INSERT INTO VESS_SCHEMA.SAMPLE_BASIC_DATA ("Year", "Measures", "Product", "Market", "Scenario", "DATA") 
	SELECT * FROM CSVREAD('/Users/jasonwjones/test/sample_basic_data.txt');
	

## Can't find bundle error message

Hyperion class in essbase-es-server named BaseLogger is attempting to load a 
bundle that is apparently in the essbase-japi, such as file:

com.essbase.api.resources.ApiObjectProperties

Error is possibly being caused because the classes are loaded by disparate
class loaders and it therefore can't "see" the proper file.

# Misc

CREATE ALIAS MY_SQRT FOR "java.lang.Math.sqrt";
CREATE ALIAS RUN_CALC FOR "com.saxifrages.vess.h2.procs.RunCalc.runCalc";
CALL RUN_CALC('CalcAll');

## Example CSV contents to use with CSVREAD function

Year,Measures,Product,Market,Scenario,DATA
Jan,Sales,100-10,New York,Actual,20