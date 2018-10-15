package ru.sberned.statemachine.util;

import org.springframework.context.ApplicationEventPublisher;
import ru.sberned.statemachine.state.StateChangedEvent;

public class Events {

    public static void publishAsynchronously(StateChangedEvent event, ApplicationEventPublisher publisher) {
        new Thread(() -> publisher.publishEvent(event)).start();
    }

}
