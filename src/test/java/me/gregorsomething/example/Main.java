package me.gregorsomething.example;

import me.gregorsomething.database.*;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        // Make database
        DatabaseDetails details = DatabaseDetails.builder()
                .dbURL("127.0.0.1")
                .user("postgres")
                .password("postgres")
                .dbName("test")
                .build();
        Database database = PostgresDatabaseProvider.of(details);
        // Init repository
        UserRepository userRepository = RepositoryProvider.create(UserRepository.class, database);

        // Read
        User user = userRepository.byId(1);
        // Use in transaction
        userRepository.inTransaction(repo -> {
            repo.addUser("Test1", new Email("test", "example.com"));
            repo.addUser("Test2", new Email("test2", "example.com"));
        });

        // Other type of transactions
        Transaction transaction = userRepository.getNewTransaction();
        try (transaction) {
            UserRepository repoInTransaction = userRepository.asTransactional(transaction);

            repoInTransaction.addUser("Test1", new Email("test", "example.com"));
            repoInTransaction.addUser("Test2", new Email("test2", "example.com"));

            transaction.commit();
        } catch (SQLException e) {
            // No rollback needed, close handels that
            throw new RuntimeException(e);
        }

        userRepository.updateUser(user);
    }
}
