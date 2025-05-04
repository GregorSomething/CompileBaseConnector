package me.gregorsomething.database;

import java.sql.SQLException;

/**
 * Adds transactional support to repository
 * @param <T> repository type
 */
public interface Transactional<T> {

    default boolean inTransaction(TransactionBlock<T> action) {
        try (Transaction transaction = this.getNewTransaction()) {
            try {
                this.inTransaction(transaction, action);
                transaction.commit();
                return true; // success
            } catch (SQLException ignored) {
                transaction.rollback();
            }
        } catch (SQLException ignored) {}
        return false; // fail
    }

    T asTransactional(Transaction transaction);

    default void inTransaction(Transaction transaction, TransactionBlock<T> action) throws SQLException {
        action.runInTransaction(this.asTransactional(transaction));
    }

    Transaction getNewTransaction();

    interface TransactionBlock<T> {
        void runInTransaction(T transaction) throws SQLException;
    }
}
