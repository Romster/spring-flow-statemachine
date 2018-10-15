package ru.sberned.statemachine.state;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 01/11/2016.
 */
public class StateChangedEvent<E, ID> implements Serializable {
    private ID id;
    private E newState;

    private StateChangedInfo info;


    public StateChangedEvent(ID id, E newState) {
        this.id = id;
        this.newState = newState;
    }

    public StateChangedEvent(ID id, E newState, StateChangedInfo info) {
        this.id = id;
        this.newState = newState;
        this.info = info;
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    public E getNewState() {
        return newState;
    }

    public void setNewState(E newState) {
        this.newState = newState;
    }

    public StateChangedInfo getInfo() {
        return info;
    }

    private void setInfo(StateChangedInfo info) {
        this.info = info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StateChangedEvent<?, ?> that = (StateChangedEvent<?, ?>) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (newState != null ? !newState.equals(that.newState) : that.newState != null) return false;
        return info != null ? info.equals(that.info) : that.info == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (newState != null ? newState.hashCode() : 0);
        return result;
    }

}
