package com.example.webchat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SocketSession {

    public static final AttributeKey<SocketSession> SESSION_KEY = AttributeKey.valueOf("SESSION_KEY");


// 通道
    private Channel channel;
    private User user;

    private final String sessionId;

    private String group;

    private Map<String, Object> map = new HashMap<String, Object>();

    public SocketSession(Channel channel) {//different client has different channel
        this.channel = channel;
        this.sessionId = buildNewSessionId();
        channel.attr(SocketSession.SESSION_KEY).set(this);
    }

    // 反向导航
    public static SocketSession getSession(ChannelHandlerContext ctx) {//different client has different ctx
        Channel channel = ctx.channel();
        return channel.attr(SocketSession.SESSION_KEY).get();
    }

    // 反向导航
    public static SocketSession getSession(Channel channel) {
        return channel.attr(SocketSession.SESSION_KEY).get();
    }

    public String getId() {
        return sessionId;
    }

    private static String buildNewSessionId() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replaceAll("-", "");
    }

    public synchronized void set(String key, Object value) {
        map.put(key, value);
    }

    public synchronized <T> T get(String key) {
        return (T) map.get(key);
    }

    public boolean isValid() {
        return getUser() != null ? true : false;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Channel getChannel() {
        return channel;
    }
}

