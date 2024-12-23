package me.gregorsomething.database;

import java.sql.Connection;
import java.sql.SQLException;

public class Transaction implements AutoCloseable {
    private final TransactionalDatabase database;
    private final Connection connection;

    public Transaction(Connection connection) throws SQLException {
        this.connection = connection;
        this.database = new TransactionalDatabase(connection);
    }

    public void commit() throws SQLException {
        this.connection.commit();
    }

    public void rollback() throws SQLException {
        this.connection.rollback();
    }

    @Override
    public void close() {
        this.database.close();
    }

    public Database getTransactionalDatabase() {
        return this.database;
    }
}
