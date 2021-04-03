package com.luvtas.taseats.Model;

public class ChatMessageModel {
    private String uid, name, content, pictureLink,client_send;
    private boolean isPicture;
    private Long timeStamp;

    public ChatMessageModel() {
    }

    public String getClient_send() {
        return client_send;
    }

    public void setClient_send(String client_send) {
        this.client_send = client_send;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPictureLink() {
        return pictureLink;
    }

    public void setPictureLink(String pictureLink) {
        this.pictureLink = pictureLink;
    }

    public boolean isPicture() {
        return isPicture;
    }

    public void setPicture(boolean picture) {
        isPicture = picture;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
