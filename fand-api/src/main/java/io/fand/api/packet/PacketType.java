package io.fand.api.packet;

import io.fand.api.packet.view.ServerboundChatCommandView;
import io.fand.api.packet.view.ServerboundChatView;
import io.fand.api.packet.view.ServerboundContainerClickView;
import io.fand.api.packet.view.ServerboundInteractView;
import io.fand.api.packet.view.ServerboundMovePlayerView;
import io.fand.api.packet.view.ServerboundPlayerActionView;
import io.fand.api.packet.view.ServerboundSetCreativeModeSlotView;
import io.fand.api.packet.view.ServerboundSwingView;
import io.fand.api.packet.view.ServerboundUseItemOnView;
import io.fand.api.packet.view.ServerboundUseItemView;
import io.fand.api.packet.view.ClientboundAddEntityView;
import io.fand.api.packet.view.ClientboundBlockEntityDataView;
import io.fand.api.packet.view.ClientboundBlockUpdateView;
import io.fand.api.packet.view.ClientboundContainerSetContentView;
import io.fand.api.packet.view.ClientboundContainerSetSlotView;
import io.fand.api.packet.view.ClientboundGameEventView;
import io.fand.api.packet.view.ClientboundLevelEventView;
import io.fand.api.packet.view.ClientboundLevelParticlesView;
import io.fand.api.packet.view.ClientboundMoveEntityView;
import io.fand.api.packet.view.ClientboundOpenScreenView;
import io.fand.api.packet.view.ClientboundPlayerChatView;
import io.fand.api.packet.view.ClientboundPlayerPositionView;
import io.fand.api.packet.view.ClientboundRemoveEntitiesView;
import io.fand.api.packet.view.ClientboundRespawnView;
import io.fand.api.packet.view.ClientboundSetActionBarTextView;
import io.fand.api.packet.view.ClientboundSetEntityDataView;
import io.fand.api.packet.view.ClientboundSetEntityMotionView;
import io.fand.api.packet.view.ClientboundSetEquipmentView;
import io.fand.api.packet.view.ClientboundSetHealthView;
import io.fand.api.packet.view.ClientboundSetTimeView;
import io.fand.api.packet.view.ClientboundSetTitleTextView;
import io.fand.api.packet.view.ClientboundSoundView;
import io.fand.api.packet.view.ClientboundSystemChatView;
import io.fand.api.packet.view.ClientboundTeleportEntityView;
import io.fand.api.packet.view.ClientboundUpdateAttributesView;

/**
 * Stable, closed set of interceptable vanilla packet types.
 *
 * <p>Constants are version-controlled by Fand and never expose vanilla types.
 * When Minecraft changes its packet classes, only the internal
 * {@code enum -> vanilla class} mapping in the server module changes; this
 * enum and the public API stay fixed. Each constant carries its
 * {@link PacketDirection} and the view record type used to read and replace
 * its fields.
 */
