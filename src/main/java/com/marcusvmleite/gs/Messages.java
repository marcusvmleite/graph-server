package com.marcusvmleite.gs;

public enum Messages {

    PREFIX_HI("HI"),
    PREFIX_BYE("BYE"),
    PREFIX_ADD_NODE("ADD NODE"),
    PREFIX_ADD_EDGE("ADD EDGE"),
    PREFIX_REMOVE_NODE("REMOVE NODE"),
    PREFIX_REMOVE_EDGE("REMOVE EDGE"),

    NODE_REMOVED("NODE REMOVED"),
    EDGE_REMOVED("EDGE REMOVED"),

    GS_000("HI, I AM"),
    GS_001("HI, I AM %s"),
    GS_002("HI %s"),
    GS_003("BYE MATE!"),
    GS_004("BYE %s, WE SPOKE FOR %d MS"),
    GS_005("SORRY, I DID NOT UNDERSTAND THAT"),

    GS_007("ADD NODE %s"),
    GS_008("NODE ADDED"),
    GS_009("ERROR: NODE ALREADY EXISTS"),
    GS_0011("EDGE ADDED"),
    GS_0012("ERROR: NODE NOT FOUND");

    private String message;

    Messages(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
