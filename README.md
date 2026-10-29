# Vess

A JDBC driver that turns a whole Essbase server into a database you can browse and query with
SQL. Applications are catalogs and cubes are schemas. Each dimension is a table, the cube's data is a
fact table, substitution variables and scripts are tables you can edit, and calc scripts are stored
procedures. It talks to Essbase over the REST API, so it needs no Oracle software.

```sql
SELECT p.CATEGORY, p.CAFFEINATED, SUM(f.VALUE) AS SALES
FROM Sample.Basic.FACTS f
JOIN Sample.Basic.DIM_PRODUCT p ON f.PRODUCT = p.NAME
WHERE f.MEASURES = 'Sales' AND f.SCENARIO = 'Actual'
GROUP BY p.CATEGORY, p.CAFFEINATED
```

```
+----------+-------------+----------+
| CATEGORY | CAFFEINATED | SALES    |
+----------+-------------+----------+
| 100      | false       | 12841.0  |
| 100      | true        | 93293.0  |
| 200      | false       | 29309.0  |
...
```

Load it into IntelliJ, DBeaver or any other tool that takes a JDBC driver, and the server is there
to explore: the tree shows every application and cube, a cube's dimensions are tables with their
aliases, generations and attributes as columns, and the diagram view draws each cube as the star
schema it is. The [cookbook](examples/README.md) has more, each with the rows it answers with.

