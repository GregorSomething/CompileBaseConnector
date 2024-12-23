package me.gregorsomething.basic;

import me.gregorsomething.database.Transactional;
import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.annotations.Repository;
import me.gregorsomething.database.annotations.Statement;

@Repository("CREATE TABLE IF NOT EXISTS gs_test_database1 (aaa INT PRIMARY KEY, bbb TEXT);")
public interface SampleRepository extends Transactional<SampleRepository> {

    @Statement("DELETE FROM gs_test_database1;")
    void deleteAll();

    @Statement("INSERT INTO gs_test_database1 VALUES (?, ?);")
    void add(int id, String text);

    @Query("SELECT bbb FROM gs_test_database1 WHERE aaa = ? LIMIT 1;")
    String get(int id);
}
