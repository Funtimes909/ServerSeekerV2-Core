package xyz.funtimes909.serverseekerv2_core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.funtimes909.serverseekerv2_core.records.Mod;
import xyz.funtimes909.serverseekerv2_core.records.Player;
import xyz.funtimes909.serverseekerv2_core.records.Server;
import xyz.funtimes909.serverseekerv2_core.records.Version;
import xyz.funtimes909.serverseekerv2_core.types.ServerType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServerObjectBuilder {
    public static Server buildServerFromPing(String address, short port, JsonObject parsedJson) {
        try {
            String version = null;
            String asn = null;
            String country = null;
            String hostname = null;
            String organization = null;
            Integer protocol = null;
            Integer fmlNetworkVersion = null;
            Integer maxPlayers = null;
            Integer onlinePlayers = null;
            ServerType type = null;
            StringBuilder motd = new StringBuilder();
            List<Player> playerList = new ArrayList<>();
            List<Mod> modsList = new ArrayList<>();
            long timestamp = System.currentTimeMillis() / 1000;

            String response = HTTPUtils.run(address);
            if (response != null) {
                JsonObject parsed = JsonParser.parseString(response).getAsJsonObject();

                if (!parsed.get("countryCode").getAsString().isBlank()) {
                    country = parsed.get("countryCode").getAsString();
                }

                if (!parsed.get("reverse").getAsString().isBlank()) {
                    hostname = parsed.get("reverse").getAsString();
                }

                if (!parsed.get("org").getAsString().isBlank()) {
                    organization = parsed.get("org").getAsString();
                }

                if (!parsed.get("as").getAsString().isBlank()) {
                    asn = parsed.get("as").getAsString();
                }
            }

            if (parsedJson.has("version")) {
                Version record = getServerType(parsedJson);

                type = record.type();
                version = record.version();
                protocol = record.protocol();
            }

            // Description can be either an object or a string
            if (parsedJson.has("description")) {
                if (parsedJson.get("description").isJsonObject()) {
                    MotdUtils.buildMOTD(parsedJson.get("description").getAsJsonObject(), 10, motd);
                } else {
                    motd.append(parsedJson.get("description").getAsString());
                }
            }

            // Handle Forge servers
            if (parsedJson.has("forgeData")) {
                fmlNetworkVersion = parsedJson.get("forgeData").getAsJsonObject().get("fmlNetworkVersion").getAsInt();
                type = ServerType.LEXFORGE;
                if (parsedJson.get("forgeData").getAsJsonObject().has("mods")) {
                    for (JsonElement mod : parsedJson.get("forgeData").getAsJsonObject().get("mods").getAsJsonArray().asList()) {
                        modsList.add(new Mod(
                                mod.getAsJsonObject().get("modId").getAsString(),
                                mod.getAsJsonObject().get("modmarker").getAsString()
                        ));
                    }
                }
            }

            // Handle players
            if (parsedJson.has("players")) {
                maxPlayers = parsedJson.get("players").getAsJsonObject().get("max").getAsInt();
                onlinePlayers = parsedJson.get("players").getAsJsonObject().get("online").getAsInt();
                if (parsedJson.get("players").getAsJsonObject().has("sample")) {
                    for (JsonElement playerJson : parsedJson.get("players").getAsJsonObject().get("sample").getAsJsonArray().asList()) {
                        playerList.add(new Player(
                                playerJson.getAsJsonObject().get("name").getAsString(),
                                playerJson.getAsJsonObject().get("id").getAsString(),
                                timestamp,
                                timestamp
                        ));
                    }
                }
            }

            // Build server
            return new Server.Builder()
                    .setAddress(address)
                    .setPort(port)
                    .setServerType(type)
                    .setFirstSeen(timestamp)
                    .setLastSeen(timestamp)
                    .setAsn(asn)
                    .setCountry(country)
                    .setReverseDns(hostname)
                    .setOrganization(organization)
                    .setVersion(version)
                    .setProtocol(protocol)
                    .setFmlNetworkVersion(fmlNetworkVersion)
                    .setMotd(motd.toString())
                    .setTimesSeen(1)
                    .setIcon(parsedJson.has("favicon") ? parsedJson.get("favicon").getAsString() : null)
                    .setPreventsReports(parsedJson.has("preventsChatReports") ? parsedJson.get("preventsChatReports").getAsBoolean() : null)
                    .setEnforceSecure(parsedJson.has("enforcesSecureChat") ? parsedJson.get("enforcesSecureChat").getAsBoolean() : null)
                    .setMaxPlayers(maxPlayers)
                    .setOnlinePlayers(onlinePlayers)
                    .setPlayers(playerList)
                    .setMods(modsList)
                    .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Version getServerType(JsonObject parsedJson) {
        JsonObject object = parsedJson.get("version").getAsJsonObject();
        String version = object.get("name").getAsString();
        int protocol = object.get("protocol").getAsInt();
        ServerType type = ServerType.JAVA;

        if (parsedJson.has("isModded")) {
            type = ServerType.NEOFORGE;
            return new Version(version, protocol, type);
        } else if (parsedJson.has("forgeData")) {
            type = ServerType.LEXFORGE;
            return new Version(version, protocol, type);
        }

        if (!Character.isDigit(version.charAt(0))) {
            type = switch (version.split(" ")[0]) {
                case "Paper" -> ServerType.PAPER;
                case "Velocity" -> ServerType.VELOCITY;
                case "BungeeCord" -> ServerType.BUNGEECORD;
                case "Spigot" -> ServerType.SPIGOT;
                case "CraftBukkit" -> ServerType.BUKKIT;
                case "Folia" -> ServerType.FOLIA;
                case "Pufferfish" -> ServerType.PUFFERFISH;
                case "Purpur" -> ServerType.PURPUR;
                case "Waterfall" -> ServerType.WATERFALL;
                case "Leaves" -> ServerType.LEAVES;
                default -> ServerType.JAVA;
            };
        }

        return new Version(
                version,
                protocol,
                type
        );
    }

    public static Server buildServerFromResultSet(ResultSet results) throws SQLException {
        try (results) {
            Server.Builder server = new Server.Builder();
            List<Player> players = new ArrayList<>();
            List<Mod> mods = new ArrayList<>();

            while (results.next()) {
                server.setAddress(results.getString("address"));
                server.setPort(results.getShort("port"));
                server.setMotd(results.getString("motd"));
                server.setVersion(results.getString("version"));
                server.setFirstSeen(results.getLong("firstseen"));
                server.setLastSeen(results.getLong("lastseen"));
                server.setProtocol(results.getInt("protocol"));
                server.setCountry(results.getString("country"));
                server.setAsn(results.getString("asn"));
                server.setReverseDns(results.getString("reversedns"));
                server.setOrganization(results.getString("organization"));
                server.setWhitelist((Boolean) results.getObject("whitelist"));
                server.setEnforceSecure((Boolean) results.getObject("enforceSecure"));
                server.setCracked((Boolean) results.getObject("cracked"));
                server.setPreventsReports((Boolean) results.getObject("preventsReports"));
                server.setMaxPlayers(results.getInt("maxPlayers"));
                server.setTimesSeen(results.getInt("timesSeen"));
                server.setFmlNetworkVersion(results.getInt("fmlnetworkversion"));

                if (results.getString("playername") != null) players.add(new Player(results.getString("playername"), results.getString("playeruuid"), results.getLong("lastseen"), System.currentTimeMillis() / 1000));
                if (results.getString("modid") != null) mods.add(new Mod(results.getString("modid"), results.getString("modmarker")));
            }

            server.setPlayers(players);
            server.setMods(mods);

            return server.build();
        }
    }

    public static Server buildServerFromApiResponse(JsonObject response) {
        return new Server.Builder()
            .setAddress(response.get("address").getAsString())
            .setPort(response.get("port").getAsShort())
            .setMotd(response.get("motd").getAsString())
            .setVersion(response.get("version").getAsString())
            .setFirstSeen(response.get("firstseen").getAsLong())
            .setLastSeen(response.get("lastseen").getAsLong())
            .setProtocol(response.get("protocol").getAsInt())
            .setCountry(response.get("country").getAsString())
            .setAsn(response.get("asn").getAsString())
            .setReverseDns(response.get("hostname").getAsString())
            .setOrganization(response.get("org").getAsString())
            .setWhitelist(response.get("whitelist").getAsBoolean())
            .setEnforceSecure(response.get("enforces_secure_chat").getAsBoolean())
            .setCracked(response.get("cracked").getAsBoolean())
            .setPreventsReports(response.get("prevents_reports").getAsBoolean())
            .setMaxPlayers(response.get("maxplayers").getAsInt())
            .build();
    }
}
