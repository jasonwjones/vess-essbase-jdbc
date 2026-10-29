# Vess cookbook

SQL against Sample.Basic, each query with the rows Vess answers with. Every query is a file in this
folder, so you can open it in a tool and run it, with Sample.Basic as the cube to work in
(`;app=Sample;cube=Basic` on the URL), which the outline functions need.

The outputs are recorded from Essbase 26.1 by `ExamplesTest`, which also checks them, and builds this
page, whenever the tests run against a server. They only read.

## The cube's dimensions ([01-dimensions.sql](01-dimensions.sql))

Every cube is a schema, and DIMENSIONS is one of its tables: what each dimension is, how big, and how deep. The attribute dimensions are there too; this leaves them out.

```
SELECT NAME, TYPE, DENSITY, MEMBERS, STORED_MEMBERS, GENERATIONS, LEVELS
FROM Sample.Basic.DIMENSIONS
WHERE TYPE <> 'Attribute'
ORDER BY NUMBER
```

```
+----------+----------+---------+---------+----------------+-------------+--------+
| NAME     | TYPE     | DENSITY | MEMBERS | STORED_MEMBERS | GENERATIONS | LEVELS |
+----------+----------+---------+---------+----------------+-------------+--------+
| Year     | Time     | Dense   | 17      | 12             | 3           | 3      |
| Measures | Accounts | Dense   | 17      | 11             | 4           | 4      |
| Product  | Regular  | Sparse  | 22      | 19             | 3           | 3      |
| Market   | Regular  | Sparse  | 25      | 25             | 3           | 3      |
| Scenario | Regular  | Dense   | 5       | 2              | 2           | 2      |
+----------+----------+---------+---------+----------------+-------------+--------+
```

## A dimension as a table ([02-a-dimension-as-a-table.sql](02-a-dimension-as-a-table.sql))

Each dimension is a table of its own, a row for each member. Besides the alias, parent, level and generation, there's a column for each named generation, holding the member's ancestor there, and one for each attribute, with its type: Caffeinated is a boolean, Ounces a number and Intro Date a date.

```
SELECT NAME, ALIAS, Category, Caffeinated, Ounces, "Pkg Type", "Intro Date"
FROM Sample.Basic.Product
WHERE LEVEL = 0
ORDER BY OUTLINE_ORDER
```

```
+--------+--------------------+----------+-------------+--------+----------+------------+
| NAME   | ALIAS              | Category | Caffeinated | Ounces | Pkg Type | Intro Date |
+--------+--------------------+----------+-------------+--------+----------+------------+
| 100-10 | Cola               | 100      | true        | 12.0   | Can      | 1996-03-25 |
| 100-20 | Diet Cola          | 100      | true        | 12.0   | Can      | 1996-04-01 |
| 100-30 | Caffeine Free Cola | 100      | false       | 16.0   | Bottle   | 1996-04-01 |
| 200-10 | Old Fashioned      | 200      | true        | 12.0   | Bottle   | 1995-09-27 |
| 200-20 | Diet Root Beer     | 200      | true        | 16.0   | Bottle   | 1996-07-26 |
| 200-30 | Sasparilla         | 200      | false       | 12.0   | Bottle   | 1996-12-10 |
| 200-40 | Birch Beer         | 200      | false       | 16.0   | Bottle   | 1996-12-10 |
| 300-10 | Dark Cream         | 300      | true        | 20.0   | Bottle   | 1996-06-26 |
| 300-20 | Vanilla Cream      | 300      | true        | 20.0   | Bottle   | 1996-06-26 |
| 300-30 | Diet Cream         | 300      | true        | 12.0   | Can      | 1996-06-26 |
| 400-10 | Grape              | 400      | false       | 32.0   | Bottle   | 1996-10-01 |
| 400-20 | Orange             | 400      | false       | 32.0   | Bottle   | 1996-10-01 |
| 400-30 | Strawberry         | 400      | false       | 32.0   | Bottle   | 1996-10-01 |
+--------+--------------------+----------+-------------+--------+----------+------------+
```

## One value ([03-one-value.sql](03-one-value.sql))

CELLS holds every value in the cube, with a column for each dimension. Name a member of each and you have one cell. Vess turns the conditions into the MDX that reads just that.

```
SELECT VALUE
FROM Sample.Basic.CELLS
WHERE Year = 'Jan' AND Measures = 'Sales' AND Product = '100-10' AND Market = 'New York' AND Scenario = 'Actual'
```

```
+-------+
| VALUE |
+-------+
| 678.0 |
+-------+
```

## Totals and calculations ([04-totals-and-calculations.sql](04-totals-and-calculations.sql))

