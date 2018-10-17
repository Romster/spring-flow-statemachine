package ru.sberned.statemachine.util;

import org.springframework.context.ApplicationEventPublisher;
import ru.sberned.statemachine.state.StateChangedEvent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Events {


    public static void publishAsynchronously(StateChangedEvent event, ApplicationEventPublisher publisher) {
        new Thread(() -> publisher.publishEvent(event)).start();
    }

    public static void publishAsynchronouslyAndWait(Collection<? extends StateChangedEvent> events, ApplicationEventPublisher publisher) {
        ExecutorService taskExecutor = Executors.newFixedThreadPool(4);
        events.forEach(ev->taskExecutor.execute(() -> publisher.publishEvent(ev)));
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
