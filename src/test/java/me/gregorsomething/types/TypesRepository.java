package me.gregorsomething.types;

import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.annotations.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Repository("CREATE TABLE IF NOT EXISTS gs_test_database2 (aaa INT PRIMARY KEY, bbb TEXT);")
public interface TypesRepository {

    @Query(value = "SELECT [( val )];", defaultValue = "-1")
    int getInt(Integer val);

    @Query("SELECT [( val )];")
    Integer getInt2(Integer val);

    @Query("SELECT [( val )];")
    Optional<Integer> getInt3(Integer val);

    @Query(value = "SELECT [( val )];", defaultValue = "-1L")
    long getLong(Long val);

    @Query("SELECT [( val )];")
    Long getLong2(Long val);

    @Query("SELECT [( val )];")
    Optional<Long> getLong3(Long val);

    @Query("SELECT istring FROM gs_test_database2 LIMIT 1;")
    String getString();

    @Query("SELECT idate FROM gs_test_database2 LIMIT 1;")
    LocalDate getLocalDate();

    @Query("SELECT idatetime FROM gs_test_database2 LIMIT 1;")
    LocalDateTime getLocalDateTime();

    @Query("SELECT itime FROM gs_test_database2 LIMIT 1;")
    LocalTime getTime();

    @Query("SELECT iinstant FROM gs_test_database2 LIMIT 1;")
    Instant getInstant();
}
