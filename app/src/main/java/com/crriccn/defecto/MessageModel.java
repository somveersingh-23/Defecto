package com.crriccn.defecto;
public class MessageModel {
    public static final int USER = 0;
    public static final int BOT = 1;

    private String message;
    private int sender;

    public MessageModel(String message, int sender) {
        this.message = message;
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public int getSender() {
        return sender;
    }
}
