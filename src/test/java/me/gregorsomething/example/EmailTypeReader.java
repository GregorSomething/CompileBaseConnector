package me.gregorsomething.example;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EmailTypeReader {

    public static Email from(ResultSet rs, int column) throws SQLException {
        String emailString = rs.getString(column);
        if (emailString == null) return null;
        String[] parts = emailString.split("@");
        if (parts.length != 2) throw new IllegalStateException();
        return new Email(parts[0], parts[1]);
    }
}
