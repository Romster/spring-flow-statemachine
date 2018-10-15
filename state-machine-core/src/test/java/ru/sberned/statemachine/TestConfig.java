package ru.sberned.statemachine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.lock.MapLockProvider;
import ru.sberned.statemachine.state.ItemWithStateProvider;
import ru.sberned.statemachine.state.StateChangedInfo;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.CustomStateProvider;
import ru.sberned.statemachine.util.DummyTransactionManager;
import ru.sberned.statemachine.util.Item;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 10/11/2016.
 */
@Configuration
public class TestConfig {
    @Bean
    public ItemWithStateProvider<Item, String> stateProvider() {
        return new CustomStateProvider();
    }

    @Bean
    public LockProvider stateLock() {
        return new MapLockProvider();
    }

    @Bean
    public PlatformTransactionManager txManager() {
        return new DummyTransactionManager();
    }

    @Bean
    public StateMachine<Item, CustomState, String> stateMachine(PlatformTransactionManager txManager) {
        return new StateMachine<>(stateProvider(), stateChanger(), stateLock(), txManager);
    }

    @Bean
    public StateChanger<Item, CustomState> stateChanger() {
        return new TestOnTransition();
    }

    private class TestOnTransition implements StateChanger<Item, CustomState> {

        @Override
        public void moveToState(CustomState state, Item item, StateChangedInfo info) {
            item.state = state;
        }
    }
}
