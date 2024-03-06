package net.liam.CloudCross;

public class WorldConfig {
    String FILEPATH = "";
    String SAVENAME = "";
    Boolean ENABLED = false;
    public WorldConfig(String filePath, String saveName, boolean enabled) {
        FILEPATH = filePath;
        SAVENAME = saveName;
        ENABLED = enabled;
    }
}
