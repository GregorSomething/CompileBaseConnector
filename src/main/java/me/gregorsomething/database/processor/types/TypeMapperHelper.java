package me.gregorsomething.database.processor.types;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TypeMapperHelper {

    /**
     * Returns value if it is not null, or def value if it was, null,
     * checks for result set nullability, as it might lose it in casting.
     * @param rs result set where te result was read from
     * @param value present value
     * @param defValue default value
     * @return not null value, or default value
     * @param <T> type of value
     * @throws SQLException error
     */
    public static <T> T valueOrDefault(ResultSet rs, T value, T defValue) throws SQLException {
        if (rs.wasNull() || value == null)
            return defValue;
        return value;
    }
}
