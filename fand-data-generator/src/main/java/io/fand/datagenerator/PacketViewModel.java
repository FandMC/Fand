package io.fand.datagenerator;

import java.util.List;
import java.util.Objects;

record PacketViewModel(
        String typeName,
        String sourceClassName,
        boolean replaceable,
        List<PacketFieldModel> fields
) {

    static final String PACKAGE_NAME = "io.fand.api.packet.view";

    PacketViewModel {
        Objects.requireNonNull(typeName, "typeName");
        Objects.requireNonNull(sourceClassName, "sourceClassName");
        fields = List.copyOf(fields);
    }

    String qualifiedName() {
        return PACKAGE_NAME + "." + typeName;
    }
}
