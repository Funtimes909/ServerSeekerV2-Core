package xyz.funtimes909.serverseekerv2_core.records;

import xyz.funtimes909.serverseekerv2_core.types.ServerType;

public record Version(String version, int protocol, ServerType type) {}