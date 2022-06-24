package com.example.telegraminstasavebot.model;

public class Message {
    Long message_id;
    Chat chat;
    String text;

    public Long getMessage_id() {
        return message_id;
    }

    public void setMessage_id(Long message_id) {
        this.message_id = message_id;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Message{" +
                "message_id=" + message_id +
                ", chat=" + chat +
                ", text='" + text + '\'' +
                '}';
    }
}
