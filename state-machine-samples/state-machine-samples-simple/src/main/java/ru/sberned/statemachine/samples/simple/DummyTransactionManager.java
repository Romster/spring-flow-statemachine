package ru.sberned.statemachine.samples.simple;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

public class DummyTransactionManager implements PlatformTransactionManager {
    @Override
    public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
       return  new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus transactionStatus) throws TransactionException {
        System.out.println("DummyTransactionManager.commit");
    }

    @Override
    public void rollback(TransactionStatus transactionStatus) throws TransactionException {
        System.out.println("DummyTransactionManager.rollback");
    }
}
