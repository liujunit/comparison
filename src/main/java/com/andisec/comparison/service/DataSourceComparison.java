package com.andisec.comparison.service;


import com.andisec.comparison.model.DataDiff;
import com.andisec.comparison.model.DiffResult;
import com.andisec.comparison.model.FormDataSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
@Slf4j
public class DataSourceComparison {

    /**
     * jdbc:mysql://localhost:3306/data1?useSSL=false&serverTimezone=UTC|root|123456
     * @param formDataSource
     * @return
     * @throws SQLException
     */
    public DiffResult comparisonDataSource(FormDataSource formDataSource) throws SQLException {
        String url1 = formDataSource.getUrl1();
        String[] splitUrl1 = url1.split("\\|");
        DataSource dataSource1 = DataSourceBuilder.create()
                .url(splitUrl1[0])
                .username(splitUrl1[1])
                .password(splitUrl1[2])
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();


        String url2 = formDataSource.getUrl2();
        String[] splitUrl2 = url2.split("\\|");
        DataSource dataSource2 = DataSourceBuilder.create()
                .url(splitUrl2[0])
                .username(splitUrl2[1])
                .password(splitUrl2[2])
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();

        DiffResult diffResult = compareTableSchemas(dataSource1, dataSource2);
        return diffResult;
    }


    public DiffResult compareTableSchemas(DataSource firstDataSource, DataSource secondDataSource) throws SQLException {
        DiffResult diffResult = new DiffResult();
        List<DataDiff> differences = new ArrayList<>();

        try (Connection conn1 = firstDataSource.getConnection();
             Connection conn2 = secondDataSource.getConnection()) {
            DatabaseMetaData meta1 = conn1.getMetaData();
            DatabaseMetaData meta2 = conn2.getMetaData();
            // 获取两个数据库的表列表
            Set<String> tables1 = getTables(meta1);
            Set<String> tables2 = getTables(meta2);

            diffResult.setNum1(tables1.size());
            diffResult.setNum2(tables2.size());

            // 比较表名差异
            Set<String> allTables = new HashSet<>();
            allTables.addAll(tables1);
            allTables.addAll(tables2);

            for (String table : allTables) {
                boolean flag = true;
                if (!tables2.contains(table)) {
                    flag = false;
                    differences.add(new DataDiff(table, "缺失", "不一致"));
                    continue;
                }

                if (!tables1.contains(table)) {
                    flag = false;
                    differences.add(new DataDiff("缺失", table, "不一致"));
                    continue;
                }

                //比较表结构
                List<DataDiff> DataDiffs1 = compareTableColumns(meta1, meta2, table);
                if (DataDiffs1.size() > 0) {
                    differences.addAll(DataDiffs1);
                    flag = false;
                    continue;
                }

                //对比数据
                List<DataDiff> DataDiffs = compareTableData(table, firstDataSource, secondDataSource);
                if (DataDiffs.size() > 0) {
                    differences.addAll(DataDiffs);
                    flag = false;
                }

                if (flag) {
                    differences.add(new DataDiff(table, table, "一致"));
                }
            }
        }
        diffResult.setDataDiffs(differences);
        return diffResult;
    }

