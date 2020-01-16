package com.marcusvmleite.gs;

public enum Messages {

    GREETING("HI, I AM %s"),
    GREETING_REPLY("HI %s"),
    FAREWELL("BYE %s, WE SPOKE FOR %d MS"),
    SORRY("SORRY, I DID NOT UNDERSTAND THAT"),

    NODE_ADDED("NODE ADDED"),
    NODE_REMOVED("NODE REMOVED"),
    NODE_EXISTS("ERROR: NODE ALREADY EXISTS"),
    NODE_NOT_FOUND("ERROR: NODE NOT FOUND"),
    EDGE_REMOVED("EDGE REMOVED"),
    EDGE_ADDED("EDGE ADDED");

    private String message;

    Messages(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }

}
