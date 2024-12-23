package me.gregorsomething.types;

import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.annotations.Repository;
import me.gregorsomething.database.annotations.Statement;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Repository("CREATE TABLE IF NOT EXISTS gs_test_database2 (aaa INT PRIMARY KEY, bbb TEXT);")
public interface TypesRepository {

    @Statement("")
    void setInt(Integer i);

    @Statement("")
    void setLong(Long l);

    @Statement("")
    void setBoolean(Boolean b);

    @Statement("")
    void setDouble(Double d);

    @Statement("")
    void setFloat(Float f);

    @Statement("")
    void setString(String s);

    @Statement("")
    void setDate(LocalDateTime d);

    @Statement("")
    void setTime(LocalTime t);

    @Statement("")
    void setInstant(Instant i);

    @Statement("")
    void setDate2(LocalDate d);

    @Query(value = "SELECT iint FROM gs_test_database2 LIMIT 1;", defaultValue = "-1")
    int getInt();

    @Query("SELECT iint FROM gs_test_database2 LIMIT 1;")
    Integer getInt2();

    @Query("SELECT iint FROM gs_test_database2 LIMIT 1;")
    Optional<Integer> getInt3();

    @Query(value = "SELECT ilong FROM gs_test_database2 LIMIT 1;", defaultValue = "-1L")
    long getLong();

    @Query("SELECT ilong FROM gs_test_database2 LIMIT 1;")
    Long getLong2();

    @Query("SELECT ilong FROM gs_test_database2 LIMIT 1;")
    Optional<Long> getLong3();

    @Query(value = "SELECT ibool FROM gs_test_database2 LIMIT 1;", defaultValue = "true")
    boolean getBoolean();

    @Query("SELECT ibool FROM gs_test_database2 LIMIT 1;")
    Boolean getBoolean2();

    @Query("SELECT ibool FROM gs_test_database2 LIMIT 1;")
    Optional<Boolean> getBoolean3();

    @Query(value = "SELECT idouble FROM gs_test_database2 LIMIT 1;", defaultValue = "0.0")
    double getDouble();

    @Query(value = "SELECT ifloat FROM gs_test_database2 LIMIT 1;", defaultValue = "0.0f")
    float getFloat();

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
