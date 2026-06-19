package io.fand.api.block;

public interface SculkSensorBlockEntity extends BlockEntity {
    int lastVibrationFrequency();

    void setLastVibrationFrequency(int frequency);

    SculkSensorPhase phase();

    int power();

    void activate(int power, int vibrationFrequency);

    void deactivate();
}
