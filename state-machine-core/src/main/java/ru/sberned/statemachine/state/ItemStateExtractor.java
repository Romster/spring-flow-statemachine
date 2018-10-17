package ru.sberned.statemachine.state;

/**
 * This interface allows you not to implement third-party interfaces in your model.
 * All calls for state of your ENTITY will be proceeded through the instance of this interface.
 * Must be stateless.
 * @param <ENTITY>
 * @param <STATE>
 */
public interface ItemStateExtractor<ENTITY, STATE> {
    STATE getItemState(ENTITY item);
}
