# Vess - Virtual Essbase JDBC Driver 

## Building

For a full release:

 * Run clean/install on essbase-utils project
 * Then run pack.sh on Vess to build dist and files


## Configuring RazorSQL

Driver file String (shown here as example of all files needed, adjust path as necessary):

/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/vess-essbase-jdbc-0.0.1-SNAPSHOT.jar;/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/essbase-es-server-11.1.2.3.jar;/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/essbase-japi-11.1.2.3.jar;/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/essbase-utils-1.2.2.jar;/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/h2-1.4.187.jar;/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/log4j-over-slf4j-1.7.8.jar;/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/logback-classic-1.1.3.jar;/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/logback-core-1.1.3.jar;/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/odl-logging-11.1.2.3.jar;/Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/dist/slf4j-api-1.7.9.jar



## Internal Dev Notes

We can recycle/subclass MetaTable into VessMetaTable, then override the generateRows
method to bring back the custom Vess tables. Then we can manage metadata our own
way and use generateRows() to adapt it to the normal expected MetaData format of
JDBC

Create Vess System tables to track databases (schemas) and column metadata
Create a new ResultSet thing that can merge two result sets

Request for schemas will be driven by the apps/databases
Request for columns can be done on the fly and just fire up a cube if 
  metadata is needed
  
 Vess init: 
 	Programmatically create all necessary schemas
 	Go to to VessDBMD and intercept calls to schemas that overlap with a tracked schema
 	If it's an H2 schema, delegate it out
 	If it's not, return a proper result set.
 	Do the same (intercept calls) for table columns


Interaction Notes with RazorSQL:

Column loading appears to happen on demand; therefore, we could probably get
away with just loading up all of the cubes and deferring loading up the
proper structures.

Packacking:

To get to a single Jar file, export a runnable Jar with all libraries unpacked

Right now Jaybase and others rely on Java 8. By default, RazorSQL runs with
a 1.6 JRE. To run it with 1.8:

cd /Applications/RazorSQL.app/Contents/Java
java -jar razorsql.jar  

(assumes that java -version shows that a 1.8 JRE is default)

Running from command line has the added benefit of seeing the Sysout!


Deleting members:

http://john-goodwin.blogspot.com/2011/01/odi-series-deleting-essbase-members.html

TODO:

Read from Essbase Cube with no WHERE predicates
Write to Essbase Cube
Read/write substitution variables
Database stats (read only)
Read dimension data from Essbase cube
Write dimension data to Essbase cube
Stored procedure to run a calc
Stored procedure to clear a cube
Gen catalogs from cubes

Custom String functions (applied to a member name):
	PARENT
	DIMENSION
	NUM_CHILDREN
	LEVEL
 
It's technically possible to do data pulls using WHERE predicates. The underlying
mechanism is to generate cartesian grid products for 'locked' dimensions. For 
example:

SELECT Time, Product FROM __data WHERE Year = 'FY15'

Without a locked member for other dimensions, it is assumed that all members
are to be loaded (skip dynamic calc?)

How do we deal with the fact that a metric fuckload of data could potentially
come back? We need to find a way to do result paging that is consistent with
H2s result paging mechanism (the cartesian product would include #Missing cells
in order to get a relatively determinable number of cells)

 
Create special views/tables for reading and inserting that orient some of the 
columns horizontally for convenience. For example, time periods splayed out

absolute minimum necessary to insert to a member table would be to specify a child
name. In this case, the parent would be assumed to be null which would correspond
to the root of the dimension. Insertion may not be successful. Insertion may
require a restructure. 

LinkedIndex
	for Ess impl lets just dump it off back to the table
	executes the query (generates the ResultSet)
	for inserts: generates the SQL and hands it back to the TableLink
LinkedCursor has a reference to ResultSet and it iterates through it

It looks like a returned row is always a full row and that something in H2
must marry it back up with the Columns[] off of the table and merge it all
together. So the only responsibility of the datasource is to return a 
row object with the same alignment as the columns.


Logic mapping units:

Map Essbase Object/family/table to a MagicTable
Build the fetch logic somewhere

Beef up Jaybase so it can stub in

## Vess over In-memory Database (H2)

