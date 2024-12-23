package me.gregorsomething.database;

import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MariaDBDatabase implements Database {
    private final MariaDbPoolDataSource dataSource;


    public MariaDBDatabase(DatabaseDetails details) throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        this.dataSource = new MariaDbPoolDataSource();
        this.dataSource.setUrl("jdbc:mariadb://" + details.dbURL() +
                "/" + details.dbName() + "?user=" + details.user() + "&password=" + details.password());
        this.dataSource.setMaxPoolSize(details.maxPoolSize());
    }

    /**
     * Queries data form database
     * @param query statement that is used
     * @param values Options/arguments in that statement
     * @return Result of query, close after use
     */
    public ResultSet query(String query, Object... values) throws SQLException {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

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

    /**
     * Executes statement in database
     * @param query statement that is used
     * @param values Options/arguments in that statement
     */
    public void execute(String query, Object... values) throws SQLException {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 1; i <= values.length; i++) {
                statement.setObject(i, values[i - 1]);
            }
            statement.execute();
        }
    }

    /**
     * Gives new connection to Database
     * @return connection to DB
     */
    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    /**
     * Closes connection to database
     */
    public void close() {
        this.dataSource.close();
    }
}