CELLS has members at every level, so it answers with Essbase's own totals and dynamic calculations: here a quarter, a region and a product family, for every scenario, Variance and Variance % included.

```
SELECT Scenario, VALUE
FROM Sample.Basic.CELLS
WHERE Year = 'Qtr1' AND Measures = 'Sales' AND Product = '100' AND Market = 'East'
```

```
+------------+-------------------+
| Scenario   | VALUE             |
+------------+-------------------+
| Scenario   | 6292.0            |
| Actual     | 6292.0            |
| Budget     | 5870.0            |
| Variance   | 422.0             |
| Variance % | 7.189097103918228 |
+------------+-------------------+
```

## The cube as a fact table ([05-facts-by-attribute.sql](05-facts-by-attribute.sql))

FACTS holds just the values loaded at level 0, so they add up the way SQL expects. Joined to the Product table, they can be grouped by anything it has: here by category and whether the drink has caffeine.

```
SELECT p.Category, p.Caffeinated, SUM(f.VALUE) AS Sales
FROM Sample.Basic.FACTS f
JOIN Sample.Basic.Product p ON f.Product = p.NAME
WHERE f.Measures = 'Sales' AND f.Scenario = 'Actual'
GROUP BY p.Category, p.Caffeinated
ORDER BY p.Category, p.Caffeinated
```

```
+----------+-------------+----------+
| Category | Caffeinated | Sales    |
+----------+-------------+----------+
| 100      | false       | 12841.0  |
| 100      | true        | 93293.0  |
| 200      | false       | 29309.0  |
| 200      | true        | 79777.0  |
| 300      | true        | 101405.0 |
| 400      | false       | 84230.0  |
+----------+-------------+----------+
```

## Asking the outline ([06-outline-functions.sql](06-outline-functions.sql))

Functions such as IS_DESCENDANT, IS_CHILD and HAS_UDA answer from the outline. In a WHERE clause they narrow what's read, since Vess tries each against the members first. Diet shares products from other families, and they count as under it.

```
SELECT Product, MEMBER_ALIAS(Product) AS Alias, SUM(VALUE) AS Sales
FROM Sample.Basic.FACTS
WHERE IS_DESCENDANT(Product, 'Diet') AND HAS_UDA(Market, 'Major Market')
  AND Measures = 'Sales' AND Scenario = 'Actual'
GROUP BY Product
ORDER BY Product
```

```
+---------+----------------+---------+
| Product | Alias          | Sales   |
+---------+----------------+---------+
| 100-20  | Diet Cola      | 14902.0 |
| 200-20  | Diet Root Beer | 21078.0 |
| 300-30  | Diet Cream     | 13313.0 |
+---------+----------------+---------+
```

## Ranking with SQL ([07-ranking.sql](07-ranking.sql))

All of SQL is there once the values are rows. Here each region's best-selling product, ranked by a window function, with the region from the Market table's generation column.

```
SELECT Region, Product, Sales
FROM (
  SELECT m.Region, f.Product, SUM(f.VALUE) AS Sales,
         RANK() OVER (PARTITION BY m.Region ORDER BY SUM(f.VALUE) DESC) AS Place
  FROM Sample.Basic.FACTS f
  JOIN Sample.Basic.Market m ON f.Market = m.NAME
  WHERE f.Measures = 'Sales' AND f.Scenario = 'Actual'
  GROUP BY m.Region, f.Product
) ranked
WHERE Place = 1
ORDER BY Region
```

```
+---------+---------+---------+
| Region  | Product | Sales   |
+---------+---------+---------+
| Central | 300-30  | 17885.0 |
| East    | 100-10  | 23205.0 |
| South   | 100-10  | 10604.0 |
| West    | 300-10  | 15803.0 |
+---------+---------+---------+
```

## Actual against budget ([08-actual-against-budget.sql](08-actual-against-budget.sql))

Scenarios side by side, the way a report wants them, by turning rows into columns with conditional sums.

```
SELECT Market,
       SUM(CASE WHEN Scenario = 'Actual' THEN VALUE END) AS Actual,
       SUM(CASE WHEN Scenario = 'Budget' THEN VALUE END) AS Budget
FROM Sample.Basic.CELLS
WHERE Year = 'Year' AND Measures = 'Sales' AND Product = 'Product'
  AND IS_CHILD(Market, 'Market')
GROUP BY Market
ORDER BY Market
```

```
+---------+----------+----------+
| Market  | Actual   | Budget   |
+---------+----------+----------+
| Central | 129680.0 | 125180.0 |
| East    | 87398.0  | 78950.0  |
| South   | 50846.0  | 49100.0  |
| West    | 132931.0 | 119850.0 |
+---------+----------+----------+
```

