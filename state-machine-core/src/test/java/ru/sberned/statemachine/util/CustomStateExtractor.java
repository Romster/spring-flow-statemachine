package ru.sberned.statemachine.util;

import ru.sberned.statemachine.state.ItemStateExtractor;

public class CustomStateExtractor implements ItemStateExtractor<Item, CustomState> {
    @Override
    public CustomState getItemState(Item item) {
        return item.getState();
    }
}
