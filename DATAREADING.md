# Data Reading Strategy for Vess

 - The data table has a compound primary key consisting of every dimension in the
   database
 - Only stored members are considered
 - The maximum address space is 64 bits
 - All rows for the table are considered to exist (null corresponds to #Missing)
 - Therefore scanning for just existing data would likely include a filter clause :
   SELECT whatever FROM SAMPLE_BASIC_DATA WHERE DATA IS NOT NULL
 - Every member from the cube must be in the data model ahead of time
 - The data modeler creates a lookup mechanism that translates from bits to members
   and vice versa
 - You can iterate over every row in the table (this does not correpsond to iterating
   over the long because the bits 'stop' in certain ranges)
 
 - WHERE predicates on other dimensions filter the potential bitset that woud be
   iterated over
   
 - Potential where clauses
 	WHERE DATA < 20
 	WHERE Time = 'Jan'
 	WHERE Time IN ('Jan', 'Feb', 'Mar')
 	WHERE LEVEL(Time) = 0
 	WHERE PARENT(Time) = 'Qtr1'
 	WHERE LEFT(Time, 1) = 'P'
 	WHERE LEFT(Time, 1) IN ('P', 'Q')
 	
 - The order of rows returned is based on the dimension order and the order of members in 
   those dimensions
   
 -
 	
 	
 ## Generating the default data model
 
  - ID: Long representation of bit combinations
  - dim1
  - dim2
  - dim3
  - data value
  - null means missing
  
  - default only projects level-0
 	