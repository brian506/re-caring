package com.recaring.safezone.vo;

import java.util.LinkedHashSet;
import java.util.Set;

public record SafeZoneTransition(Set<String> previousKeys, Set<String> currentKeys) {

    public Set<String> enteredKeys() {
        return difference(currentKeys, previousKeys);
    }

    public Set<String> exitedKeys() {
        return difference(previousKeys, currentKeys);
    }

    private static Set<String> difference(Set<String> source, Set<String> exclude) {
        Set<String> result = new LinkedHashSet<>(source);
        result.removeAll(exclude);
        return result;
    }
}
