package com.example.telegraminstasavebot.model;

public class Media {
    String id;
    String url;
    User user;
    String caption;
    MediaType mediaType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public String toString() {
        return "Media{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", user=" + user +
                ", caption='" + caption + '\'' +
                ", mediaType=" + mediaType +
                '}';
    }
}
