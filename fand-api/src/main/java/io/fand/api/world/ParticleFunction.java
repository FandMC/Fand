package io.fand.api.world;

/**
 * A mathematical parametric particle function. The input {@code t} is supplied
 * by a sampler, and the returned vector is added to an origin location.
 */
@FunctionalInterface
public interface ParticleFunction {

    /** Returns the relative point for parameter {@code t}. */
    Vector apply(double t);

    /** A relative 3D point produced by a particle function. */
    record Vector(double x, double y, double z) {
    }
}
