/*
# Where members are shared

The dimension tables have each member once. MEMBERS has every place each one sits, shared ones included, which shows Diet's alternate hierarchy.
*/
SELECT NAME, PARENT, SHARED, BASE_MEMBER, STORAGE
FROM Sample.Basic.MEMBERS
WHERE DIMENSION = 'Product' AND (SHARED OR NAME = 'Diet')
ORDER BY OUTLINE_ORDER