It has been tested against Essbase 26.1. Vess started in 2015, on the Essbase Java API; this is a
rewrite. See [History](#history).

## Getting it

Download `vess-essbase-jdbc-<version>-all.jar` from the
[releases page](https://github.com/jasonwjones/vess-essbase-jdbc/releases) or from
[Maven Central](https://central.sonatype.com/artifact/com.jasonwjones.hyperion.vess/vess-essbase-jdbc).
It's a single jar with everything Vess needs, an SQL engine included, which is why it's about 36 MB,
and it runs on Java 17 or newer.

To use it from code, depend on it instead:

```xml
<dependency>
  <groupId>com.jasonwjones.hyperion.vess</groupId>
  <artifactId>vess-essbase-jdbc</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Connecting

The driver class is `com.jasonwjones.hyperion.vess.VessDriver`, and the URL is the server's address,
as you'd open it in a browser, after `jdbc:essbase:vess:`:

```
jdbc:essbase:vess:http://localhost:9000/essbase
```

Settings go after semicolons on the URL, or in the connection's properties, as tools' driver
settings put them; the URL wins:

| Setting | Meaning |
| --- | --- |
| `user`, `password` | The Essbase login |
| `app`, `cube` | The application and cube to work in, so names needn't say them: `;app=Sample;cube=Basic` |
| `auth` | `password` (the default); `token`, to send the password as a bearer token; or `session`, to present it as the id of a session established elsewhere |
| `readonly` | `true` to refuse anything that would change the server |
| `browse.stopped` | `true` to list the data and dimension tables of stopped applications too; see [Stopped applications](#stopped-applications) |
| `outline.cache` | Seconds to keep an outline before reading it again; 300 by default |
| `chunk.cells`, `chunk.members` | The most cells one MDX query asks for (500,000), and the most members of one dimension it names (2,000) |
| `server.catalog` | The name of the catalog holding the server's own tables; `ESSBASE` by default |
| `dimension.prefix` | What each dimension's table is named with first; `DIM_` by default, and it can be empty |
| `alias.table` | The alias table that `ALIAS` columns and `MEMBER_ALIAS(member)` show, such as `Long Names`; each cube's default unless given, and a cube without it uses its default. MDX run with `members=alias` uses it too |
| `dimension.names` | How names that come from Essbase are written as names of tables and columns: `upper` (the default) or `lower` make them identifiers, so Pkg Type is `PKG_TYPE`; `as-is` keeps Essbase's spelling |

From code:

```java
Connection connection = DriverManager.getConnection(
        "jdbc:essbase:vess:http://localhost:9000/essbase;app=Sample;cube=Basic", "admin", password);
```

`VessDataSource` does the same for frameworks that configure a `DataSource`.

## Using it in a tool

### IntelliJ IDEA and DataGrip

1. In the Database tool window, add a **Driver**: add the jar under **Driver Files**, and choose
   `com.jasonwjones.hyperion.vess.VessDriver` as the class.
2. Add a **Data Source** using that driver, with the URL
   `jdbc:essbase:vess:http://localhost:9000/essbase` and your Essbase user and password.
3. On the data source's **Schemas** tab, choose the cubes to show, or all of them.

The tree then has a database for each application, a schema for each cube, and the tables, routines
and keys described below. Open a table to see its rows, and right-click a cube's schema for
**Diagrams > Show Diagram** to see it as a star: CELLS in the middle, a line to each dimension's
table, and from those to their attribute dimensions.

Choosing a cube for a query console makes it the one names refer to, so `SELECT * FROM DIM_PRODUCT`
works, and the outline functions need no prefix. Ticking **Read-only** on the data source makes the
connection read-only, as `readonly=true` does.

Tables you can change, such as VARIABLES and SCRIPTS, can be edited in the table view: change a
value and submit, and the variable or script changes on the server.

### DBeaver and others

Add a driver with the jar and the class above, and the URL template
`jdbc:essbase:vess:http://{host}:{port}/essbase`. DBeaver, SQuirreL, RazorSQL and the like browse
it the same way, through the same metadata.

## What's there

### The server: `ESSBASE.SERVER`

| Table | Holds |
| --- | --- |
| `APPLICATIONS` | Each application: type, status, owner, when created and changed. Set `STATUS` to `Started` or `Stopped` to start or stop it |
| `CUBES` | Each cube, likewise |
| `VARIABLES` | Substitution variables at every scope, with `APPLICATION` and `CUBE` saying where. Insert, update and delete them |
| `SESSIONS` | Who's connected to what. Deleting a session logs it off |
| `USERS`, `GROUPS`, `GROUP_MEMBERS`, `PERMISSIONS` | Security: users, groups, who's in each, and who has which role on the server or an application |
| `JOBS` | The job history, newest first, a page at a time |
| `LOG` | Each application's latest log, an entry a row: when, how severe, the message. Say which `APPLICATION` to read just its log |
| `CONFIGURATION` | Each application's configuration settings |
| `CONNECTIONS`, `DATASOURCES`, `DATASOURCE_COLUMNS` | Global connections and datasources |
| `MDX_FUNCTIONS` | The MDX functions the server knows, with their syntax |
| `PROPERTIES` | What the server says about itself: version, build, settings |

`SERVER.SESSIONS` works as well as `ESSBASE.SERVER.SESSIONS`, unless there's an application named
Server.

### A cube: `Application.Cube`

| Table | Holds |
| --- | --- |
| `CELLS` | Every value in the cube, at every level, totals and dynamic calculations included: a column for each dimension and `VALUE` |
| `FACTS` | The values loaded at level 0 of each dimension: the cube as a fact table |
| `DIM_` and each dimension | Its members, a row each, in `DIM_PRODUCT` for Product: name, alias, parent, level, generation, storage, consolidation, formula and UDAs; a column for each generation, holding the member's ancestor there; and a column for each attribute, typed as a boolean, number, date or text. The alias is from one alias table; `ALIASES` has the others |
| `DIMENSIONS` | Each dimension: type, dense or sparse, members, generations, levels |
| `MEMBERS` | Every member of every dimension where it sits in the outline, shared members included |
| `GENERATIONS`, `LEVELS` | Each dimension's generations and levels, with their names |
| `ALIAS_TABLES`, `ALIASES` | The alias tables, and every alias in every one |
| `VARIABLES` | The substitution variables the cube sees, at every scope, with `IN_EFFECT` false where a narrower one hides a variable. Insert, update and delete them |
| `SCRIPTS` | Calc and MDX scripts. Insert them, change `CONTENT` to save one, delete them |
| `DRILLTHROUGHS` | Drill-through definitions |
| `LOCKS` | Locked objects. Deleting one unlocks it |
| `SESSIONS` | Who's connected to the cube |
| `SCENARIOS` | Sandboxed scenarios, where the cube has them |

Each table has its primary key, and CELLS, FACTS and the dimension tables have foreign keys to the
tables of the dimensions they name, so tools can draw them and follow them.

### Procedures

| Procedure | Does |
| --- | --- |
| each calc script | `CALL Sample.Basic.CalcAll()` runs it |
| each MDX script | `CALL Sample.Basic.MyReport()` runs it and answers with its result |
| `RUN_CALC(script)` | Runs calc script text: `CALL Sample.Basic.RUN_CALC('CALC ALL;')` |
| `DEFAULT_CALC()` | Runs the cube's default calculation |
| `SERVER.START_APPLICATION(name)`, `SERVER.STOP_APPLICATION(name)` | Starts or stops an application |
| `SERVER.REFRESH()` | Forgets the outlines and applications Vess has read, so it reads them again |

`EXEC`, and the JDBC escape `{call ...}`, work too. Each answers with a row saying what it did, or
for an MDX script, with its result.

Tools list these as routines, with the outline functions. Each one's description says how to call
it, and a script's shows how it starts. What a tool shows as a routine's source is made up from
that, not the script itself, whose full text is in `SCRIPTS`.

### Functions

Each cube's schema has functions that answer from its outline:

| Function | Answers |
| --- | --- |
| `IS_DESCENDANT(member, ancestor)` | Whether the member is below the ancestor, at any depth, counting where it's shared |
| `IS_CHILD(member, parent)` | Whether it's directly below |
| `MEMBER_LEVEL(member)`, `MEMBER_GENERATION(member)` | Its level and generation |
| `MEMBER_PARENT(member)`, `MEMBER_DIMENSION(member)` | Its parent and dimension |
| `MEMBER_ALIAS(member)`, `MEMBER_ALIAS(member, table)` | Its alias, in the alias table the connection uses or the one named |
| `HAS_UDA(member, uda)` | Whether it has a user-defined attribute |
| `MEMBER_ATTRIBUTE(member, attribute dimension)` | Its attribute in an attribute dimension |

Name them with the cube, `Sample.Basic.IS_DESCENDANT(...)`, or choose the cube to work in.

## Querying data

CELLS and FACTS are tables like any other, so a query can filter them, join them to the dimension
tables, group them, rank them and pivot them. Underneath, Vess reads them with MDX, and it reads only
what the query wants:

- **Each condition about one dimension is tried against that dimension's members first**, and only
  the members it holds for are asked for. That covers `=`, `IN`, `LIKE`, `<>`, anything you can
  write about one column, and the outline functions: `IS_DESCENDANT(PRODUCT, '100')` reads the
  colas and nothing else. A condition about two dimensions, or about the value, is applied to the
  rows that come back. Either way the answer is the same; only the reading differs.
- A dimension narrowed to one member goes in the MDX query's slicer; the largest dense one goes on
  the columns; the rest are crossed on the rows with `NON EMPTY`, so empty combinations are left out.
- A big read is split into queries of a sensible size, made one at a time as the rows are used, so
  a tool showing the first 500 rows of CELLS reads only those.

All of Sample.Basic, every value at every level, is just over half a million values, which Vess
reads in about three and a half seconds; the values for one market take a fifth of a second.

Some things to know:

- **A cell that's #Missing isn't a row.** The tables hold the values the cube has.
- **Members are their names, not their aliases**: `PRODUCT = '100-10'`, not `'Cola'`. Use
  `MEMBER_ALIAS(PRODUCT) = 'Cola'`, or join the dimension's table, to work with aliases.
- **Names are compared exactly**, as SQL compares text, so `MARKET = 'new york'` matches nothing.
  Table and column names, on the other hand, aren't case sensitive.
- **CELLS has every level**, so summing it counts each value again in every total above it. Sum
  FACTS, whose values are those loaded, or pick the level you want: `PRODUCT = 'Product'` for the
  total, `IS_CHILD(MARKET, 'Market')` for the regions.
- **Shared members appear once**, where they're stored; `IS_DESCENDANT` counts them where they're
  shared, so Diet's members are under Diet.
- Names that SQL reserves, such as `YEAR`, `TIME` and `VALUE`, needn't be quoted.

## Changing things

| Statement | Does |
| --- | --- |
| `INSERT INTO VARIABLES (NAME, VALUE) VALUES ('CurMonth', 'Feb')` | Creates a variable for the cube; give `SCOPE` as `Application` or `Server` for those |
| `UPDATE SERVER.VARIABLES SET VALUE = 'Feb' WHERE NAME = 'CurMonth'` | Changes a variable, wherever it is |
| `UPDATE SCRIPTS SET CONTENT = '...' WHERE NAME = 'CalcAll'` | Saves a calc script |
| `DELETE FROM SERVER.SESSIONS WHERE USER_NAME = 'jdoe'` | Logs someone off |
| `UPDATE SERVER.APPLICATIONS SET STATUS = 'Stopped' WHERE NAME = 'Demo'` | Stops an application |
| `INSERT INTO CELLS VALUES ('Jan', 'Sales', '100-10', 'New York', 'Budget', 700)` | Writes a value |
| `UPDATE FACTS SET VALUE = VALUE * 1.05 WHERE SCENARIO = 'Budget'` | Raises the budget by five percent |
| `INSERT INTO CELLS SELECT YEAR, MEASURES, PRODUCT, MARKET, 'Budget', VALUE FROM FACTS WHERE SCENARIO = 'Actual'` | Copies actuals to budget |
| `DELETE FROM FACTS WHERE SCENARIO = 'Budget' AND YEAR = 'Dec'` | Clears December's budget |

Writes to CELLS and FACTS go through Essbase's streaming data load, with no load rule: inserting a
row writes its value, overwriting what was there; updating one writes the new value; deleting one
makes it #Missing. A member that's calculated rather than stored, such as a Dynamic Calc quarter,
can't be written to, and Vess says so rather than letting Essbase drop the value. Upper-level stored
values are written as given, and the cube's next aggregation will replace them.

A `WHERE` clause can say anything a query can, and an update changes exactly the rows a `SELECT`
with it would show. Columns that can't change, such as a variable's name, are refused rather than
ignored.

There are no transactions: each statement takes effect as it runs. A read-only connection, from
`readonly=true` or `setReadOnly(true)`, refuses all of the above, calc procedures included.

## MDX

A statement that's MDX goes straight to Essbase, through the
[Thriller](https://github.com/jasonwjones/thriller-mdx-jdbc) driver, so one connection answers
both:

```
SELECT {[Jan], [Feb]} ON COLUMNS, [Market].Children ON ROWS
FROM Sample.Basic WHERE ([Sales], [Actual])
```

Thriller's settings, such as `members=alias` and `shape=long`, can go on a Vess URL for these, and
its hints work in the queries; see its README.

## Stopped applications

Reading a cube's outline starts its application, so browsing a server would start every stopped
application on it. Vess doesn't: for a cube whose application is stopped, a tool sees the tables
that don't depend on the outline, such as VARIABLES and SCRIPTS, but not CELLS, FACTS or the
dimension tables until the application is started, by `CALL SERVER.START_APPLICATION('Sample')`,
by any query of its cubes, or elsewhere. Set `browse.stopped=true` to show everything anyway,
starting applications as they're browsed.

## How it works

Vess embeds [Apache Calcite](https://calcite.apache.org) as its SQL engine, with tables that read
from Essbase as the engine asks for rows. A cube's outline is read whole, in one request for its
XML, and kept for a while across connections; the dimension tables, the members a query wants and
the outline functions all come from it. Cells are read with MDX built for each query, and written
with the streaming data load. Everything else is essbase-rest-client calls, a few to the REST API
itself.

## History

Vess was a 2015 experiment, written up on [jasonwjones.com](https://www.jasonwjones.com/2015/04/vess-virtual-essbase-jdbc-driver/):
a JDBC driver built on the H2 database engine and the Essbase Java API, which read substitution
variables, sessions, statistics and outlines as tables, and loaded data with plain `INSERT`s. It
never had a public release, and it needed Oracle's Essbase jars. That code is still in this
repository: its history ends at the tag `vess-1.x`, and the 2016 work that was never committed, much
of it on reading cube data, is on the `1.x-data-reading` branch.

This version is a rewrite for the REST API, and does what that one set out to do.

## Building and testing

Java 17 or newer, and Maven.

```
mvn package
```

leaves both jars in `target/`, after the tests, which need no server: they run against Sample.Basic's
outline and a stand-in for its data. To run them against a live server with Sample.Basic as well,
which also checks the cookbook:

```
VESS_URL=jdbc:essbase:vess:http://localhost:9000/essbase \
VESS_USER=admin VESS_PASSWORD=... mvn test
```

They only read, unless `VESS_WRITE_TESTS=true` is set too, when they also create and delete a
substitution variable and a calc script, and change three of Sample.Basic's budget values and put
them back. `-Dvess.examples.record=true` records the cookbook's outputs afresh.

## Releasing

Set the release version in `pom.xml`, commit, and push a matching tag:

```
git tag v1.0.0
git push origin v1.0.0
```

The release workflow checks the tag against the pom, builds and tests, publishes to Maven Central,
and publishes a GitHub release with the single jar attached. Then move the pom on to the next
`-SNAPSHOT` version.

## License

[Apache License 2.0](LICENSE).
