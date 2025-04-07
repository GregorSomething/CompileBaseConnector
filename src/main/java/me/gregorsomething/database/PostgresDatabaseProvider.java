package me.gregorsomething.database;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.postgresql.ds.PGSimpleDataSource;

import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PostgresDatabaseProvider {

    public static Database of(DatabaseDetails details) {
        return of(details, dataSource -> {});
    }

    public static Database of(DatabaseDetails details, Consumer<PGSimpleDataSource> extraConfig) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://" + details.dbURL() +
                "/" + details.dbName() + "?user=" + details.user() + "&password=" + details.password());
        extraConfig.accept(dataSource);
        return new DataSourceDatabase(dataSource);
    }

    public static Database of(Consumer<PGSimpleDataSource> dataSourceConfigurer) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSourceConfigurer.accept(dataSource);
        return new DataSourceDatabase(dataSource);
    }
}
