package me.gregorsomething.database;

import lombok.SneakyThrows;

import java.sql.SQLException;

public interface Transactional<T> {

    @SneakyThrows
    default boolean inTransaction(TransactionBlock<T> action) {
        try (Transaction transaction = this.getNewTransaction()) {
            try {
                this.inTransaction(transaction, action);
                transaction.commit();
                return true; // Sucsess
            } catch (SQLException ignored) {
                transaction.rollback();
            }
        }
        return false; // fail
    }

    T asTransactional(Transaction transaction);

    default void inTransaction(Transaction transaction, TransactionBlock<T> action) throws SQLException {
        action.runInTransaction(this.asTransactional(transaction));
    }

    Transaction getNewTransaction();

    public interface TransactionBlock<T> {
        void runInTransaction(T transaction) throws SQLException;
    }
}
