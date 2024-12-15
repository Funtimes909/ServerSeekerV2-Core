package xyz.funtimes909.serverseekerv2_core.types;

public enum ServerType {
    JAVA("Java"),
    BEDROCK("Bedrock"),
    NEOFORGE("Neoforge"),
    LEXFORGE("Neoforge"),
    PAPER("Paper"),
    SPIGOT("Spigot"),
    PURPUR("Purpur"),
    PUFFERFISH("Pufferfish"),
    VELOCITY("Velocity"),
    LEAVES("Leaves"),
    WATERFALL("Waterfall"),
    BUNGEECORD("BungeeCord"),
    BUKKIT("CraftBukkit"),
    THERMOS("thermos"),
    LEGACY("Legacy");

    ServerType(String versionName) {
        this.versionName = versionName;
    }

    public final String versionName;
}