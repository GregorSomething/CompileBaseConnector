package me.gregorsomething.basic;

import me.gregorsomething.database.Database;
import me.gregorsomething.database.DatabaseIntTest;
import me.gregorsomething.database.RepositoryProvider;
import me.gregorsomething.database.Transaction;
import org.junit.jupiter.api.*;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BasicRepoIntTest {

    private static Database database;
    private static SampleRepository repo;

    @BeforeAll
    static void setup() throws SQLException {
        database = DatabaseIntTest.makeTest();
    }

    @AfterAll
    static void teardown() {
        database.close();
    }

    @Test
    @Order(1)
    void testRepoRepository() {
        repo = RepositoryProvider.create(SampleRepository.class, database);
        assertNotNull(repo);
        repo.deleteAll();
    }

    @Test
    @Order(2)
    void testWrite() {
        repo.add(1, "a");
        repo.add(2, "b");
        repo.add(3, "c");
        repo.add(4, "d");
        assertTrue(true);
    }

    @Test
    @Order(3)
    void testRead() {
        assertEquals("a", repo.get(1));
        assertEquals("d", repo.get(4));
    }

    @Test
    @Order(4)
    void testTransactionSimple() {
        assertTrue(repo.inTransaction(r ->
                r.add(5, "e")));
        assertFalse(repo.inTransaction(r ->
                r.add(5, "a")));
    }

    @Test
    @Order(5)
    void testTransactionAdvanced() throws SQLException {
        try (Transaction transaction = repo.getNewTransaction()) {
            repo.inTransaction(transaction, r ->
                    r.add(6, "f"));
            assertNull(repo.get(6));
            transaction.commit();
            assertEquals("f", repo.get(6));
        }
    }

    @Test
    @Order(6)
    void testTransactionAdvanced1() throws SQLException {
        try (Transaction transaction = repo.getNewTransaction()) {
            repo.inTransaction(transaction, r ->
                    r.add(7, "g"));
            assertNull(repo.get(7));
            transaction.rollback();
            assertNull(repo.get(7));
        }
    }
}
