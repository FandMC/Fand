package io.fand.api.world;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/** Factory methods for mathematical particle functions. */
public final class ParticleFunctions {

    private ParticleFunctions() {}

    public static ParticleFunction parametric(DoubleUnaryOperator x,
                                              DoubleUnaryOperator y,
                                              DoubleUnaryOperator z) {
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(y, "y");
        Objects.requireNonNull(z, "z");
        return t -> new ParticleFunction.Vector(x.applyAsDouble(t), y.applyAsDouble(t), z.applyAsDouble(t));
    }

    public static ParticleFunction explicit(DoubleUnaryOperator y) {
        return explicit(y, 1.0);
    }

    public static ParticleFunction explicit(DoubleUnaryOperator y, double xScale) {
        Objects.requireNonNull(y, "y");
        return t -> new ParticleFunction.Vector(t * xScale, y.applyAsDouble(t), 0.0);
    }

    public static ParticleFunction circle(double radius) {
        return circle(radius, 0.0);
    }

    public static ParticleFunction circle(double radius, double yOffset) {
        return parametric(
                t -> Math.cos(t) * radius,
                t -> yOffset,
                t -> Math.sin(t) * radius
        );
    }

    public static ParticleFunction helix(double radius, double heightPerTurn) {
        return parametric(
                t -> Math.cos(t) * radius,
                t -> t / (Math.PI * 2.0) * heightPerTurn,
                t -> Math.sin(t) * radius
        );
    }

    public static ParticleFunction sine(double amplitude, double lengthScale) {
        return parametric(
                t -> t * lengthScale,
                t -> Math.sin(t) * amplitude,
                t -> 0.0
        );
    }

    public static ParticleFunction rose(double radius, int petals) {
        return parametric(
                t -> Math.cos(petals * t) * Math.cos(t) * radius,
                t -> 0.0,
                t -> Math.cos(petals * t) * Math.sin(t) * radius
        );
    }

    public static ParticleFunction lissajous(double xAmplitude, double yAmplitude, double zAmplitude,
                                             double xFrequency, double yFrequency, double zFrequency) {
        return parametric(
                t -> Math.sin(xFrequency * t) * xAmplitude,
                t -> Math.sin(yFrequency * t) * yAmplitude,
                t -> Math.sin(zFrequency * t) * zAmplitude
        );
    }
}
