package database.execution;

import java.sql.SQLException;
import java.util.List;

import database.objectParse.Status;

/**
 * @author amit
 *
 */
public interface IExecuteQueries {
	void insertToTable(String tableName, List<Object> objects, Status status) throws SQLException;

	int insertAndGenerateId(String tableName, List<Object> objects, Status status) throws SQLException;

	void insertToTable(String tableName, List<Object> objects) throws SQLException;

	int insertAndGenerateId(String tableName, List<Object> objects) throws SQLException;

	void deleteValueFromTable(String tableName, String objectName, Object object) throws SQLException;

	void deleteValuesFromTable(String tableName, List<String> objectNames, List<Object> objects) throws SQLException;

	void deleteValueFromTable(String tableName, String objectName, Object object, Status status) throws SQLException;

	void deleteValuesFromTable(String tableName, List<String> objectNames, List<Object> objects, Status status)
			throws SQLException;

	List<List<Object>> selectColumnsByValue(String tableName, String objectName, Object object, String columnsToSelect)
			throws SQLException;

	List<List<Object>> selectColumnsByPartialValue(String tableName, String objectName, Object object,
			String columnsToSelect) throws SQLException;

	List<List<Object>> selectColumnsByValues(String tableName, List<String> objectNames, List<Object> objectsValues,
			String columnsToSelect) throws SQLException;

	List<List<Object>> selectColumnsByValue(String tableName, String objectName, Object object, String columnsToSelect,
			Status status) throws SQLException;

	List<List<Object>> selectColumnsByPartialValue(String tableName, String objectName, Object object,
			String columnsToSelect, Status status) throws SQLException;

	List<List<Object>> selectColumnsByValues(String tableName, List<String> objectNames, List<Object> objectsValues,
			String columnsToSelect, Status status) throws SQLException;
}
