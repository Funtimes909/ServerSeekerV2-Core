package xyz.funtimes909.serverseekerv2_core.util;

import com.google.gson.JsonElement;
import xyz.funtimes909.serverseekerv2_core.types.AnsiCodes;

import java.util.Map;

public class MotdUtils {
    public static void buildMOTD(JsonElement element, int limit, StringBuilder motd) {
        if (limit == 0) return;

        if (element.isJsonObject()) {
            Map<String, JsonElement> map = element.getAsJsonObject().asMap();

            if (map.containsKey("text")) {
                if (map.containsKey("color")) {
                    if (!map.get("color").getAsString().startsWith("#")) {
                        motd.append('§').append(AnsiCodes.codes.get(map.get("color").getAsString()).c);
                    }
                }

                if (map.containsKey("bold")) {
                    motd.append("§l");
                }

                if (map.containsKey("underlined")) {
                    motd.append("§n");
                }

                motd.append(map.get("text").getAsString());
            }

            if (map.containsKey("extra")) {
                buildMOTD(map.get("extra"), limit, motd);
            }
        } else {
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                if (jsonElement.isJsonPrimitive()) {
                    motd.append(jsonElement.getAsString());
                } else {
                    buildMOTD(jsonElement, limit, motd);
                }
            }
        }
    }
}
