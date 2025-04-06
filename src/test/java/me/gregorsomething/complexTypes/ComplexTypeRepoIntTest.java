package me.gregorsomething.complexTypes;


import me.gregorsomething.database.Database;
import me.gregorsomething.database.DatabaseIntTest;
import me.gregorsomething.database.RepositoryProvider;
import me.gregorsomething.database.processor.helpers.Pair;
import org.junit.jupiter.api.*;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This testing assumes Database, Simple query and parameters work correctly
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComplexTypeRepoIntTest {
    private static Database database;
    private static ComplexTypeRepo repo;

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
    void testRetrieval() {
        repo = RepositoryProvider.create(ComplexTypeRepo.class, database);
        assertNotNull(repo);
    }

    @Test
    @Order(2)
    void testGenericMapping() {
        // Theoretically this test is not needed as compiler can type check generated code
        assertDoesNotThrow(() -> {
            Pair<Integer, Long> pair = repo.getPair(1, 1L);
            long l = pair.right();
            int r = pair.left();
        });

        Pair<Integer, Long> p1 = repo.getPair(2, 3L);
        assertEquals(2, p1.left());
        assertEquals(3L, p1.right());

        Pair<Integer, Long> p2 = repo.getPair(3, 6L);
        assertEquals(3, p2.left());
        assertEquals(6L, p2.right());

        Pair<Integer, Long> p3 = repo.getPair(7, null);
        assertEquals(7, p3.left());
        assertNull(p3.right());
    }


}
