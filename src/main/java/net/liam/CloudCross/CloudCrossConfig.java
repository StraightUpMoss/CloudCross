package net.liam.CloudCross;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public final class CloudCrossConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec SPEC = null;
    public static ForgeConfigSpec.ConfigValue<String> exampleString;
    static {
        BUILDER.push("Config");
        exampleString = BUILDER.comment("Test").define("Example 1", "TestStr");
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
