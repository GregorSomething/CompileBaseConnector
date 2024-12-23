package me.gregorsomething.types;

import me.gregorsomething.database.Database;
import me.gregorsomething.database.DatabaseIntTest;
import me.gregorsomething.database.RepositoryProvider;
import org.junit.jupiter.api.*;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class TypesRepositoryIntTest {

    private static Database database;
    private static TypesRepository repo;

    @BeforeAll
    static void setup() throws SQLException {
        database = DatabaseIntTest.makeTest();
        repo = RepositoryProvider.create(TypesRepository.class, database);
        database.execute("INSERT INTO gs_test_database1 "); // TODO Complete
    }

    @AfterAll
    static void teardown() {
        database.close();
    }

    @Test
    void testInt() {
        repo.setInt(1);
        assertEquals(1, repo.getInt());
        assertEquals(1, repo.getInt2());
        assertEquals(1, repo.getInt3().orElseThrow());
        repo.setInt(null);
        assertEquals(-1, repo.getInt());
        assertNull(repo.getInt2());
        assertTrue(repo.getInt3().isEmpty());
    }

    @Test
    void testLong() {
        repo.setLong(1L);
        assertEquals(1, repo.getLong());
        assertEquals(1, repo.getLong2());
        assertEquals(1, repo.getLong3().orElseThrow());
        repo.setLong(null);
        assertEquals(-1, repo.getLong());
        assertNull(repo.getLong2());
        assertTrue(repo.getLong3().isEmpty());
    }
}
