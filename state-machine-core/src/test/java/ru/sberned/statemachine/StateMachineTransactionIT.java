package ru.sberned.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import ru.sberned.statemachine.StateRepository.StateRepositoryBuilder;
import ru.sberned.statemachine.state.AfterAnyTransition;
import ru.sberned.statemachine.state.StateChangedEvent;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.DBStateProvider;
import ru.sberned.statemachine.util.Events;
import ru.sberned.statemachine.util.Item;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static ru.sberned.statemachine.util.CustomState.*;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 25/04/2017.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = ITConfig.class
)
public class StateMachineTransactionIT {
    @Autowired
    private StateMachine<Item, String, CustomState> stateMachine;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private DBStateProvider stateProvider;

    @Before
    public void before() {
        stateProvider.cleanItems();
        stateProvider.insertItems(Arrays.asList(new Item("1", CustomState.START),
                new Item("2", CustomState.START),
                new Item("3", CustomState.START),
                new Item("4", CustomState.START),
                new Item("5", CustomState.START),
                new Item("6", CustomState.START),
                new Item("7", CustomState.START)));
    }

    @Test
    public void testStateUpdated() throws InterruptedException {
        StateRepository<Item, CustomState, String> repository = StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(STATE1)
                .and()
                .from(STATE1)
                .to(CustomState.FINISH)
                .build();

        stateMachine.setStateRepository(repository);
        List<String> items = Arrays.asList("1", "2");
        List<StateChangedEvent> events = items
                .stream()
                .map(id -> new StateChangedEvent<>(id, STATE1)).collect(Collectors.toList());
        Events.publishAsynchronouslyAndWait(events, publisher);

        verifyState(items, STATE1);

        events = items
                .stream()
                .map(id -> new StateChangedEvent<>(id, FINISH)).collect(Collectors.toList());
        Events.publishAsynchronouslyAndWait(events, publisher);

        verifyState(items, FINISH);
    }

    @Test
    public void testRollback() throws InterruptedException {
        final List<String> failedIds = Arrays.asList("2", "4");
        StateRepository<Item, CustomState, String> repository = StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .setAnyAfter((AfterAnyTransition<Item, CustomState>) (item, stateFrom) -> {
                    if (failedIds.contains(item.getId())) {
                        throw new RuntimeException("just to check");
                    }
                })
                .defineTransitions()
                .from(CustomState.START)
                .to(STATE1)
                .build();

        stateMachine.setStateRepository(repository);
        List<String> items = Arrays.asList("1", "2", "4", "5", "6", "7");
        List<StateChangedEvent<CustomState, String>> events = items
                .stream()
                .map(id -> new StateChangedEvent<>(id, STATE1)).collect(Collectors.toList());
        Events.publishAsynchronouslyAndWait(events, publisher);
        // events are handled in async mode

        verifyState(Arrays.asList("2", "3", "4"), START);
        verifyState(Arrays.asList("1", "5", "6", "7"), STATE1);
    }

    private void verifyState(List<String> ids, CustomState expectedState) {
        for (String id : ids) {
            verifyState(id, expectedState);
        }
    }

    private void verifyState(String id, CustomState expectedState) {
        Item item = stateProvider.getItemById(id);
        assertEquals("Id: " + id, expectedState, item.getState());
    }
}
