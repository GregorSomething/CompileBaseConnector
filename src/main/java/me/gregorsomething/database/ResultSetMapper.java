package me.gregorsomething.database;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetMapper<T> {

    @NotNull T fromRow(@NotNull ResultSet rs) throws SQLException;
}
