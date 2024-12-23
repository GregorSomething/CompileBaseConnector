package me.gregorsomething.database;

import lombok.Builder;

import java.sql.SQLException;

@Builder()
public record DatabaseDetails(String dbURL, String user, String password, String dbName, int maxPoolSize) {
    public Database asMariaDb() throws SQLException {
        return new MariaDBDatabase(this);
    }
}
