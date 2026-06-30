package io.fand.api.nms;

public interface NmsService {

    NmsAccess access();

    NmsHookService hooks();

    static NmsService empty() {
        return Empty.INSTANCE;
    }

    enum Empty implements NmsService {
        INSTANCE;

        @Override
        public NmsAccess access() {
            throw new UnsupportedOperationException("NMS access is not supported");
        }

        @Override
        public NmsHookService hooks() {
            return NmsHookService.empty();
        }
    }
}
