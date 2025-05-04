package me.gregorsomething.database;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mariadb.jdbc.MariaDbDataSource;

import java.sql.SQLException;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MariaDBDatabaseProvider {

    public static Database of(DatabaseDetails details) throws SQLException {
        return of(details, dataSource -> {});
    }

    public static Database of(DatabaseDetails details, Consumer<MariaDbDataSource> extraConfig) throws SQLException {
        MariaDbDataSource dataSource = new MariaDbDataSource();
        dataSource.setUrl("jdbc:mariadb://" + details.dbURL() +
                "/" + details.dbName() + "?user=" + details.user() + "&password=" + details.password());
        extraConfig.accept(dataSource);
        return new DataSourceDatabase(dataSource);
    }

    public static Database of(Consumer<MariaDbDataSource> dataSourceConfigurer) {
        MariaDbDataSource dataSource = new MariaDbDataSource();
        dataSourceConfigurer.accept(dataSource);
        return new DataSourceDatabase(dataSource);
    }
}
