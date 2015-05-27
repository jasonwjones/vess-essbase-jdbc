package com.saxifrages.vess.h2vess;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VessDatabaseMetaData implements DatabaseMetaData {

	private static final Logger logger = LoggerFactory.getLogger(VessDatabaseMetaData.class);
	
	private DatabaseMetaData h2;
	
	public VessDatabaseMetaData(DatabaseMetaData h2) {
		this.h2 = h2;
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return h2.unwrap(iface);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return h2.isWrapperFor(iface);
	}

	public boolean allProceduresAreCallable() throws SQLException {
		return h2.allProceduresAreCallable();
	}

	public boolean allTablesAreSelectable() throws SQLException {
		return h2.allTablesAreSelectable();
	}

	public String getURL() throws SQLException {
		return h2.getURL();
	}

	public String getUserName() throws SQLException {
		return h2.getUserName();
	}

	public boolean isReadOnly() throws SQLException {
		return h2.isReadOnly();
	}

	public boolean nullsAreSortedHigh() throws SQLException {
		return h2.nullsAreSortedHigh();
	}

	public boolean nullsAreSortedLow() throws SQLException {
		return h2.nullsAreSortedLow();
	}

	public boolean nullsAreSortedAtStart() throws SQLException {
		return h2.nullsAreSortedAtStart();
	}

	public boolean nullsAreSortedAtEnd() throws SQLException {
		return h2.nullsAreSortedAtEnd();
	}

	public String getDatabaseProductName() throws SQLException {
		return h2.getDatabaseProductName();
	}

	public String getDatabaseProductVersion() throws SQLException {
		return h2.getDatabaseProductVersion();
	}

	public String getDriverName() throws SQLException {
		return h2.getDriverName();
	}

	public String getDriverVersion() throws SQLException {
		return h2.getDriverVersion();
	}

	public int getDriverMajorVersion() {
		return h2.getDriverMajorVersion();
	}

	public int getDriverMinorVersion() {
		return h2.getDriverMinorVersion();
	}

	public boolean usesLocalFiles() throws SQLException {
		return h2.usesLocalFiles();
	}

	public boolean usesLocalFilePerTable() throws SQLException {
		return h2.usesLocalFilePerTable();
	}

	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		return h2.supportsMixedCaseIdentifiers();
	}

	public boolean storesUpperCaseIdentifiers() throws SQLException {
		return h2.storesUpperCaseIdentifiers();
	}

	public boolean storesLowerCaseIdentifiers() throws SQLException {
		return h2.storesLowerCaseIdentifiers();
	}

	public boolean storesMixedCaseIdentifiers() throws SQLException {
		return h2.storesMixedCaseIdentifiers();
	}

	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		return h2.supportsMixedCaseQuotedIdentifiers();
	}

	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		return h2.storesUpperCaseQuotedIdentifiers();
	}

	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		return h2.storesLowerCaseQuotedIdentifiers();
	}

	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		return h2.storesMixedCaseQuotedIdentifiers();
	}

	public String getIdentifierQuoteString() throws SQLException {
		return h2.getIdentifierQuoteString();
	}

	public String getSQLKeywords() throws SQLException {
		return h2.getSQLKeywords();
	}

	public String getNumericFunctions() throws SQLException {
		return h2.getNumericFunctions();
	}

	public String getStringFunctions() throws SQLException {
		return h2.getStringFunctions();
	}

	public String getSystemFunctions() throws SQLException {
		return h2.getSystemFunctions();
	}

	public String getTimeDateFunctions() throws SQLException {
		return h2.getTimeDateFunctions();
	}

	public String getSearchStringEscape() throws SQLException {
		return h2.getSearchStringEscape();
	}

	public String getExtraNameCharacters() throws SQLException {
		return h2.getExtraNameCharacters();
	}

	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		return h2.supportsAlterTableWithAddColumn();
	}

	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		return h2.supportsAlterTableWithDropColumn();
	}

	public boolean supportsColumnAliasing() throws SQLException {
		return h2.supportsColumnAliasing();
	}

	public boolean nullPlusNonNullIsNull() throws SQLException {
		return h2.nullPlusNonNullIsNull();
	}

	public boolean supportsConvert() throws SQLException {
		return h2.supportsConvert();
	}

	public boolean supportsConvert(int fromType, int toType) throws SQLException {
		return h2.supportsConvert(fromType, toType);
	}

	public boolean supportsTableCorrelationNames() throws SQLException {
		return h2.supportsTableCorrelationNames();
	}

	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		return h2.supportsDifferentTableCorrelationNames();
	}

	public boolean supportsExpressionsInOrderBy() throws SQLException {
		return h2.supportsExpressionsInOrderBy();
	}

	public boolean supportsOrderByUnrelated() throws SQLException {
		return h2.supportsOrderByUnrelated();
	}

	public boolean supportsGroupBy() throws SQLException {
		return h2.supportsGroupBy();
	}

	public boolean supportsGroupByUnrelated() throws SQLException {
		return h2.supportsGroupByUnrelated();
	}

	public boolean supportsGroupByBeyondSelect() throws SQLException {
		return h2.supportsGroupByBeyondSelect();
	}

	public boolean supportsLikeEscapeClause() throws SQLException {
		return h2.supportsLikeEscapeClause();
	}

	public boolean supportsMultipleResultSets() throws SQLException {
		return h2.supportsMultipleResultSets();
	}

	public boolean supportsMultipleTransactions() throws SQLException {
		return h2.supportsMultipleTransactions();
	}

	public boolean supportsNonNullableColumns() throws SQLException {
		return h2.supportsNonNullableColumns();
	}

	public boolean supportsMinimumSQLGrammar() throws SQLException {
		return h2.supportsMinimumSQLGrammar();
	}

	public boolean supportsCoreSQLGrammar() throws SQLException {
		return h2.supportsCoreSQLGrammar();
	}

	public boolean supportsExtendedSQLGrammar() throws SQLException {
		return h2.supportsExtendedSQLGrammar();
	}

	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		return h2.supportsANSI92EntryLevelSQL();
	}

	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		return h2.supportsANSI92IntermediateSQL();
	}

	public boolean supportsANSI92FullSQL() throws SQLException {
		return h2.supportsANSI92FullSQL();
	}

	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		return h2.supportsIntegrityEnhancementFacility();
	}

	public boolean supportsOuterJoins() throws SQLException {
		return h2.supportsOuterJoins();
	}

	public boolean supportsFullOuterJoins() throws SQLException {
		return h2.supportsFullOuterJoins();
	}

	public boolean supportsLimitedOuterJoins() throws SQLException {
		return h2.supportsLimitedOuterJoins();
	}

	public String getSchemaTerm() throws SQLException {
		return h2.getSchemaTerm();
	}

	public String getProcedureTerm() throws SQLException {
		return h2.getProcedureTerm();
	}

	public String getCatalogTerm() throws SQLException {
		return h2.getCatalogTerm();
	}

	public boolean isCatalogAtStart() throws SQLException {
		return h2.isCatalogAtStart();
	}

	public String getCatalogSeparator() throws SQLException {
		return h2.getCatalogSeparator();
	}

	public boolean supportsSchemasInDataManipulation() throws SQLException {
		return h2.supportsSchemasInDataManipulation();
	}

	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		return h2.supportsSchemasInProcedureCalls();
	}

	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		return h2.supportsSchemasInTableDefinitions();
	}

	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		return h2.supportsSchemasInIndexDefinitions();
	}

	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		return h2.supportsSchemasInPrivilegeDefinitions();
	}

	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		return h2.supportsCatalogsInDataManipulation();
	}

	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		return h2.supportsCatalogsInProcedureCalls();
	}

	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		return h2.supportsCatalogsInTableDefinitions();
	}

	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		return h2.supportsCatalogsInIndexDefinitions();
	}

	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		return h2.supportsCatalogsInPrivilegeDefinitions();
	}

	public boolean supportsPositionedDelete() throws SQLException {
		return h2.supportsPositionedDelete();
	}

	public boolean supportsPositionedUpdate() throws SQLException {
		return h2.supportsPositionedUpdate();
	}

	public boolean supportsSelectForUpdate() throws SQLException {
		return h2.supportsSelectForUpdate();
	}

	public boolean supportsStoredProcedures() throws SQLException {
		return h2.supportsStoredProcedures();
	}

	public boolean supportsSubqueriesInComparisons() throws SQLException {
		return h2.supportsSubqueriesInComparisons();
	}

	public boolean supportsSubqueriesInExists() throws SQLException {
		return h2.supportsSubqueriesInExists();
	}

	public boolean supportsSubqueriesInIns() throws SQLException {
		return h2.supportsSubqueriesInIns();
	}

	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		return h2.supportsSubqueriesInQuantifieds();
	}

	public boolean supportsCorrelatedSubqueries() throws SQLException {
		return h2.supportsCorrelatedSubqueries();
	}

	public boolean supportsUnion() throws SQLException {
		return h2.supportsUnion();
	}

	public boolean supportsUnionAll() throws SQLException {
		return h2.supportsUnionAll();
	}

	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		return h2.supportsOpenCursorsAcrossCommit();
	}

	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		return h2.supportsOpenCursorsAcrossRollback();
	}

	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		return h2.supportsOpenStatementsAcrossCommit();
	}

	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		return h2.supportsOpenStatementsAcrossRollback();
	}

	public int getMaxBinaryLiteralLength() throws SQLException {
		return h2.getMaxBinaryLiteralLength();
	}

	public int getMaxCharLiteralLength() throws SQLException {
		return h2.getMaxCharLiteralLength();
	}

	public int getMaxColumnNameLength() throws SQLException {
		return h2.getMaxColumnNameLength();
	}

	public int getMaxColumnsInGroupBy() throws SQLException {
		return h2.getMaxColumnsInGroupBy();
	}

	public int getMaxColumnsInIndex() throws SQLException {
		return h2.getMaxColumnsInIndex();
	}

	public int getMaxColumnsInOrderBy() throws SQLException {
		return h2.getMaxColumnsInOrderBy();
	}

	public int getMaxColumnsInSelect() throws SQLException {
		return h2.getMaxColumnsInSelect();
	}

	public int getMaxColumnsInTable() throws SQLException {
		return h2.getMaxColumnsInTable();
	}

	public int getMaxConnections() throws SQLException {
		return h2.getMaxConnections();
	}

	public int getMaxCursorNameLength() throws SQLException {
		return h2.getMaxCursorNameLength();
	}

	public int getMaxIndexLength() throws SQLException {
		return h2.getMaxIndexLength();
	}

	public int getMaxSchemaNameLength() throws SQLException {
		return h2.getMaxSchemaNameLength();
	}

	public int getMaxProcedureNameLength() throws SQLException {
		return h2.getMaxProcedureNameLength();
	}

	public int getMaxCatalogNameLength() throws SQLException {
		return h2.getMaxCatalogNameLength();
	}

	public int getMaxRowSize() throws SQLException {
		return h2.getMaxRowSize();
	}

	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		return h2.doesMaxRowSizeIncludeBlobs();
	}

	public int getMaxStatementLength() throws SQLException {
		return h2.getMaxStatementLength();
	}

	public int getMaxStatements() throws SQLException {
		return h2.getMaxStatements();
	}

	public int getMaxTableNameLength() throws SQLException {
		return h2.getMaxTableNameLength();
	}

	public int getMaxTablesInSelect() throws SQLException {
		return h2.getMaxTablesInSelect();
	}

	public int getMaxUserNameLength() throws SQLException {
		return h2.getMaxUserNameLength();
	}

	public int getDefaultTransactionIsolation() throws SQLException {
		return h2.getDefaultTransactionIsolation();
	}

	public boolean supportsTransactions() throws SQLException {
		return h2.supportsTransactions();
	}

	public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
		return h2.supportsTransactionIsolationLevel(level);
	}

	public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		return h2.supportsDataDefinitionAndDataManipulationTransactions();
	}

	public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
		return h2.supportsDataManipulationTransactionsOnly();
	}

	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		return h2.dataDefinitionCausesTransactionCommit();
	}

	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		return h2.dataDefinitionIgnoredInTransactions();
	}

	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
			throws SQLException {
		logger.info("getProcedures()");
		return h2.getProcedures(catalog, schemaPattern, procedureNamePattern);
	}

	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
			String columnNamePattern) throws SQLException {
		return h2.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern);
	}

	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
			throws SQLException {
		logger.info("Asked for tables for catalog {}, pattern {}, tableNamePattern {}", catalog, schemaPattern, tableNamePattern);
		// Asked for tables for catalog null, pattern null, tableNamePattern null
		return h2.getTables(catalog, schemaPattern, tableNamePattern, types);
	}

	public ResultSet getSchemas() throws SQLException {
		logger.info("getSchemas()");
		return h2.getSchemas();
	}

	public ResultSet getCatalogs() throws SQLException {
		return h2.getCatalogs();
	}

	public ResultSet getTableTypes() throws SQLException {
		return h2.getTableTypes();
	}

	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
			throws SQLException {
		System.out.println("Get columns");
		logger.info("getColumns, catalog={}, schemaPattern={}, tableName={}", catalog, schemaPattern, tableNamePattern);
		// getColumns, catalog=VESS, schemaPattern=VESS_SCHEMA, tableName=SAMPLE_BASIC_DATA
		return h2.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
	}

	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
			throws SQLException {
		return h2.getColumnPrivileges(catalog, schema, table, columnNamePattern);
	}

	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
			throws SQLException {
		return h2.getTablePrivileges(catalog, schemaPattern, tableNamePattern);
	}

	public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
			throws SQLException {
		return h2.getBestRowIdentifier(catalog, schema, table, scope, nullable);
	}

	public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
		return h2.getVersionColumns(catalog, schema, table);
	}

	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return h2.getPrimaryKeys(catalog, schema, table);
	}

	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return h2.getImportedKeys(catalog, schema, table);
	}

	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return h2.getExportedKeys(catalog, schema, table);
	}

	public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable,
			String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
		return h2.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema,
				foreignTable);
	}

	public ResultSet getTypeInfo() throws SQLException {
		return h2.getTypeInfo();
	}

	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
			throws SQLException {
		return h2.getIndexInfo(catalog, schema, table, unique, approximate);
	}

	public boolean supportsResultSetType(int type) throws SQLException {
		return h2.supportsResultSetType(type);
	}

	public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
		return h2.supportsResultSetConcurrency(type, concurrency);
	}

	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		return h2.ownUpdatesAreVisible(type);
	}

	public boolean ownDeletesAreVisible(int type) throws SQLException {
		return h2.ownDeletesAreVisible(type);
	}

	public boolean ownInsertsAreVisible(int type) throws SQLException {
		return h2.ownInsertsAreVisible(type);
	}

	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		return h2.othersUpdatesAreVisible(type);
	}

	public boolean othersDeletesAreVisible(int type) throws SQLException {
		return h2.othersDeletesAreVisible(type);
	}

	public boolean othersInsertsAreVisible(int type) throws SQLException {
		return h2.othersInsertsAreVisible(type);
	}

	public boolean updatesAreDetected(int type) throws SQLException {
		return h2.updatesAreDetected(type);
	}

	public boolean deletesAreDetected(int type) throws SQLException {
		return h2.deletesAreDetected(type);
	}

	public boolean insertsAreDetected(int type) throws SQLException {
		return h2.insertsAreDetected(type);
	}

	public boolean supportsBatchUpdates() throws SQLException {
		return h2.supportsBatchUpdates();
	}

	public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
			throws SQLException {
		return h2.getUDTs(catalog, schemaPattern, typeNamePattern, types);
	}

	public Connection getConnection() throws SQLException {
		return h2.getConnection();
	}

	public boolean supportsSavepoints() throws SQLException {
		return h2.supportsSavepoints();
	}

	public boolean supportsNamedParameters() throws SQLException {
		return h2.supportsNamedParameters();
	}

	public boolean supportsMultipleOpenResults() throws SQLException {
		return h2.supportsMultipleOpenResults();
	}

	public boolean supportsGetGeneratedKeys() throws SQLException {
		return h2.supportsGetGeneratedKeys();
	}

	public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
		return h2.getSuperTypes(catalog, schemaPattern, typeNamePattern);
	}

	public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return h2.getSuperTables(catalog, schemaPattern, tableNamePattern);
	}

	public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern,
			String attributeNamePattern) throws SQLException {
		return h2.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern);
	}

	public boolean supportsResultSetHoldability(int holdability) throws SQLException {
		return h2.supportsResultSetHoldability(holdability);
	}

	public int getResultSetHoldability() throws SQLException {
		return h2.getResultSetHoldability();
	}

	public int getDatabaseMajorVersion() throws SQLException {
		return h2.getDatabaseMajorVersion();
	}

	public int getDatabaseMinorVersion() throws SQLException {
		return h2.getDatabaseMinorVersion();
	}

	public int getJDBCMajorVersion() throws SQLException {
		return h2.getJDBCMajorVersion();
	}

	public int getJDBCMinorVersion() throws SQLException {
		return h2.getJDBCMinorVersion();
	}

	public int getSQLStateType() throws SQLException {
		return h2.getSQLStateType();
	}

	public boolean locatorsUpdateCopy() throws SQLException {
		return h2.locatorsUpdateCopy();
	}

	public boolean supportsStatementPooling() throws SQLException {
		return h2.supportsStatementPooling();
	}

	public RowIdLifetime getRowIdLifetime() throws SQLException {
		return h2.getRowIdLifetime();
	}

	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return h2.getSchemas(catalog, schemaPattern);
	}

	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		return h2.supportsStoredFunctionsUsingCallSyntax();
	}

	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		return h2.autoCommitFailureClosesAllResultSets();
	}

	public ResultSet getClientInfoProperties() throws SQLException {
		return h2.getClientInfoProperties();
	}

	public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
		return h2.getFunctions(catalog, schemaPattern, functionNamePattern);
	}

	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern,
			String columnNamePattern) throws SQLException {
		return h2.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern);
	}

	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern,
			String columnNamePattern) throws SQLException {
		return h2.getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
	}

	public boolean generatedKeyAlwaysReturned() throws SQLException {
		return h2.generatedKeyAlwaysReturned();
	}

	public long getMaxLogicalLobSize() throws SQLException {
		return h2.getMaxLogicalLobSize();
	}

	public boolean supportsRefCursors() throws SQLException {
		return h2.supportsRefCursors();
	}
	
}
