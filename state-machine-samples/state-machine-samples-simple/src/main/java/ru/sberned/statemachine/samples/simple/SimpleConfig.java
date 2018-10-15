package ru.sberned.statemachine.samples.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import ru.sberned.statemachine.StateMachine;
import ru.sberned.statemachine.StateRepository;
import ru.sberned.statemachine.StateRepository.StateRepositoryBuilder;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.samples.simple.store.ItemStore;
import ru.sberned.statemachine.state.*;

import java.util.EnumSet;

import static ru.sberned.statemachine.samples.simple.SimpleState.*;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 25/04/2017.
 */
@SuppressWarnings("unchecked")
@Configuration
public class SimpleConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleConfig.class);

    @Autowired
    private LockProvider lockProvider;

    @Bean
    public StateMachine<SimpleItem, SimpleState, String> stateMachine(PlatformTransactionManager txManager) {
        StateRepository<SimpleItem, SimpleState, String> repository = StateRepositoryBuilder.<SimpleItem, SimpleState, String>configure()
                .setAvailableStates(EnumSet.allOf(SimpleState.class))
                .setUnhandledMessageProcessor((item, state, type, ex) -> LOGGER.error("Got unhandled item with id {}, issue is {}", item, type))
                .setAnyBefore((BeforeAnyTransition<SimpleItem, SimpleState>) (item, state) -> {
                    LOGGER.info("Started working on item with id {}", item.getId());
                    return true;
                })
                .defineTransitions()
                .from(STARTED)
                .to(IN_PROGRESS)
                .after((AfterTransition<SimpleItem>) item -> LOGGER.info("Moved from STARTED to IN_PROGRESS"))
                .and()
                .from(IN_PROGRESS)
                .to(FINISHED)
                .after((AfterTransition<SimpleItem>) item -> LOGGER.info("Moved from IN_PROGRESS to FINISHED"))
                .and()
                .from(STARTED, IN_PROGRESS)
                .to(CANCELLED)
                .after((AfterTransition<SimpleItem>) item -> LOGGER.info("Moved from CANCELED"))
                .build();

        StateMachine<SimpleItem, SimpleState, String> stateMachine = new StateMachine<>(stateProvider(), stateChanger(), lockProvider, txManager);
        stateMachine.setStateRepository(repository);
        return stateMachine;
    }

    @Bean
    public ItemWithStateProvider<SimpleItem, String> stateProvider() {
        return new ListStateProvider();
    }

    @Bean
    public PlatformTransactionManager dummyPlatformTransactionManager() {
        return new DummyTransactionManager();
    }

    @Bean
    public StateChanger<SimpleItem, SimpleState> stateChanger() {
        return new StateHandler();
    }

    public static class ListStateProvider implements ItemWithStateProvider<SimpleItem, String> {
        @Autowired
        private ItemStore store;

        @Override
        public SimpleItem getItemById(String id) {
            return store.getItem(id);
        }
    }

    public static class StateHandler implements StateChanger<SimpleItem, SimpleState> {
        @Autowired
        private ItemStore store;

        @Override
        public void moveToState(SimpleState state, SimpleItem item, StateChangedInfo info) {
            SimpleItem itemFound = store.getItem(item);
            if (itemFound != null) {
                itemFound.setState(state);
            } else {
                throw new RuntimeException("Item not found");
            }
        }
    }
}
