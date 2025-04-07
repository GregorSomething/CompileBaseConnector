package me.gregorsomething.types;

import me.gregorsomething.database.Database;
import me.gregorsomething.database.DatabaseIntTest;
import me.gregorsomething.database.RepositoryProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TypesRepositoryIntTest {

    private static Database database;
    private static TypesRepository repo;

    @BeforeAll
    static void setup() throws SQLException {
        database = DatabaseIntTest.makeTest();
        repo = RepositoryProvider.create(TypesRepository.class, database);
    }

    @AfterAll
    static void teardown() {
        database.close();
    }

    @Test
    void testInt() {
        assertEquals(1, repo.getInt(1));
        assertEquals(1, repo.getInt2(1));
        assertEquals(1, repo.getInt3(1).orElseThrow());
        assertEquals(-1, repo.getInt(null));
        assertNull(repo.getInt2(null));
        assertTrue(repo.getInt3(null).isEmpty());
    }

    @Test
    void testLong() {
        assertEquals(1, repo.getLong(1L));
        assertEquals(1, repo.getLong2(1L));
        assertEquals(1, repo.getLong3(1L).orElseThrow());
        assertEquals(-1, repo.getLong(null));
        assertNull(repo.getLong2(null));
        assertTrue(repo.getLong3(null).isEmpty());
    }

    @Test
    void testString() {
        assertNull(repo.getString(null));
        assertEquals("test", repo.getString("test"));
    }

    @Test
    void testDate() {
        assertNull(repo.getLocalDate(null));
        assertEquals(LocalDate.of(2025, 3, 1), repo.getLocalDate(LocalDate.of(2025, 3, 1)));
    }
}
