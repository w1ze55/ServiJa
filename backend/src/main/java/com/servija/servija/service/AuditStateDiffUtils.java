package com.servija.servija.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
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
                .filter(key -> !sameValue(valueOf(before, key), valueOf(after, key)))
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

    private static boolean sameValue(Object left, Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof BigDecimal leftDecimal && right instanceof BigDecimal rightDecimal) {
            return leftDecimal.compareTo(rightDecimal) == 0;
        }
        if (left instanceof Map<?, ?> leftMap && right instanceof Map<?, ?> rightMap) {
            return sameMap(leftMap, rightMap);
        }
        if (left instanceof Collection<?> leftCollection && right instanceof Collection<?> rightCollection) {
            return sameCollection(leftCollection, rightCollection);
        }
        return Objects.equals(left, right);
    }

    private static boolean sameMap(Map<?, ?> left, Map<?, ?> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : left.entrySet()) {
            Object rightValue = right.get(entry.getKey());
            if (!sameValue(entry.getValue(), rightValue)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameCollection(Collection<?> left, Collection<?> right) {
        if (left.size() != right.size()) {
            return false;
        }
        Iterator<?> leftIterator = left.iterator();
        Iterator<?> rightIterator = right.iterator();
        while (leftIterator.hasNext() && rightIterator.hasNext()) {
            if (!sameValue(leftIterator.next(), rightIterator.next())) {
                return false;
            }
        }
        return true;
    }
}