public enum PacketType {
    C2S_CLIENT_INTENTION(PacketDirection.INBOUND, PacketView.class),
    C2S_ACCEPT_CODE_OF_CONDUCT(PacketDirection.INBOUND, PacketView.class),
    C2S_ACCEPT_TELEPORTATION(PacketDirection.INBOUND, PacketView.class),
    C2S_ATTACK(PacketDirection.INBOUND, PacketView.class),
    C2S_BLOCK_ENTITY_TAG_QUERY(PacketDirection.INBOUND, PacketView.class),
    C2S_CHANGE_DIFFICULTY(PacketDirection.INBOUND, PacketView.class),
    C2S_CHANGE_GAME_MODE(PacketDirection.INBOUND, PacketView.class),
    C2S_CHAT_ACK(PacketDirection.INBOUND, PacketView.class),
    C2S_CHAT_COMMAND(PacketDirection.INBOUND, ServerboundChatCommandView.class),
    C2S_CHAT_COMMAND_SIGNED(PacketDirection.INBOUND, PacketView.class),
    C2S_CHAT(PacketDirection.INBOUND, ServerboundChatView.class),
    C2S_CHAT_SESSION_UPDATE(PacketDirection.INBOUND, PacketView.class),
    C2S_CHUNK_BATCH_RECEIVED(PacketDirection.INBOUND, PacketView.class),
    C2S_CLIENT_COMMAND(PacketDirection.INBOUND, PacketView.class),
    C2S_CLIENT_INFORMATION(PacketDirection.INBOUND, PacketView.class),
    C2S_CLIENT_TICK_END(PacketDirection.INBOUND, PacketView.class),
    C2S_COMMAND_SUGGESTION(PacketDirection.INBOUND, PacketView.class),
    C2S_CONFIGURATION_ACKNOWLEDGED(PacketDirection.INBOUND, PacketView.class),
    C2S_CONTAINER_BUTTON_CLICK(PacketDirection.INBOUND, PacketView.class),
    C2S_CONTAINER_CLICK(PacketDirection.INBOUND, ServerboundContainerClickView.class),
    C2S_CONTAINER_CLOSE(PacketDirection.INBOUND, PacketView.class),
    C2S_CONTAINER_SLOT_STATE_CHANGED(PacketDirection.INBOUND, PacketView.class),
    C2S_CUSTOM_CLICK_ACTION(PacketDirection.INBOUND, PacketView.class),
    C2S_CUSTOM_PAYLOAD(PacketDirection.INBOUND, PacketView.class),
    C2S_CUSTOM_QUERY_ANSWER(PacketDirection.INBOUND, PacketView.class),
    C2S_DEBUG_SUBSCRIPTION_REQUEST(PacketDirection.INBOUND, PacketView.class),
    C2S_EDIT_BOOK(PacketDirection.INBOUND, PacketView.class),
    C2S_ENTITY_TAG_QUERY(PacketDirection.INBOUND, PacketView.class),
    C2S_FINISH_CONFIGURATION(PacketDirection.INBOUND, PacketView.class),
    C2S_HELLO(PacketDirection.INBOUND, PacketView.class),
    C2S_INTERACT(PacketDirection.INBOUND, ServerboundInteractView.class),
    C2S_JIGSAW_GENERATE(PacketDirection.INBOUND, PacketView.class),
    C2S_KEEP_ALIVE(PacketDirection.INBOUND, PacketView.class),
    C2S_KEY(PacketDirection.INBOUND, PacketView.class),
    C2S_LOCK_DIFFICULTY(PacketDirection.INBOUND, PacketView.class),
    C2S_LOGIN_ACKNOWLEDGED(PacketDirection.INBOUND, PacketView.class),
    C2S_MOVE_PLAYER(PacketDirection.INBOUND, ServerboundMovePlayerView.class),
    C2S_MOVE_VEHICLE(PacketDirection.INBOUND, PacketView.class),
    C2S_PADDLE_BOAT(PacketDirection.INBOUND, PacketView.class),
    C2S_PICK_ITEM_FROM_BLOCK(PacketDirection.INBOUND, PacketView.class),
    C2S_PICK_ITEM_FROM_ENTITY(PacketDirection.INBOUND, PacketView.class),
    C2S_PING_REQUEST(PacketDirection.INBOUND, PacketView.class),
    C2S_PLACE_RECIPE(PacketDirection.INBOUND, PacketView.class),
    C2S_PLAYER_ABILITIES(PacketDirection.INBOUND, PacketView.class),
    C2S_PLAYER_ACTION(PacketDirection.INBOUND, ServerboundPlayerActionView.class),
    C2S_PLAYER_COMMAND(PacketDirection.INBOUND, PacketView.class),
    C2S_PLAYER_INPUT(PacketDirection.INBOUND, PacketView.class),
    C2S_PLAYER_LOADED(PacketDirection.INBOUND, PacketView.class),
    C2S_PONG(PacketDirection.INBOUND, PacketView.class),
    C2S_RECIPE_BOOK_CHANGE_SETTINGS(PacketDirection.INBOUND, PacketView.class),
    C2S_RECIPE_BOOK_SEEN_RECIPE(PacketDirection.INBOUND, PacketView.class),
    C2S_RENAME_ITEM(PacketDirection.INBOUND, PacketView.class),
    C2S_RESOURCE_PACK(PacketDirection.INBOUND, PacketView.class),
    C2S_SEEN_ADVANCEMENTS(PacketDirection.INBOUND, PacketView.class),
    C2S_SELECT_BUNDLE_ITEM(PacketDirection.INBOUND, PacketView.class),
    C2S_SELECT_KNOWN_PACKS(PacketDirection.INBOUND, PacketView.class),
    C2S_SELECT_TRADE(PacketDirection.INBOUND, PacketView.class),
    C2S_SET_BEACON(PacketDirection.INBOUND, PacketView.class),
    C2S_SET_CARRIED_ITEM(PacketDirection.INBOUND, PacketView.class),
    C2S_SET_COMMAND_BLOCK(PacketDirection.INBOUND, PacketView.class),
    C2S_SET_COMMAND_MINECART(PacketDirection.INBOUND, PacketView.class),
    C2S_SET_CREATIVE_MODE_SLOT(PacketDirection.INBOUND, ServerboundSetCreativeModeSlotView.class),
    C2S_SET_GAME_RULE(PacketDirection.INBOUND, PacketView.class),
    C2S_SET_JIGSAW_BLOCK(PacketDirection.INBOUND, PacketView.class),
    C2S_SET_STRUCTURE_BLOCK(PacketDirection.INBOUND, PacketView.class),
    C2S_SET_TEST_BLOCK(PacketDirection.INBOUND, PacketView.class),
    C2S_SIGN_UPDATE(PacketDirection.INBOUND, PacketView.class),
    C2S_SPECTATE_ENTITY(PacketDirection.INBOUND, PacketView.class),
    C2S_STATUS_REQUEST(PacketDirection.INBOUND, PacketView.class),
    C2S_SWING(PacketDirection.INBOUND, ServerboundSwingView.class),
    C2S_TELEPORT_TO_ENTITY(PacketDirection.INBOUND, PacketView.class),
    C2S_TEST_INSTANCE_BLOCK_ACTION(PacketDirection.INBOUND, PacketView.class),
    C2S_USE_ITEM_ON(PacketDirection.INBOUND, ServerboundUseItemOnView.class),
    C2S_USE_ITEM(PacketDirection.INBOUND, ServerboundUseItemView.class),
    S2C_ADD_ENTITY(PacketDirection.OUTBOUND, ClientboundAddEntityView.class),
    S2C_ANIMATE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_AWARD_STATS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_BLOCK_CHANGED_ACK(PacketDirection.OUTBOUND, PacketView.class),
    S2C_BLOCK_DESTRUCTION(PacketDirection.OUTBOUND, PacketView.class),
    S2C_BLOCK_ENTITY_DATA(PacketDirection.OUTBOUND, ClientboundBlockEntityDataView.class),
    S2C_BLOCK_EVENT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_BLOCK_UPDATE(PacketDirection.OUTBOUND, ClientboundBlockUpdateView.class),
    S2C_BOSS_EVENT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_BUNDLE_DELIMITER(PacketDirection.OUTBOUND, PacketView.class),
    S2C_BUNDLE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CHANGE_DIFFICULTY(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CHUNK_BATCH_FINISHED(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CHUNK_BATCH_START(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CHUNKS_BIOMES(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CLEAR_DIALOG(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CLEAR_TITLES(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CODE_OF_CONDUCT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_COMMAND_SUGGESTIONS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_COMMANDS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CONTAINER_CLOSE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CONTAINER_SET_CONTENT(PacketDirection.OUTBOUND, ClientboundContainerSetContentView.class),
    S2C_CONTAINER_SET_DATA(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CONTAINER_SET_SLOT(PacketDirection.OUTBOUND, ClientboundContainerSetSlotView.class),
    S2C_COOLDOWN(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CUSTOM_CHAT_COMPLETIONS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CUSTOM_PAYLOAD(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CUSTOM_QUERY(PacketDirection.OUTBOUND, PacketView.class),
    S2C_CUSTOM_REPORT_DETAILS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_DAMAGE_EVENT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_DEBUG_BLOCK_VALUE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_DEBUG_CHUNK_VALUE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_DEBUG_ENTITY_VALUE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_DEBUG_EVENT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_DEBUG_SAMPLE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_DELETE_CHAT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_DISCONNECT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_DISGUISED_CHAT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_ENTITY_EVENT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_ENTITY_POSITION_SYNC(PacketDirection.OUTBOUND, PacketView.class),
    S2C_EXPLODE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_FINISH_CONFIGURATION(PacketDirection.OUTBOUND, PacketView.class),
    S2C_FORGET_LEVEL_CHUNK(PacketDirection.OUTBOUND, PacketView.class),
    S2C_GAME_EVENT(PacketDirection.OUTBOUND, ClientboundGameEventView.class),
    S2C_GAME_RULE_VALUES(PacketDirection.OUTBOUND, PacketView.class),
    S2C_GAME_TEST_HIGHLIGHT_POS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_HELLO(PacketDirection.OUTBOUND, PacketView.class),
    S2C_HURT_ANIMATION(PacketDirection.OUTBOUND, PacketView.class),
    S2C_INITIALIZE_BORDER(PacketDirection.OUTBOUND, PacketView.class),
    S2C_KEEP_ALIVE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_LEVEL_CHUNK_WITH_LIGHT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_LEVEL_EVENT(PacketDirection.OUTBOUND, ClientboundLevelEventView.class),
    S2C_LEVEL_PARTICLES(PacketDirection.OUTBOUND, ClientboundLevelParticlesView.class),
    S2C_LIGHT_UPDATE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_LOGIN_COMPRESSION(PacketDirection.OUTBOUND, PacketView.class),
    S2C_LOGIN_DISCONNECT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_LOGIN_FINISHED(PacketDirection.OUTBOUND, PacketView.class),
    S2C_LOGIN(PacketDirection.OUTBOUND, PacketView.class),
    S2C_LOW_DISK_SPACE_WARNING(PacketDirection.OUTBOUND, PacketView.class),
    S2C_MAP_ITEM_DATA(PacketDirection.OUTBOUND, PacketView.class),
    S2C_MERCHANT_OFFERS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_MOUNT_SCREEN_OPEN(PacketDirection.OUTBOUND, PacketView.class),
    S2C_MOVE_ENTITY(PacketDirection.OUTBOUND, ClientboundMoveEntityView.class),
    S2C_MOVE_MINECART(PacketDirection.OUTBOUND, PacketView.class),
    S2C_MOVE_VEHICLE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_OPEN_BOOK(PacketDirection.OUTBOUND, PacketView.class),
    S2C_OPEN_SCREEN(PacketDirection.OUTBOUND, ClientboundOpenScreenView.class),
    S2C_OPEN_SIGN_EDITOR(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PING(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PLACE_GHOST_RECIPE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PLAYER_ABILITIES(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PLAYER_CHAT(PacketDirection.OUTBOUND, ClientboundPlayerChatView.class),
    S2C_PLAYER_COMBAT_END(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PLAYER_COMBAT_ENTER(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PLAYER_COMBAT_KILL(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PLAYER_INFO_REMOVE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PLAYER_INFO_UPDATE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PLAYER_LOOK_AT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PLAYER_POSITION(PacketDirection.OUTBOUND, ClientboundPlayerPositionView.class),
    S2C_PLAYER_ROTATION(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PONG_RESPONSE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_PROJECTILE_POWER(PacketDirection.OUTBOUND, PacketView.class),
    S2C_RECIPE_BOOK_ADD(PacketDirection.OUTBOUND, PacketView.class),
    S2C_RECIPE_BOOK_REMOVE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_RECIPE_BOOK_SETTINGS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_REGISTRY_DATA(PacketDirection.OUTBOUND, PacketView.class),
    S2C_REMOVE_ENTITIES(PacketDirection.OUTBOUND, ClientboundRemoveEntitiesView.class),
    S2C_REMOVE_MOB_EFFECT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_RESET_CHAT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_RESET_SCORE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_RESOURCE_PACK_POP(PacketDirection.OUTBOUND, PacketView.class),
    S2C_RESOURCE_PACK_PUSH(PacketDirection.OUTBOUND, PacketView.class),
    S2C_RESPAWN(PacketDirection.OUTBOUND, ClientboundRespawnView.class),
    S2C_ROTATE_HEAD(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SECTION_BLOCKS_UPDATE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SELECT_ADVANCEMENTS_TAB(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SELECT_KNOWN_PACKS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SERVER_DATA(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SERVER_LINKS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_ACTION_BAR_TEXT(PacketDirection.OUTBOUND, ClientboundSetActionBarTextView.class),
    S2C_SET_BORDER_CENTER(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_BORDER_LERP_SIZE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_BORDER_SIZE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_BORDER_WARNING_DELAY(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_BORDER_WARNING_DISTANCE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_CAMERA(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_CHUNK_CACHE_CENTER(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_CHUNK_CACHE_RADIUS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_CURSOR_ITEM(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_DEFAULT_SPAWN_POSITION(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_DISPLAY_OBJECTIVE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_ENTITY_DATA(PacketDirection.OUTBOUND, ClientboundSetEntityDataView.class),
    S2C_SET_ENTITY_LINK(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_ENTITY_MOTION(PacketDirection.OUTBOUND, ClientboundSetEntityMotionView.class),
    S2C_SET_EQUIPMENT(PacketDirection.OUTBOUND, ClientboundSetEquipmentView.class),
    S2C_SET_EXPERIENCE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_HEALTH(PacketDirection.OUTBOUND, ClientboundSetHealthView.class),
    S2C_SET_HELD_SLOT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_OBJECTIVE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_PASSENGERS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_PLAYER_INVENTORY(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_PLAYER_TEAM(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_SCORE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_SIMULATION_DISTANCE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_SUBTITLE_TEXT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SET_TIME(PacketDirection.OUTBOUND, ClientboundSetTimeView.class),
    S2C_SET_TITLE_TEXT(PacketDirection.OUTBOUND, ClientboundSetTitleTextView.class),
    S2C_SET_TITLES_ANIMATION(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SHOW_DIALOG(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SOUND_ENTITY(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SOUND(PacketDirection.OUTBOUND, ClientboundSoundView.class),
    S2C_START_CONFIGURATION(PacketDirection.OUTBOUND, PacketView.class),
    S2C_STATUS_RESPONSE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_STOP_SOUND(PacketDirection.OUTBOUND, PacketView.class),
    S2C_STORE_COOKIE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_SYSTEM_CHAT(PacketDirection.OUTBOUND, ClientboundSystemChatView.class),
    S2C_TAB_LIST(PacketDirection.OUTBOUND, PacketView.class),
    S2C_TAG_QUERY(PacketDirection.OUTBOUND, PacketView.class),
    S2C_TAKE_ITEM_ENTITY(PacketDirection.OUTBOUND, PacketView.class),
    S2C_TELEPORT_ENTITY(PacketDirection.OUTBOUND, ClientboundTeleportEntityView.class),
    S2C_TEST_INSTANCE_BLOCK_STATUS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_TICKING_STATE(PacketDirection.OUTBOUND, PacketView.class),
    S2C_TICKING_STEP(PacketDirection.OUTBOUND, PacketView.class),
    S2C_TRACKED_WAYPOINT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_TRANSFER(PacketDirection.OUTBOUND, PacketView.class),
    S2C_UPDATE_ADVANCEMENTS(PacketDirection.OUTBOUND, PacketView.class),
    S2C_UPDATE_ATTRIBUTES(PacketDirection.OUTBOUND, ClientboundUpdateAttributesView.class),
    S2C_UPDATE_ENABLED_FEATURES(PacketDirection.OUTBOUND, PacketView.class),
    S2C_UPDATE_MOB_EFFECT(PacketDirection.OUTBOUND, PacketView.class),
    S2C_UPDATE_RECIPES(PacketDirection.OUTBOUND, PacketView.class),
    S2C_UPDATE_TAGS(PacketDirection.OUTBOUND, PacketView.class);

    private final PacketDirection direction;
    private final Class<? extends PacketView> viewType;

    PacketType(PacketDirection direction, Class<? extends PacketView> viewType) {
        this.direction = direction;
        this.viewType = viewType;
    }

    /** The travel direction of this packet relative to the server. */
    public PacketDirection direction() {
        return direction;
    }

    /** The view interface an interceptor receives for this type ({@link PacketView} if untyped). */
    public Class<? extends PacketView> viewType() {
        return viewType;
    }
}
