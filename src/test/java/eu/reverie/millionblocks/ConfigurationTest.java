package eu.reverie.millionblocks;
import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;
import com.google.gson.JsonParser;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {
    @Test void loadsDefaultConfigurationWithPaperYamlParser() throws Exception {
        var config=new YamlConfiguration();
        config.loadFromString(Files.readString(Path.of("src/main/resources/config.yml")));
        assertEquals("betterhud",config.getString("hud.mode"));
        assertEquals(1000000,config.getLong("challenge.target-blocks"));
        assertTrue(config.getBoolean("portal.use-custom-model"));
        assertEquals(8,config.getConfigurationSection("items").getKeys(false).size());
    }
    @Test void pluginDescriptorDeclaresOptionalPaperIntegration() throws Exception {
        var config=new YamlConfiguration();
        config.loadFromString(Files.readString(Path.of("src/main/resources/plugin.yml")));
        assertEquals("26.2",config.getString("api-version"));assertEquals("0.7.0",config.getString("version"));
        assertEquals(java.util.List.of("BetterHud"),config.getStringList("softdepend"));
    }
    @Test void everyPlayableItemMapsToAnExistingModernItemDefinition() throws Exception {
        for(ToolId id:ToolId.playable()) {
            String model=ItemFactory.modelId(id);
            Path path=Path.of("assets/resourcepack/assets/millionblocks/items",model+".json");
            var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonObject("model");
            assertEquals("minecraft:model",json.get("type").getAsString());
            assertEquals("millionblocks:item/"+model,json.get("model").getAsString());
        }
    }
}
