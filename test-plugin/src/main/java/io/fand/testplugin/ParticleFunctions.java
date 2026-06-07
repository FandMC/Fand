package io.fand.testplugin;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

final class ParticleFunctions {

    private ParticleFunctions() {}

    static Function parametric(DoubleUnaryOperator x,
                              DoubleUnaryOperator y,
                              DoubleUnaryOperator z) {
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(y, "y");
        Objects.requireNonNull(z, "z");
        return t -> new Vector(x.applyAsDouble(t), y.applyAsDouble(t), z.applyAsDouble(t));
    }

    static Function circle(double radius, double yOffset) {
        return parametric(
                t -> Math.cos(t) * radius,
                t -> yOffset,
                t -> Math.sin(t) * radius
        );
    }

    static Function helix(double radius, double heightPerTurn) {
        return parametric(
                t -> Math.cos(t) * radius,
                t -> t / (Math.PI * 2.0) * heightPerTurn,
                t -> Math.sin(t) * radius
        );
    }

    static Function sine(double amplitude, double lengthScale) {
        return parametric(
                t -> t * lengthScale,
                t -> Math.sin(t) * amplitude,
                t -> 0.0
        );
    }

    static Function rose(double radius, int petals) {
        return parametric(
                t -> Math.cos(petals * t) * Math.cos(t) * radius,
                t -> 0.0,
                t -> Math.cos(petals * t) * Math.sin(t) * radius
        );
    }

    static Function lissajous(double xAmplitude, double yAmplitude, double zAmplitude,
                              double xFrequency, double yFrequency, double zFrequency) {
        return parametric(
                t -> Math.sin(xFrequency * t) * xAmplitude,
                t -> Math.sin(yFrequency * t) * yAmplitude,
                t -> Math.sin(zFrequency * t) * zAmplitude
        );
    }

    @FunctionalInterface
    interface Function {
        Vector apply(double t);
    }

    record Vector(double x, double y, double z) {
    }
}
