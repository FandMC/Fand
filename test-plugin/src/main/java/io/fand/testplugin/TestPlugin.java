package io.fand.testplugin;

import io.fand.api.block.BlockKey;
import io.fand.api.block.BlockTypes;
import io.fand.api.block.custom.CustomBlockContext;
import io.fand.api.block.custom.CustomBlockListener;
import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.command.CommandContext;
import io.fand.api.entity.Player;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.item.ItemKey;
import io.fand.api.item.ItemTypes;
import io.fand.api.item.component.ItemKeySet;
import io.fand.api.item.component.ItemTool;
import io.fand.api.item.custom.CustomItemType;
import io.fand.api.player.ResourcePackRequest;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class TestPlugin implements Plugin {

    private static final Key BLOCK_ID = Key.key("fand-test-plugin:color_block");
    private static final Key PICKAXE_ID = Key.key("fand-test-plugin:color_pickaxe");
    private static final String COMMAND_PERMISSION = "fand.testplugin.use";

    private Optional<ResourcePackRequest> resourcePackRequest = Optional.empty();
    private CustomItemType blockItem;
    private CustomItemType pickaxe;

    @Override
    public void onLoad(PluginContext context) {
        context.logger().info("Fand example plugin loaded");
    }

    @Override
    public void onEnable(PluginContext context) {
        var block = registerBlock(context);
        registerItems(context, block);
        buildResourcePack(context, block);
        registerCommand(context);
        registerJoinEvent(context);

        context.scheduler().runMainAfterTicks(
                () -> context.logger().info("Fand example plugin is ready"),
                1);
    }

    @Override
    public void onDisable(PluginContext context) {
        resourcePackRequest = Optional.empty();
        context.logger().info("Fand example plugin disabled");
    }

    private CustomBlockType registerBlock(PluginContext context) {
        var block = CustomBlockType.builder(BLOCK_ID, BlockTypes.of(BlockKey.NOTE_BLOCK))
                .state("instrument", "custom_head")
                .state("note", "0")
                .state("powered", "false")
                .mining(4.0F, 8.0F, BlockTypes.of(BlockKey.DIAMOND_BLOCK), true)
                .build();
        return context.customBlocks().register(block, new CustomBlockListener() {
            @Override
            public void placed(CustomBlockContext event) {
                context.logger().info("Placed {} at {}, {}, {}",
                        event.type().id(), event.block().x(), event.block().y(), event.block().z());
            }

            @Override
            public void broken(CustomBlockContext event) {
                context.logger().info("Broke {} at {}, {}, {}",
                        event.type().id(), event.block().x(), event.block().y(), event.block().z());
            }
        }).type();
    }

    private void registerItems(PluginContext context, CustomBlockType block) {
        var blockTemplate = ItemTypes.of(ItemKey.NOTE_BLOCK).one()
                .withItemName(Component.text("Six-sided Color Block", NamedTextColor.AQUA));
        blockItem = context.customItems().register(CustomItemType.builder(BLOCK_ID, blockTemplate.type())
                .components(blockTemplate.components())
                .build()).type();

        var pickaxeTemplate = ItemTypes.of(ItemKey.DIAMOND_PICKAXE).one()
                .withItemName(Component.text("Color Pickaxe", NamedTextColor.LIGHT_PURPLE))
                .withEnchantmentGlintOverride(true);
        pickaxe = context.customItems().register(CustomItemType.builder(PICKAXE_ID, pickaxeTemplate.type())
                .components(pickaxeTemplate.components())
                .customBlockToolRule(ItemTool.Rule.minesAndDrops(ItemKeySet.of(block.id()), 16.0F))
                .build()).type();

        context.customBlocks().bindItem(blockItem.id(), block.id());
    }

    private void buildResourcePack(PluginContext context, CustomBlockType block) {
        var build = ExampleAssets.install(context.resourcePacks(), blockItem, pickaxe, block);
        var url = context.resourcePacks().hostedUrl(build).orElseThrow(() ->
                new IllegalStateException("Enable network.resourcePacks in fand.yml"));
        resourcePackRequest = Optional.of(build.request(
                url,
                context.config().booleanValue("resource-pack.required", false),
                Component.text("Fand example textures")));
        context.logger().info("Resource pack built and hosted at {}", url);
    }

    private void registerCommand(PluginContext context) {
        context.commands().register("fandexample", command -> command
                .permission(COMMAND_PERMISSION)
                .executes(this::sendCommandHelp)
                .literal("give", branch -> branch.executes(this::giveItems))
                .literal("pack", branch -> branch.executes(this::sendPack)));
    }

    private void registerJoinEvent(PluginContext context) {
        context.events().subscribe(PlayerJoinEvent.class, event -> {
            var welcome = context.config().string("welcome-message", "Welcome to the Fand example server.");
            if (!welcome.isBlank()) {
                event.player().sendMessage(Component.text(welcome, NamedTextColor.GREEN));
            }
            resourcePackRequest.ifPresent(event.player()::sendResourcePack);
        });
    }

    private void sendCommandHelp(CommandContext command) {
        command.sender().sendMessage(Component.text("/fandexample give", NamedTextColor.YELLOW));
        command.sender().sendMessage(Component.text("/fandexample pack", NamedTextColor.YELLOW));
    }

    private void giveItems(CommandContext command) {
        if (!(command.sender() instanceof Player player)) {
            command.sender().sendMessage(Component.text("This command requires a player.", NamedTextColor.RED));
            return;
        }
        var blockLeftover = player.inventory().add(blockItem.stack(8));
        var pickaxeLeftover = player.inventory().add(pickaxe.one());
        if (blockLeftover.empty() && pickaxeLeftover.empty()) {
            player.sendMessage(Component.text("Custom items added to your inventory.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Your inventory does not have enough space.", NamedTextColor.RED));
        }
    }

    private void sendPack(CommandContext command) {
        if (!(command.sender() instanceof Player player)) {
            command.sender().sendMessage(Component.text("This command requires a player.", NamedTextColor.RED));
            return;
        }
        if (resourcePackRequest.isEmpty()) {
            player.sendMessage(Component.text("The resource pack is not available.", NamedTextColor.RED));
            return;
        }
        player.sendResourcePack(resourcePackRequest.orElseThrow());
    }
}
