package io.fand.server.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectionFields {

    private ReflectionFields() {
    }

    public static Field field(Class<?> type, String name) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Missing field " + type.getName() + "#" + name, failure);
        }
    }

    public static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            var method = type.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Missing method " + type.getName() + "#" + name, failure);
        }
    }

    public static int intValue(Field field, Object instance) {
        try {
            return field.getInt(instance);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot read field " + field, failure);
        }
    }

    public static void setInt(Field field, Object instance, int value) {
        try {
            field.setInt(instance, value);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot write field " + field, failure);
        }
    }

    public static boolean booleanValue(Field field, Object instance) {
        try {
            return field.getBoolean(instance);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot read field " + field, failure);
        }
    }

    public static Object value(Field field, Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot read field " + field, failure);
        }
    }

    public static void set(Field field, Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot write field " + field, failure);
        }
    }

    public static boolean booleanValue(Method method, Object instance) {
        try {
            return (boolean) method.invoke(instance);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot call method " + method, failure);
        }
    }

    public static void setBoolean(Method method, Object instance, boolean value) {
        try {
            method.invoke(instance, value);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot call method " + method, failure);
        }
    }

    public static void invoke(Method method, Object instance, Object... arguments) {
        try {
            method.invoke(instance, arguments);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot call method " + method, failure);
        }
    }

    public static <T> T call(Method method, Object instance, Class<T> returnType, Object... arguments) {
        try {
            return returnType.cast(method.invoke(instance, arguments));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot call method " + method, failure);
        }
    }
}
