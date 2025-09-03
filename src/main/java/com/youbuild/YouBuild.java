package com.youbuild;

import com.ldtteam.structurize.storage.SurvivalBlueprintHandlers;
import com.youbuild.blueprinthandler.Red;
import com.youbuild.command.Command;
import com.youbuild.item.Cardiac;
import com.youbuild.network.NetworkHandler;
import com.youbuild.ui.BuildUI;
import com.youbuild.ui.ComparisonUI;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.youbuild.Config.createConfig;

@Mod(YouBuild.ModId)
public class YouBuild {
    public static final String ModId = "youbuild";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ModId);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModId);

    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ModId);

    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ModId);

    public static final DeferredHolder<Item, Cardiac> Cardiac_ITEM = ITEMS.register("cardiac",
            () -> new Cardiac(new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STRUCTURE_BUILDER_TAB = CREATIVE_TABS.register(
            "you_build_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.youbuild.you_build_tab"))
                    .icon(() -> new ItemStack(Cardiac_ITEM.get()))
                    .displayItems((params, output) -> output.accept(Cardiac_ITEM.get()))
                    .build()
    );

    public YouBuild(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::onCommonSetup);

        NetworkHandler.register(modEventBus);
        new Red(modEventBus);

    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {

        //获取服务端根目录
        Path serverRoot = event.getServer().getServerDirectory();

        //创建config根目录
        Path configDir = serverRoot.resolve("youbuild");
        ensureDirectoryExists(configDir);
        createConfig(configDir);

        //commands存储
        Path comPath = serverRoot.resolve("youbuild/commands/");
        ensureDirectoryExists(comPath);

        //hide存储
        Path hidPath = serverRoot.resolve("./youbuild/hide");
        ensureDirectoryExists(hidPath);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        Cardiac.registerEventListeners();
        event.enqueueWork(() -> {
            //注册ui
            SurvivalBlueprintHandlers.registerHandler(new ComparisonUI());
            SurvivalBlueprintHandlers.registerHandler(new BuildUI());
        });
    }

    @SubscribeEvent
    private void onRegisterCommands(RegisterCommandsEvent event) {
        //注册命令
        Command.register(event.getDispatcher());
    }

    private void ensureDirectoryExists(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("[YouBuild] Created directory: " + path);
            }
        } catch (Exception e) {
            System.err.println("[YouBuild] ERROR: Failed to create directory");
        }
    }
}