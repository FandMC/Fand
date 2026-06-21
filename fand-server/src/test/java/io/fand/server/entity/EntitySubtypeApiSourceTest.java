package io.fand.server.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EntitySubtypeApiSourceTest {

    @Test
    void beeWrapperExposesHiveNectarStingAndAngerState() throws IOException {
        var api = read("../fand-api/src/main/java/io/fand/api/entity/Bee.java");
        var wrapper = read("src/main/java/io/fand/server/entity/FandBee.java");

        assertThat(api).contains("extends Animal, Angerable");
        assertThat(api).contains("Optional<Location> hiveLocation()");
        assertThat(api).contains("void setHasNectar");
        assertThat(api).contains("void setHasStung");
        assertThat(wrapper).contains("handle().getHivePos()");
        assertThat(wrapper).contains("handle().setHivePos");
        assertThat(wrapper).contains("SET_HAS_NECTAR");
        assertThat(wrapper).contains("SET_HAS_STUNG");
        assertThat(wrapper).contains("FandAngerable");
    }

    @Test
    void axolotlWrapperExposesVariantPlayingDeadAndBucketState() throws IOException {
        var api = read("../fand-api/src/main/java/io/fand/api/entity/Axolotl.java");
        var wrapper = read("src/main/java/io/fand/server/entity/FandAxolotl.java");

        assertThat(api).contains("enum Variant");
        assertThat(api).contains("boolean playingDead()");
        assertThat(api).contains("boolean fromBucket()");
        assertThat(wrapper).contains("handle().getVariant()");
        assertThat(wrapper).contains("SET_VARIANT");
        assertThat(wrapper).contains("handle().setPlayingDead");
        assertThat(wrapper).contains("handle().setFromBucket");
    }

    @Test
    void catAndWolfWrappersResolveRegistryVariantsAndCollars() throws IOException {
        var catApi = read("../fand-api/src/main/java/io/fand/api/entity/Cat.java");
        var catWrapper = read("src/main/java/io/fand/server/entity/FandCat.java");
        var wolfApi = read("../fand-api/src/main/java/io/fand/api/entity/Wolf.java");
        var wolfWrapper = read("src/main/java/io/fand/server/entity/FandWolf.java");

        assertThat(catApi).contains("Key variant()");
        assertThat(catApi).contains("ItemDyeColor collarColor()");
        assertThat(catApi).contains("boolean relaxed()");
        assertThat(catWrapper).contains("Registries.CAT_VARIANT");
        assertThat(catWrapper).contains("Registries.CAT_SOUND_VARIANT");
        assertThat(catWrapper).contains("SET_COLLAR_COLOR");
        assertThat(catWrapper).contains("SET_RELAX_STATE_ONE");

        assertThat(wolfApi).contains("extends Animal, Tameable, Angerable");
        assertThat(wolfApi).contains("Key soundVariant()");
        assertThat(wolfApi).contains("boolean interested()");
        assertThat(wolfWrapper).contains("Registries.WOLF_VARIANT");
        assertThat(wolfWrapper).contains("Registries.WOLF_SOUND_VARIANT");
        assertThat(wolfWrapper).contains("SET_COLLAR_COLOR");
        assertThat(wolfWrapper).contains("FandAngerable");
    }

    @Test
    void boatAndWardenWrappersExposeRealEntityState() throws IOException {
        var boatApi = read("../fand-api/src/main/java/io/fand/api/entity/Boat.java");
        var boatWrapper = read("src/main/java/io/fand/server/entity/FandBoat.java");
        var wardenApi = read("../fand-api/src/main/java/io/fand/api/entity/Warden.java");
        var wardenWrapper = read("src/main/java/io/fand/server/entity/FandWarden.java");

        assertThat(boatApi).contains("Key woodType()");
        assertThat(boatApi).contains("boolean paddling");
        assertThat(boatApi).contains("int bubbleTime()");
        assertThat(boatWrapper).contains("BuiltInRegistries.ENTITY_TYPE.getKey");
        assertThat(boatWrapper).contains("handle().setPaddleState");
        assertThat(boatWrapper).contains("GET_BUBBLE_TIME");

        assertThat(wardenApi).contains("enum AngerLevel");
        assertThat(wardenApi).contains("int anger()");
        assertThat(wardenApi).contains("activeAngerTarget()");
        assertThat(wardenWrapper).contains("handle().getAngerManagement().getActiveAnger");
        assertThat(wardenWrapper).contains("handle().increaseAngerAt");
        assertThat(wardenWrapper).contains("handle().clearAnger");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
