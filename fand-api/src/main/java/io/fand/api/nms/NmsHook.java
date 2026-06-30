package io.fand.api.nms;

@FunctionalInterface
public interface NmsHook {

    NmsHookResult invoke(NmsHookContext context) throws Exception;
}
