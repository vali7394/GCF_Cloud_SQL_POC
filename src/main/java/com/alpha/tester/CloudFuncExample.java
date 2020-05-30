package com.alpha.tester;


import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

public class CloudFuncExample implements HttpFunction {

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        DataSource dataSource = createConnectionPool();
        List<Guest> guestsList = new ArrayList();
        try (Connection conn = dataSource.getConnection()) {

            String stmt1 = "select guestName,content,entryID from entries";
            try (PreparedStatement voteStmt = conn.prepareStatement(stmt1);) {
                // Execute the statement
                ResultSet guestResults = voteStmt.executeQuery();
                // Convert a ResultSet into Vote objects
                while (guestResults.next()) {
                    String name = guestResults.getString("guestName");
                    String content = guestResults.getString("content");
                    int entryId = guestResults.getInt("entryID");
                    guestsList.add(new Guest(name, content,entryId));
                }
            }
        }

        var bufferedWriter = httpResponse.getWriter();
        bufferedWriter.write(guestsList.toString());

    }

    private DataSource createConnectionPool() {

        String CLOUD_SQL_CONNECTION_NAME =
            System.getenv("<PROJECT_ID>:<REGION_ID>:<INSTANCE_ID>");

        final String DB_USER = System.getenv("DB_USER");
        final String DB_PASS = System.getenv("DB_PASS");
        final String DB_NAME = System.getenv("DB_NAME");

        HikariConfig config = new HikariConfig();

        // Configure which instance and what database user to connect with.
        config.setJdbcUrl(String.format("jdbc:mysql:///%s", DB_NAME));
        config.setUsername(DB_USER); // e.g. "root", "postgres"
        config.setPassword(DB_PASS); // e.g. "my-password"
        config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
        config.addDataSourceProperty("cloudSqlInstance", CLOUD_SQL_CONNECTION_NAME);
        DataSource pool = new HikariDataSource(config);
        return pool;

    }

    @Data
    @AllArgsConstructor
    @ToString
    class Guest {
        String guestName;
        String content;
        int entryId;
    }
}
