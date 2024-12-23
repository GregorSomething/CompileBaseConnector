package me.gregorsomething.database;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.sql.*;

public class TransactionalDatabase implements Database {
    private final Connection connection;

    public TransactionalDatabase(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.connection.setAutoCommit(false);
    }

    @Override
    public ResultSet query(String query, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            try {
                for (int i = 1; i <= values.length; i++) {
                    statement.setObject(i, values[i - 1]);
                }
                return statement.executeQuery();

            } finally {
                statement.closeOnCompletion();
            }
        }
    }

    @Override
    public void execute(String query, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 1; i <= values.length; i++) {
                statement.setObject(i, values[i - 1]);
            }
            statement.execute();
        }
    }

    @Override
    public Connection getConnection() {
        return this.connection;
    }

    @Override
    @SneakyThrows
    public void close() {
        if (!this.connection.isClosed())
            this.connection.close();
    }
}