EssAdapter:

	Wraps the Essbase API into a ResultSet-like object. 
	Vess then pumps this data into a temporary table inside of H2
	It then runs the original query using the temporary table as the data source
	This way we can get ANSI SQL-92 semantics for free, as well as rearranging
	columns and shit like that
	
	For example
	
	SELECT [selectExpr] FROM SYS.SUBSTITUTION_VARIABLES WHERE [whereExpr]
	
	the use of the sub var table causes Vess to refresh data into a temporary
	table, rewrite the query, and hand it off to H2 for execution
	

## Typical JDBC Connection

 1. Connection = DriverManager.getConnection(url, user, pass)
 2. Statement = Connection.createStatement();
 3. ResultSet = Statement.executeQuery();

## Vess JDBC Urls

^jdbc:(essbase|essbase:embedded)://((\\w+)(:(\\w*))?@)?([^/:]+)(:(\\d+))?(/(\\w+))?");

jdbc:essbase://user:pass@server:port/params

Available parameters:

application=
cube=

Other connection options:


## Mapping JDBC Concepts to Essbase

There aren't any methods to do a dim build without a load rule, we would need
to  use one to do a built or go into the outline one member at a time and build
there.

select [description] from data.sample_basic where 
	__
	
insert into data.sample_basic (time, product, year)
values ('000', '000', '000', '000)

insert into data.sample_basic (time, product, year)
select * from data.sample_basic2 where time = 0

execmdx query _____

SERVER
	Properties
		Description
		Locale
		Name

	Connections
	Users
	Requests
	Properties (from parent Object?)
	Subvars
		
	
	CDFs

APP

App is the least interesting object, but nevertheless, we have it.

CUBE
	
	Various cube properties
	You can load data directly to a virtual table that's modeled on the dimensions?
	
	Outline
	Parent, Child, Consolidation, TwoPass, etc
	Aliases/UDAs
	

Stored Procedures

	EXEC HumanResources.uspGetEmployeesTest2 @LastName = N'Ackerman', @FirstName = N'Pilar';

	EXEC sp_clear_database @Database = 'Sample.Basic'
	

	begin dim build
	clear data

readable/writeable

ESS_SUB_VARS

	The combination of app/db/name are considered to be a compound primary key

	app
	db
	name
	value
	
ESS_OUTLINE (readable)

ESS_SERVER_STATS

ESS_CUBE_STATS

## TODO

Get the JDBC Compliance test suite



	Builder
		.addColumn("NAME", Class<?> clazz, int length)
		.addString("NAME")
		.addString("NAME", 50)
	
## Future (Theoretical)

UPDATE Variables SET Name = "Foo" WHERE APP = "Sample"

CREATE LINKED TABLE LINK('com.mysql.jdbc.Driver', 'jdbc:mysql://45.55.168.93:3306/dev_odi_work?noDatetimeStringSync=true', 'root', '9nOZ6BfsES', 'SNP_COL');

CREATE LINKED TABLE TRANSACTIONS('com.mysql.jdbc.Driver', 'jdbc:mysql://diskstation.corp.saxifrages.com:3306/drilluser?noDatetimeStringSync=true', 'drilluser', 'drillpass', 'TRANSACTIONS');

CREATE LINKED TABLE TRANS_SMALL('com.mysql.jdbc.Driver', 'jdbc:mysql://diskstation.corp.saxifrages.com:3306/drilluser?noDatetimeStringSync=true', 'drilluser', 'drillpass', 'TRANS_SMALL');


CREATE LINKED TABLE TRANS_SMALL('com.mysql.jdbc.Driver', 'jdbc:mysql://diskstation.corp.saxifrages.com:3306/drilluser?noDatetimeStringSync=true', 'user', 'pass', 'TRANS_SMALL');

INSERT INTO VESS_SCHEMA.DRILLING_DRILLING_DATA ("Years", "Time", "Location", "Account", "DATA")
SELECT 'FY' || RIGHT(FISCAL_YEAR, 2) AS Years, 'Jan' AS Time, DIVISION || '-' || LOCATION AS LOCATION, '0' || ACCOUNT, AMOUNT FROM TRANS_SMALL;
