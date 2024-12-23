package me.gregorsomething.database;

import org.junit.jupiter.api.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseIntTest {

    private static Database database;

    @BeforeAll
    public static void setup() throws SQLException {
        database = makeTest();
    }

    public static Database makeTest() throws SQLException {
        return Database.detailsBuilder()
                .dbURL("127.0.0.1")
                .user("user1")
                .password("password")
                .maxPoolSize(3)
                .dbName("test")
                .build().asMariaDb();
    }

    @AfterAll
    public static void teardown() throws SQLException {
        database.execute("DELETE FROM gs_test_database;");
        database.close();
    }

    @Test
    @Order(1)
    void testTableCreate() {
        assertDoesNotThrow(() -> database.execute("CREATE TABLE IF NOT EXISTS gs_test_database (aaa INT PRIMARY KEY, bbb TEXT);"));
    }

    @Test
    @Order(2)
    void testInsertAndRead() throws SQLException {
        assertDoesNotThrow(() ->
                database.execute("INSERT INTO gs_test_database VALUES (1, 'aa'), (2, 'bb'), (?, ?);", 3, "cc"));
        try (ResultSet rs = database.query("SELECT * FROM gs_test_database;")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
            assertEquals("aa", rs.getString(2));
            rs.next();
            assertEquals(2, rs.getInt(1));
            assertEquals("bb", rs.getString(2));
            rs.next();
            assertEquals(3, rs.getInt(1));
            assertEquals("cc", rs.getString(2));
        }
    }

    @Test
    @Order(3)
    void testReadWithMap() throws SQLException {
        Optional<Integer> present = database.queryAndMap("SELECT aaa FROM gs_test_database WHERE bbb = ? LIMIT 1;",
                rs -> rs.getInt(1), "bb");
        assertEquals(2, present.orElseThrow());
        Optional<Integer> notPresent = database.queryAndMap("SELECT aaa FROM gs_test_database WHERE bbb = ? LIMIT 1;",
                rs -> rs.getInt(1), "fff");
        assertFalse(notPresent.isPresent());
    }

    @Test
    @Order(4)
    void testReadWithMapAll() throws SQLException {
        List<Integer> list = database.queryAndMapAll("SELECT aaa FROM gs_test_database WHERE bbb != ?;",
                rs -> rs.getInt(1), "bb");
        assertEquals(2, list.size());
        assertTrue(list.contains(1));
        assertTrue(list.contains(3));
        List<Integer> list1 = database.queryAndMapAll("SELECT aaa FROM gs_test_database WHERE bbb = ?;",
                rs -> rs.getInt(1), "fff");
        assertEquals(0, list1.size());
    }
}
