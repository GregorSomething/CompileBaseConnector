package me.gregorsomething.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface Database {

    /**
     * Provided result set must be closed, otherwise statement will be not closed
     */
    ResultSet query(String query, Object... values) throws SQLException;

    void execute(String query, Object... values) throws SQLException;

    Connection getConnection() throws SQLException;

    default <T> Optional<T> queryAndMap(String query, ResultSetMapper<T> mapper, Object... values) throws SQLException {
        try (ResultSet rs = this.query(query, values)) {
            if (!rs.isBeforeFirst())
                return Optional.empty();
            rs.next();
            return Optional.of(mapper.fromRow(rs));
        }
    }

    default <T> List<T> queryAndMapAll(String query, ResultSetMapper<T> mapper, Object... values) throws SQLException {
        try (ResultSet rs = this.query(query, values)) {
            if (!rs.isBeforeFirst())
                return List.of();
            List<T> list = new ArrayList<>();
            while (rs.next())
                list.add(mapper.fromRow(rs));
            return List.copyOf(list);
        }
    }

    void close();

    static DatabaseDetails.DatabaseDetailsBuilder detailsBuilder() {
        return DatabaseDetails.builder();
    }
}
