package net.liam.CloudCross;


import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CloudCross.MODID)
public class CloudCross
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "cloudcross";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "CloudCross" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "CloudCross" namespace

    public CloudCross()
    {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading

        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        //ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        //CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        //modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        //ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)  {
        // Some common setup code
        //LOGGER.info("HELLO FROM COMMON SETUP");
        try {
            CloudCrossDriveManager.main();
        }
        catch (IOException ioException){
            System.err.println(ioException);
        }
        catch(GeneralSecurityException generalSecurityException) {
            System.err.println((generalSecurityException));
        }
    }

    // Add the example block item to the building blocks tab
    /*private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }
*/
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        //LOGGER.info("HELLO from server starting");
    }


    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) throws GeneralSecurityException, IOException {

            // Some client setup code
            //LOGGER.info("HELLO FROM CLIENT SETUP");
            //LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());


            //Call CloudCross functions here.
            //Download any updated files
        }

    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeModEvents {
        @SubscribeEvent
        public static void onGameShutown(GameShuttingDownEvent event) {
            //Called on game close
            LOGGER.info("GAME SHUT DOWN");
        }
        @SubscribeEvent
        public static void LevelEvent(LevelEvent.Save event) {
            //Called everytime host hits escape so might not be best
            LOGGER.info("Saved new chunk");
        }
        @SubscribeEvent
        public static void OnAutoSave(PlayerEvent.SaveToFile event) throws GeneralSecurityException, IOException {
            String filePath = event.getPlayerDirectory().getParentFile().getAbsolutePath();
            LOGGER.info(filePath);
            //CloudCrossDriveManager.UploadFileFromMC(filePath);
        }
        @SubscribeEvent
        public static void OnServerShutdown(ServerStoppedEvent event) {

            LOGGER.info("SERVER STOPPED");
            //CloudCrossDriveManager.UploadFile();
        }
    }

}
