package io.fand.datagenerator;

import java.util.Comparator;
import java.util.List;

record PacketCatalog(List<PacketEntry> packets, List<PacketViewModel> views) {

    PacketCatalog {
        packets = List.copyOf(packets);
        views = views.stream()
                .sorted(Comparator.comparing(PacketViewModel::typeName))
                .toList();
    }
}
