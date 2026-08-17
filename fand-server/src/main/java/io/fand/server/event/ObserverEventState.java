package io.fand.server.event;

import io.fand.api.event.Event;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Captures the mutable top-level state of an event for observer enforcement. */
final class ObserverEventState {

    private final Event event;
    private final List<FieldState> fields;

    private ObserverEventState(Event event, List<FieldState> fields) {
        this.event = event;
        this.fields = fields;
    }

    static ObserverEventState capture(Event event) {
        var fields = new ArrayList<FieldState>();
        for (Class<?> type = event.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(event);
                    if (!Modifier.isFinal(field.getModifiers()) || value instanceof Collection<?> || value instanceof Map<?, ?>) {
                        fields.add(FieldState.capture(field, value));
                    }
                } catch (RuntimeException | IllegalAccessException failure) {
                    throw new IllegalStateException("Failed to capture observer state for " + event.getClass().getName(), failure);
                }
            }
        }
        return new ObserverEventState(event, List.copyOf(fields));
    }

    @Nullable IllegalStateException restoreAndCreateFailure() {
        var changedFields = new ArrayList<String>();
        for (var field : fields) {
            if (field.restore(event)) {
                changedFields.add(field.field().getName());
            }
        }
        if (changedFields.isEmpty()) {
            return null;
        }
        return new IllegalStateException(
                "OBSERVER listener mutated " + event.getClass().getName() + " fields " + changedFields);
    }

    private record FieldState(Field field, Object originalValue, Object contents) {

        static FieldState capture(Field field, Object value) {
            Object contents = null;
            if (value instanceof List<?> list) {
                contents = new ArrayList<>(list);
            } else if (value instanceof Collection<?> collection) {
                contents = new ArrayList<>(collection);
            } else if (value instanceof Map<?, ?> map) {
                contents = new LinkedHashMap<>(map);
            }
            return new FieldState(field, value, contents);
        }

        boolean restore(Event event) {
            try {
                Object currentValue = field.get(event);
                boolean changed = currentValue != originalValue || contentsChanged(currentValue);
                if (!changed) {
                    return false;
                }
                if (!Modifier.isFinal(field.getModifiers())) {
                    field.set(event, originalValue);
                }
                restoreContents();
                return true;
            } catch (RuntimeException | IllegalAccessException failure) {
                throw new IllegalStateException("Failed to restore observer mutation of " + field, failure);
            }
        }

        private boolean contentsChanged(Object currentValue) {
            if (contents instanceof List<?> snapshot && currentValue instanceof Collection<?> collection) {
                return !snapshot.equals(new ArrayList<>(collection));
            }
            if (contents instanceof Map<?, ?> snapshot && currentValue instanceof Map<?, ?> map) {
                return !snapshot.equals(map);
            }
            return contents == null && !Objects.equals(currentValue, originalValue);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void restoreContents() {
            if (contents instanceof List<?> snapshot && originalValue instanceof Collection collection) {
                if (snapshot.equals(new ArrayList<>(collection))) {
                    return;
                }
                collection.clear();
                collection.addAll(snapshot);
            } else if (contents instanceof Map<?, ?> snapshot && originalValue instanceof Map map) {
                if (snapshot.equals(map)) {
                    return;
                }
                map.clear();
                map.putAll(snapshot);
            }
        }
    }
}
