package com.andisec.comparison;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@SpringBootTest
class ComparisonApplicationTests {

    @Test
    void contextLoads() throws SQLException {

        DataSource dataSource1 = DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/data1")
                .username("root")
                .password("123456")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
        Connection conn1 = dataSource1.getConnection();
        DatabaseMetaData meta1 = conn1.getMetaData();
        Set<String> tables = getTables(meta1);
        for (String table : tables) {
            System.out.println(table);
        }
    }

    private Set<String> getTables(DatabaseMetaData meta) throws SQLException {
        String catalog = meta.getConnection().getCatalog();
        Set<String> tables = new HashSet<>();
        try (ResultSet rs = meta.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }
}
