# Vess Projections

## Design Statement

Given a list of lists, the Vess projection will create a unique key for each value, allow for 
iterating, and then use a delegate to fetch the data.

The key thing to consider is that all data in the cube is predetermined based on the list of lists
(LoL). So we need to pretend that it all exists and can jump to any piece of data immediately based
on the combination of elements.

Future item to worry about: key size bigger than 64 bits.

## Fetching Data

A pluggable data fetch strategy is 


## Assumptions

Data space is every possible piece of data, with null representing #Missing
Queries into the data space need to be defined as generically as possible, but with as many hints 
to the optimizer as possible. 

For example, certain POV members are specified concretely


Data Request
	With
		Scenario = Actual
		Year = FY15
		Time = Q1, Q2, Q3, Q4
		all others default
		
		
Cache mechanism
	Is optional, pluggable
	multiple strategies for representing known attributes of data		
	Must choose sparse combos to serve as cache entries?

Retriever
	Is free to formulate requests however it wants
	Sends back data in a representative format
		I.e, retriever might know that certain data is blank, so it can compactly represent as such
	Is initialized with cube structure upon startup; remains fixed
	Acts as factory for projections?
	Sparse elimination
	
		
Remapper
	Maps returned data (such as it is) to the requested format (table)
	


Absolute worst case performance scenario:
	Table Projection represents the entire search space (all dims, all members)
	No optimization is used for group definitions, no cache
	No compact returns
		Scenario: *
		Time *
		etc
		GridRetriever comes up with set of requests to find data for all
		
	

DimensionComprehension
	1..* Dimensions
		Name (must all be unique)
		Root
			Children, etc + tree operations
	
	
Request
	Dimension
		Scenario = Actual
		etc.
		
		
	