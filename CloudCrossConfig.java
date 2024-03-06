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
    public static ForgeConfigSpec.ConfigValue<Boolean> ccEnabled;
    public static ForgeConfigSpec.ConfigValue<Integer> maxBackups;
    public static ForgeConfigSpec.ConfigValue<List<String>> worldPaths;
    public static ForgeConfigSpec.ConfigValue<List<String>> worldNames;
    public static ForgeConfigSpec.ConfigValue<List<Boolean>> enabled;
    static {
        BUILDER.push("Config");
        BUILDER.comment("For first time setup make sure you launch minecraft with this link: https://accounts.google.com/o/oauth2/auth/oauthchooseaccount?access_type=offline&approval_prompt=force&client_id=67117034265-fpgd27545uvujcgpqcma6b99o0bph26j.apps.googleusercontent.com&redirect_uri=http%3A%2F%2Flocalhost%3A8888%2FCallback&response_type=code&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fdrive.file&service=lso&o2v=1&theme=glif&flowName=GeneralOAuthFlow " +
                "\n Once the loading process stops for a sec login and accept required scopes\n If you want to change account delete the StoredCredential file in tokens");
        BUILDER.comment("\n\n");
        ccEnabled = BUILDER.comment("Do you want to enable cc on launch? (true/false)").define("ccEnabled", false);
        maxBackups = BUILDER.comment("Don't recommend to set to more than 5 if you have multiple worlds being saved or if you're too deep into the game\nCheck google drive space if you're worried").define("Max Backups", 5);
        worldPaths = BUILDER.comment("Match array positions for each world file and complete all req for each property \nMake sure to put a comma between each field ex: '',''\nCopy paste world path ").define("World Paths", new ArrayList<>());
        worldNames = BUILDER.comment("World name in google drive MAKE SURE THIS IS DIFFERENT FOR EACH SAVED WORLD \nDoes not need to be the same as the actual world name in minercaft").define("World Names", new ArrayList<>());
        enabled = BUILDER.comment("Do you want this file to be uploaded and downloaded using CC \ntrue/false").define("World Status", new ArrayList<>());
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
