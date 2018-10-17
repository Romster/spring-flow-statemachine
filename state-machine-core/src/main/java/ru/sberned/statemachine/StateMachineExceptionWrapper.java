package ru.sberned.statemachine;

class StateMachineExceptionWrapper extends RuntimeException {


    public StateMachineExceptionWrapper(Exception ex) {
        super("State machine wraps exception", ex, false, false);
    }

    public Exception getWrappedException() {
        return (Exception) getCause();
    }
}
