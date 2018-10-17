package ru.sberned.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import ru.sberned.statemachine.exception.StateMachineException;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.processor.UnableToProcessException;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType;
import ru.sberned.statemachine.state.*;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType.*;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 09/11/2016.
 * Modified by Nikolai Romanov 12/10/2018
 */
public class StateMachine<ENTITY, ID, STATE> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    private final ItemWithStateProvider<ENTITY, ID> stateProvider;
    private final ItemStateExtractor<ENTITY, STATE> idAndStateExtractor;
    private final StateChanger<ENTITY, STATE> stateChanger;
    private final LockProvider lockProvider;
    private final PlatformTransactionManager transactionManager;

    volatile private StateRepository<ENTITY, STATE, ID> stateRepository;
    @Value("${statemachine.lock.timeout.ms:5000}")
    private long lockTimeout;
    @Value("${statemachine.transaction.always-new:false}")
    private boolean isOpenNewTransaction;


    @Autowired
    public StateMachine(ItemWithStateProvider<ENTITY, ID> stateProvider,
                        ItemStateExtractor<ENTITY, STATE> idAndStateExtractor,
                        StateChanger<ENTITY, STATE> stateChanger,
                        LockProvider lockProvider,
                        PlatformTransactionManager transactionManager) {
        this.stateProvider = stateProvider;
        this.idAndStateExtractor = idAndStateExtractor;
        this.stateChanger = stateChanger;
        this.lockProvider = lockProvider;
        this.transactionManager = transactionManager;
    }

    public void setStateRepository(StateRepository<ENTITY, STATE, ID> stateRepository) {
        this.stateRepository = stateRepository;
    }

    @EventListener
    public void handleStateChanged(StateChangedEvent<STATE, ID> event) {
        Assert.notNull(stateRepository, "StateRepository must be initialized!");

        changeState(event.getId(), event.getNewState(), event.getInfo());
    }

    public boolean changeState(ID id, STATE newState) {
        return changeState(id, newState, null);
    }

    public boolean changeState(ID id, STATE newState, StateChangedInfo info) {
        if(id == null) throw new NullPointerException("id can't be null");
        return handleMessage(id, newState, info);
    }

    public boolean handleMessage(ID id, STATE newState, StateChangedInfo info) {
        try {
            return buildTxTemplate(id)
                    .execute(status -> doHandleMessage(id, newState, info));
        } catch (StateMachineExceptionWrapper exWrapper) {
            Exception ex = exWrapper.getWrappedException();

            if(ex instanceof StateMachineException) {
                handleIncorrectCase(id, newState, ((StateMachineException)ex).getIssueType(), null);
            } else if(ex instanceof InterruptedException) {
                handleIncorrectCase(id, newState, INTERRUPTED_EXCEPTION, ex);
            } else {
                handleIncorrectCase(id, newState, EXECUTION_EXCEPTION, ex);
            }
        }
        return false;
    }

    private boolean doHandleMessage(ID id, STATE newState, StateChangedInfo info) {
        Lock lockObject = lockProvider.getLockObject(id);
        boolean locked = false;
        try {
            if (locked = lockObject.tryLock(lockTimeout, TimeUnit.MILLISECONDS)) {
                ENTITY entity = stateProvider.getItemById(id);
                if (entity == null) throw new StateMachineException(ENTITY_NOT_FOUND);

                STATE currentState = idAndStateExtractor.getItemState(entity);
                if (stateRepository.isValidTransition(currentState, newState)) {
                    processItem(entity, currentState, newState, info);
                    return true;
                } else {
                    throw new StateMachineException(INVALID_TRANSITION);
                }
            } else {
                throw new StateMachineException(TIMEOUT);
            }
        } catch (Exception e) {
            throw new StateMachineExceptionWrapper(e);
        } finally {
            if(locked) {
                try {
                    lockObject.unlock();
                } catch (RuntimeException ex) {
                    LOGGER.error("Exception during unlocking", ex);
                }
            }
        }
    }

    private void handleIncorrectCase(ID id, STATE newState, IssueType issueType, Exception e) {
        String errorMsg = MessageFormat.format("Processing for item with id {0} failed. New state is {1}. Issue type is {2}", id, newState, issueType);

        if (e != null) LOGGER.error(errorMsg, e);
        else LOGGER.error(errorMsg);

        UnhandledMessageProcessor<ID, STATE> unhandledMessageProcessor = stateRepository.getUnhandledMessageProcessor();
        if (unhandledMessageProcessor != null) {
            unhandledMessageProcessor.process(id, newState, issueType, e);
        }
    }

    private void processItem(ENTITY item, STATE from, STATE to, StateChangedInfo info) {
        stateRepository.getBeforeAll().forEach(handler -> {
            if (!handler.beforeTransition(item, to)) {
                throw new UnableToProcessException();
            }
        });
        stateRepository.getBefore(from, to).forEach(handler -> {
            if (!handler.beforeTransition(item)) {
                throw new UnableToProcessException();
            }
        });

        if (info != null) {
            stateChanger.moveToState(to, item, info);
        } else {
            stateChanger.moveToState(to, item, null);
        }

        stateRepository.getAfter(from, to).forEach(handler -> handler.afterTransition(item));
        stateRepository.getAfterAll().forEach(handler -> handler.afterTransition(item, to));
    }

    private TransactionTemplate buildTxTemplate(ID id) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setName("StateMachine-Transaction-id");
        transactionTemplate.setPropagationBehavior(this.isOpenNewTransaction
                ? Propagation.REQUIRES_NEW.value()
                : Propagation.REQUIRED.value());

        return transactionTemplate;
    }
}