    private Set<String> getTables(DatabaseMetaData meta) throws SQLException {
        Set<String> tables = new HashSet<>();
        String catalog = meta.getConnection().getCatalog();
        try (ResultSet rs = meta.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private List<DataDiff> compareTableColumns(DatabaseMetaData meta1, DatabaseMetaData meta2, String tableName)
            throws SQLException {
        List<DataDiff> differences = new ArrayList<>();

        Map<String, ColumnInfo> columns1 = getColumns(meta1, tableName);
        Map<String, ColumnInfo> columns2 = getColumns(meta2, tableName);

        Set<String> allColumns = new HashSet<>();
        allColumns.addAll(columns1.keySet());
        allColumns.addAll(columns2.keySet());

        for (String column : allColumns) {
            if (!columns1.containsKey(column)) {
                differences.add(new DataDiff(tableName, tableName  + "（列：" + column + "）", "不一致（数据库1中不存在）"));
                continue;
            }
            if (!columns2.containsKey(column)) {
                differences.add(new DataDiff(tableName  + "（列：" + column + "）", tableName, "不一致（数据库2中不存在）"));
                continue;
            }

            ColumnInfo col1 = columns1.get(column);
            ColumnInfo col2 = columns2.get(column);

            if (!col1.equals(col2)) {
                differences.add(new DataDiff(tableName  + "（列：" + column + "）", tableName  + "（列：" + column + "）", "不一致（列对比不同）"));
            }
        }

        return differences;
    }

    private Map<String, ColumnInfo> getColumns(DatabaseMetaData meta, String tableName) throws SQLException {
        Map<String, ColumnInfo> columns = new HashMap<>();
        String catalog = meta.getConnection().getCatalog();
        try (ResultSet rs = meta.getColumns(catalog, null, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                int nullable = rs.getInt("NULLABLE");
                columns.put(columnName, new ColumnInfo(columnName, typeName, columnSize, nullable));
            }
        }
        return columns;
    }



    public List<DataDiff> compareTableData(String tableName, DataSource firstDataSource, DataSource secondDataSource) throws SQLException {
        List<DataDiff> differences = new ArrayList<>();

        try (Connection conn1 = firstDataSource.getConnection();
             Connection conn2 = secondDataSource.getConnection();
             Statement stmt1 = conn1.createStatement();
             Statement stmt2 = conn2.createStatement()) {

            // 获取主键信息
            List<String> primaryKeys = getPrimaryKeys(conn1.getMetaData(), tableName);
            if (primaryKeys.isEmpty()) {
                differences.add(new DataDiff(tableName, "", "不一致（没有主键，无法比较数据）"));
                return differences;
            }

            // 获取所有列
            List<String> columns = getColumnss(conn1.getMetaData(), tableName);

            // 查询两个表的数据
            String query = "SELECT " + String.join(", ", columns) + " FROM " + tableName +
                    " ORDER BY " + String.join(", ", primaryKeys);

            Map<String, Map<String, Object>> data1 = getData(stmt1, query, primaryKeys, columns);
            Map<String, Map<String, Object>> data2 = getData(stmt2, query, primaryKeys, columns);

            // 比较数据
            differences.addAll(compareData(data1, data2, tableName, primaryKeys));
        }

        return differences;
    }

    private List<String> getPrimaryKeys(DatabaseMetaData meta, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        String catalog = meta.getConnection().getCatalog();
        try (ResultSet rs = meta.getPrimaryKeys(catalog, null, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        return primaryKeys;
    }

    private List<String> getColumnss(DatabaseMetaData meta, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String catalog = meta.getConnection().getCatalog();
        try (ResultSet rs = meta.getColumns(catalog, null, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    private Map<String, Map<String, Object>> getData(Statement stmt, String query,
                                                     List<String> primaryKeys, List<String> columns)
            throws SQLException {
        Map<String, Map<String, Object>> data = new HashMap<>();

        try (ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                // 构建主键字符串作为Map的key
                StringBuilder keyBuilder = new StringBuilder();
                for (String pk : primaryKeys) {
                    if (keyBuilder.length() > 0) keyBuilder.append("|");
                    keyBuilder.append(rs.getObject(pk));
                }
                String key = keyBuilder.toString();

                // 存储行数据
                Map<String, Object> row = new HashMap<>();
                for (String column : columns) {
                    row.put(column, rs.getObject(column));
                }
                data.put(key, row);
            }
        }

        return data;
    }

    private List<DataDiff> compareData(Map<String, Map<String, Object>> data1,
                                     Map<String, Map<String, Object>> data2,
                                     String tableName, List<String> primaryKeys) {
        List<DataDiff> differences = new ArrayList<>();

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(data1.keySet());
        allKeys.addAll(data2.keySet());

        for (String key : allKeys) {
            if (!data1.containsKey(key)) {
                differences.add(new DataDiff(tableName, tableName + "（主键：" + key.replace("|", "") + "）", "不一致（数据库1中不存在）"));
                continue;
            }
            if (!data2.containsKey(key)) {
                differences.add(new DataDiff(tableName + "（主键：" + key.replace("|", "") + "）", tableName, "不一致（数据库2中不存在）"));
                continue;
            }

            Map<String, Object> row1 = data1.get(key);
            Map<String, Object> row2 = data2.get(key);

            for (Map.Entry<String, Object> entry : row1.entrySet()) {
                String column = entry.getKey();
                Object value1 = entry.getValue();
                Object value2 = row2.get(column);

                if (!Objects.equals(value1, value2)) {
                    differences.add(new DataDiff(tableName + "（主键：" + key.replace("|", "") + "，列："+ column +"）", tableName + "（主键：" + key.replace("|", "") + "，列："+ column +"）", "不一致（数据不同）"));
                }
            }
        }

        return differences;
    }

    @Data
    @AllArgsConstructor
    private static class ColumnInfo {
        private String name;
        private String type;
        private int size;
        private int nullable;
    }


}
