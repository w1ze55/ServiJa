package com.servija.servija.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AuditStateDiffUtils {

    private AuditStateDiffUtils() {
    }

    public static List<String> changedFields(Map<String, Object> before, Map<String, Object> after) {
        LinkedHashSet<String> orderedKeys = new LinkedHashSet<>();
        if (before != null) {
            orderedKeys.addAll(before.keySet());
        }
        if (after != null) {
            orderedKeys.addAll(after.keySet());
        }

        return orderedKeys.stream()
                .filter(key -> !Objects.equals(valueOf(before, key), valueOf(after, key)))
                .toList();
    }

    public static Map<String, Object> filterFields(Map<String, Object> state, Collection<String> fields) {
        Map<String, Object> filtered = new LinkedHashMap<>();
        if (state == null || fields == null) {
            return filtered;
        }
        for (String field : fields) {
            if (state.containsKey(field)) {
                filtered.put(field, state.get(field));
            }
        }
        return filtered;
    }

    private static Object valueOf(Map<String, Object> state, String key) {
        return state == null ? null : state.get(key);
    }
}
