package io.fand.api.nms;

import java.util.Optional;

/**
 * Low-level access to the server's Minecraft internals.
 *
 * <p>This API is intentionally unsafe and version-coupled. Prefer stable Fand
 * API types whenever they expose the behavior you need.
 */
public interface NmsAccess {

    Object server();

    Object handle(Object apiObject);

    Optional<Object> handleOrEmpty(Object apiObject);

    Class<?> type(String className);

    <T> Class<? extends T> type(String className, Class<T> expectedType);

    Object get(Object target, String fieldName);

    <T> T get(Object target, String fieldName, Class<T> expectedType);

    void set(Object target, String fieldName, Object value);

    Object call(Object target, String methodName, Object... args);

    <T> T call(Object target, String methodName, Class<T> expectedType, Object... args);

    Object construct(String className, Object... args);

    <T> T construct(String className, Class<T> expectedType, Object... args);
}
