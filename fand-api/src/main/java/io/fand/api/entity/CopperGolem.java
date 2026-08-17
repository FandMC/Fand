package io.fand.api.entity;

import io.fand.api.world.Location;
import java.util.Optional;

/** Copper Golem transport and weathering state. */
public interface CopperGolem extends Mob {

    enum ActivityState { IDLE, GETTING_ITEM, GETTING_NO_ITEM, DROPPING_ITEM, DROPPING_NO_ITEM }
    enum WeatherState { UNAFFECTED, EXPOSED, WEATHERED, OXIDIZED }

    ActivityState activityState();
    void setActivityState(ActivityState state);
    WeatherState weatherState();
    void setWeatherState(WeatherState state);
    Optional<Location> openedContainer();
    void clearOpenedContainer();
    boolean readyForShearing();
}