## Where members are shared ([09-shared-members.sql](09-shared-members.sql))

The dimension tables have each member once. MEMBERS has every place each one sits, shared ones included, which shows Diet's alternate hierarchy.

```
SELECT NAME, PARENT, SHARED, BASE_MEMBER, STORAGE
FROM Sample.Basic.MEMBERS
WHERE DIMENSION = 'Product' AND (SHARED OR NAME = 'Diet')
ORDER BY OUTLINE_ORDER
```

```
+--------+---------+--------+-------------+---------+
| NAME   | PARENT  | SHARED | BASE_MEMBER | STORAGE |
+--------+---------+--------+-------------+---------+
| Diet   | Product | false  | null        | Store   |
| 100-20 | Diet    | true   | 100-20      | Shared  |
| 200-20 | Diet    | true   | 200-20      | Shared  |
| 300-30 | Diet    | true   | 300-30      | Shared  |
+--------+---------+--------+-------------+---------+
```

## Every alias table ([10-aliases.sql](10-aliases.sql))

Each alias table is a column of the dimension's table; ALIASES has them all as rows, for when they're wanted together. Here are four of Sample.Basic's six.

```
SELECT MEMBER, ALIAS_TABLE, ALIAS
FROM Sample.Basic.ALIASES
WHERE MEMBER IN ('100-10', 'Qtr1') AND ALIAS_TABLE IN ('Default', 'Long Names', 'GermanNames', 'RussianNames')
ORDER BY MEMBER, ALIAS_TABLE
```

```
+--------+--------------+------------+
| MEMBER | ALIAS_TABLE  | ALIAS      |
+--------+--------------+------------+
| 100-10 | Default      | Cola       |
| 100-10 | GermanNames  | Cola       |
| 100-10 | RussianNames | Кола       |
| Qtr1   | GermanNames  | 1. Quartal |
| Qtr1   | Long Names   | Quarter1   |
| Qtr1   | RussianNames | Квартал 1  |
+--------+--------------+------------+
```

## Calc scripts ([11-calc-scripts.sql](11-calc-scripts.sql))

SCRIPTS holds the cube's calc and MDX scripts, text and all; here each line break is shown as a space. Updating CONTENT saves a script, and each calc script is also a procedure: CALL Sample.Basic.CalcAll() runs this one.

```
SELECT TYPE, NAME, TRIM(REPLACE(CONTENT, U&'\000A', ' ')) AS CONTENT
FROM Sample.Basic.SCRIPTS
WHERE TYPE = 'Calc'
```

```
+------+---------+-----------------------------------------------------------------+
| TYPE | NAME    | CONTENT                                                         |
+------+---------+-----------------------------------------------------------------+
| Calc | CalcAll | SET UPDATECALC OFF; SET CACHE HIGH; SET MSG SUMMARY;  CALC ALL; |
+------+---------+-----------------------------------------------------------------+
```

## MDX too ([12-mdx.sql](12-mdx.sql))

A statement that's MDX goes straight to Essbase, through the Thriller driver, so one connection answers both.

```
SELECT {[Jan], [Feb], [Mar]} ON COLUMNS,
[Market].Children ON ROWS
FROM Sample.Basic
WHERE ([Sales], [Actual], [Product])
```

```
+---------+---------+---------+---------+
| Market  | Jan     | Feb     | Mar     |
+---------+---------+---------+---------+
| East    | 6780.0  | 6920.0  | 6921.0  |
| West    | 10436.0 | 10564.0 | 10674.0 |
| South   | 3976.0  | 4082.0  | 4055.0  |
| Central | 10346.0 | 10503.0 | 10563.0 |
+---------+---------+---------+---------+
```

## What the server knows ([13-mdx-functions.sql](13-mdx-functions.sql))

The server's own tables are in ESSBASE.SERVER: applications, cubes, sessions, users, jobs, logs, and the MDX functions it understands, with their syntax.

```
SELECT NAME, SYNTAX
FROM ESSBASE.SERVER.MDX_FUNCTIONS
WHERE NAME LIKE 'Top%'
ORDER BY NAME
```

```
+------------+----------------------------------------------------------------------+
| NAME       | SYNTAX                                                               |
+------------+----------------------------------------------------------------------+
| TopCount   | TopCount ( set , index [,numeric_value_expression ] )                |
| TopPercent | TopPercent ( set, percentage, numeric_value_expression )             |
| TopSum     | TopSum ( set, numeric_value_expression1, numeric_value_expression2 ) |
+------------+----------------------------------------------------------------------+
```
