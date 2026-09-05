package io.fand.server.tick;

public record OwnershipCell(int x, int z) implements Comparable<OwnershipCell> {

    @Override
    public int compareTo(OwnershipCell other) {
        int horizontal = Integer.compare(x, other.x);
        return horizontal != 0 ? horizontal : Integer.compare(z, other.z);
    }
}
