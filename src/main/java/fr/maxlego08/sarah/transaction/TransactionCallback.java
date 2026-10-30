package fr.maxlego08.sarah.transaction;

import fr.maxlego08.sarah.RequestHelper;

/**
 * Operations run by {@link RequestHelper#inTransaction(TransactionCallback)}.
 *
 * @param <T> what the operations return
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    /**
     * @param helper a helper whose every call runs inside the transaction
     * @return any result, handed back by {@code inTransaction}
     * @throws Exception to roll the transaction back
     */
    T execute(RequestHelper helper) throws Exception;
}
