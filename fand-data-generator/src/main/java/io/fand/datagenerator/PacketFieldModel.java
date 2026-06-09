package io.fand.datagenerator;

import java.util.Objects;

record PacketFieldModel(
        String name,
        String accessorName,
        String apiType,
        String classLiteral,
        boolean wildcardType
) {

    PacketFieldModel {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(accessorName, "accessorName");
        Objects.requireNonNull(apiType, "apiType");
        Objects.requireNonNull(classLiteral, "classLiteral");
    }
}
