package xyz.funtimes909.serverseekerv2_core.database;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.funtimes909.serverseekerv2_core.records.Mod;
import xyz.funtimes909.serverseekerv2_core.records.Player;
import xyz.funtimes909.serverseekerv2_core.records.Server;
import xyz.funtimes909.serverseekerv2_core.records.Version;
import xyz.funtimes909.serverseekerv2_core.types.ServerType;
import xyz.funtimes909.serverseekerv2_core.util.HTTPUtils;
import xyz.funtimes909.serverseekerv2_core.util.MotdUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database{
    private static void init(Connection conn) throws SQLException {
        try (conn) {
            Statement tables = conn.createStatement();

            // Servers
            tables.addBatch("CREATE TABLE IF NOT EXISTS Servers (" +
                    "Address TEXT," +
                    "Port NUMERIC," +
                    "Type TEXT," +
                    "FirstSeen INT," +
                    "LastSeen INT," +
                    "Country TEXT," +
                    "Asn TEXT," +
                    "ReverseDNS TEXT," +
                    "Organization TEXT," +
                    "Version TEXT," +
                    "Protocol INT," +
                    "FmlNetworkVersion INT," +
                    "Motd TEXT," +
                    "Icon TEXT," +
                    "TimesSeen INT," +
                    "PreventsReports BOOLEAN DEFAULT NULL," +
                    "EnforceSecure BOOLEAN DEFAULT NULL," +
                    "Whitelist BOOLEAN DEFAULT NULL," +
                    "Cracked BOOLEAN DEFAULT NULL ," +
                    "MaxPlayers INT," +
                    "OnlinePlayers INT," +
                    "PRIMARY KEY (Address, Port))");

            // Player History
            tables.addBatch("CREATE TABLE IF NOT EXISTS PlayerHistory (" +
                    "Address TEXT," +
                    "Port INT," +
                    "PlayerUUID TEXT," +
                    "PlayerName TEXT," +
                    "FirstSeen INT," +
                    "LastSeen INT," +
                    "PRIMARY KEY (Address, Port, PlayerUUID)," +
                    "FOREIGN KEY (Address, Port) REFERENCES Servers(Address, Port))");

            // Mods
            tables.addBatch("CREATE TABLE IF NOT EXISTS Mods (" +
                    "Address TEXT," +
                    "Port INT," +
                    "ModID TEXT," +
                    "ModMarker TEXT," +
                    "PRIMARY KEY (Address, Port, ModId)," +
                    "FOREIGN KEY (Address, Port) REFERENCES Servers(Address, Port))");

            // Indexes
            tables.addBatch("CREATE INDEX IF NOT EXISTS ServersIndex ON Servers (Motd, version, onlineplayers, country, cracked, whitelist, lastseen)");
            tables.addBatch("CREATE INDEX IF NOT EXISTS PlayersIndex ON PlayerHistory (playername)");
            tables.addBatch("CREATE INDEX IF NOT EXISTS ModsIndex ON Mods (modid)");

            tables.executeBatch();
            tables.close();
        }
    }

    public static void updateServer(Connection conn, Server server) throws SQLException {
        try (conn) {
            String address = server.getAddress();
            short port = server.getPort();

            // Attempt to insert new server, if address and port already exist, update relevant information
            PreparedStatement insertServer = conn.prepareStatement("INSERT INTO Servers " +
                    "(Address," +
                    "Port," +
                    "Type," +
                    "FirstSeen," +
                    "LastSeen," +
                    "Country," +
                    "Asn," +
                    "ReverseDNS," +
                    "Organization," +
                    "Version," +
                    "Protocol," +
                    "FmlNetworkVersion," +
                    "Motd," +
                    "Icon," +
                    "TimesSeen," +
                    "PreventsReports," +
                    "EnforceSecure," +
                    "Whitelist," +
                    "Cracked," +
                    "MaxPlayers," +
                    "OnlinePlayers)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                    "ON CONFLICT (Address, Port) DO UPDATE SET " +
                    "LastSeen = EXCLUDED.LastSeen," +
                    "Type = EXCLUDED.Type," +
                    "Country = EXCLUDED.Country," +
                    "Asn = EXCLUDED.Asn," +
                    "ReverseDNS = EXCLUDED.ReverseDNS," +
                    "Organization = EXCLUDED.Organization," +
                    "Version = EXCLUDED.Version," +
                    "Protocol = EXCLUDED.Protocol," +
                    "FmlNetworkVersion = EXCLUDED.FmlNetworkVersion," +
                    "Motd = EXCLUDED.Motd," +
                    "Icon = EXCLUDED.Icon," +
                    "TimesSeen = Servers.TimesSeen + 1," +
                    "PreventsReports = EXCLUDED.PreventsReports," +
                    "EnforceSecure = EXCLUDED.EnforceSecure," +
                    "Whitelist = EXCLUDED.Whitelist," +
                    "Cracked = EXCLUDED.Cracked," +
                    "MaxPlayers = EXCLUDED.MaxPlayers," +
                    "OnlinePlayers = EXCLUDED.OnlinePlayers");

            // Set most values as objects to insert a null if value doesn't exist
            insertServer.setString(1, server.getAddress());
            insertServer.setInt(2, server.getPort());
            insertServer.setString(3, server.getServerType().name());
            insertServer.setLong(4, server.getFirstSeen());
            insertServer.setLong(5, server.getLastSeen());
            insertServer.setString(6, server.getCountry());
            insertServer.setString(7, server.getAsn());
            insertServer.setString(8, server.getReverseDns());
            insertServer.setString(9, server.getOrganization());
            insertServer.setString(10, server.getVersion());
            insertServer.setObject(11, server.getProtocol(), Types.INTEGER);
            insertServer.setObject(12, server.getFmlNetworkVersion(), Types.INTEGER);
            insertServer.setString(13, server.getMotd());
            insertServer.setString(14, server.getIcon());
            insertServer.setInt(15, server.getTimesSeen());
            insertServer.setObject(16, server.getPreventsReports(), Types.BOOLEAN);
            insertServer.setObject(17, server.getEnforceSecure(), Types.BOOLEAN);
            insertServer.setObject(18, server.getWhitelist(), Types.BOOLEAN);
            insertServer.setObject(19, server.getCracked(), Types.BOOLEAN);
            insertServer.setObject(20, server.getMaxPlayers(), Types.INTEGER);
            insertServer.setObject(21, server.getOnlinePlayers(), Types.INTEGER);
            insertServer.executeUpdate();
            insertServer.close();

            // Add players, update LastSeen and Name (Potential name change) if duplicate
            if (!server.getPlayers().isEmpty()) {
                PreparedStatement updatePlayers = conn.prepareStatement("INSERT INTO PlayerHistory (Address, Port, PlayerUUID, PlayerName, FirstSeen, LastSeen) VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (Address, Port, PlayerUUID) DO UPDATE SET " +
                        "LastSeen = EXCLUDED.LastSeen," +
                        "PlayerName = EXCLUDED.PlayerName");

                // Constants
                updatePlayers.setString(1, address);
                updatePlayers.setShort(2, port);

                for (Player player : server.getPlayers()) {
                    updatePlayers.setString(3, player.uuid());
                    updatePlayers.setString(4, player.name());
                    updatePlayers.setLong(5, player.firstseen());
                    updatePlayers.setLong(6, player.lastseen());
                    updatePlayers.addBatch();
                }

                // Execute and close
                updatePlayers.executeBatch();
                updatePlayers.close();
            }

            // Add mods, do nothing if duplicate
            if (!server.getMods().isEmpty()) {
                PreparedStatement updateMods = conn.prepareStatement("INSERT INTO Mods (Address, Port, ModId, ModMarker) " +
                        "VALUES (?, ?, ?, ?)" +
                        "ON CONFLICT (Address, Port, ModId) DO NOTHING");

                // Constants
                updateMods.setString(1, address);
                updateMods.setShort(2, port);

                for (Mod mod : server.getMods()) {
                    updateMods.setString(3, mod.modid());
                    updateMods.setString(4, mod.modmarker());
                    updateMods.addBatch();
                }

                // Execute and close
                updateMods.executeBatch();
                updateMods.close();
            }
        }
    }

    public static Server buildServer(String address, short port, JsonObject parsedJson) {
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
}