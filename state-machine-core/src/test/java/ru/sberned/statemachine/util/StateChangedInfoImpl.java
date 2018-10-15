package ru.sberned.statemachine.util;

import ru.sberned.statemachine.state.StateChangedInfo;

import java.util.Objects;

public class StateChangedInfoImpl implements StateChangedInfo {

    public static StateChangedInfoImpl info(String value) {
        return new StateChangedInfoImpl(value);
    }

    private final String info;

    public StateChangedInfoImpl(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateChangedInfoImpl that = (StateChangedInfoImpl) o;
        return Objects.equals(info, that.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(info);
    }

    @Override
    public String toString() {
        return "StateChangedInfoImpl{" +
                "info='" + info + '\'' +
                '}';
    }
}



