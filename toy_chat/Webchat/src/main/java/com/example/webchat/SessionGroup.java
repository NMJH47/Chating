package com.example.webchat;

import com.google.gson.Gson;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionGroup {

    private static SessionGroup singleInstance = new SessionGroup();

    // 组的映射
    private ConcurrentHashMap<String, ChannelGroup> groupMap = new ConcurrentHashMap<>();

    public static SessionGroup inst() {
        return singleInstance;
    }

    public void shutdownGracefully() {

        Iterator<ChannelGroup> groupIterator = groupMap.values().iterator();
        while (groupIterator.hasNext()) {
            ChannelGroup group = groupIterator.next();
            group.close();
        }
    }

    public void sendToOthers(Map<String, String> result, SocketSession s) {
        // 获取组
        ChannelGroup group = groupMap.get(s.getGroup());
        if (null == group) {
            return;
        }
        Gson gson=new Gson();
        String json = gson.toJson(result);

        ChannelGroupFuture future = group.writeAndFlush(new TextWebSocketFrame(json));
        future.addListener(f -> {
            System.out.println("Sent："+json);
//          group.add(channel);

        });
    }

    public void addSession(SocketSession session) {

        String groupName = session.getGroup();
        if (StringUtils.isEmpty(groupName)) {
            // group is empty, return directly
            return;
        }
        ChannelGroup group = groupMap.get(groupName);
        if (null == group) {
            group = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
            groupMap.put(groupName, group);
        }
        group.add(session.getChannel());
    }

    /**
     * Close connection with echo
     */
    public void closeSession(SocketSession session, String echo) {
        ChannelFuture sendFuture = session.getChannel().writeAndFlush(new TextWebSocketFrame(echo));
        sendFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                System.out.println("Close Connection："+echo);
                future.channel().close();
            }
        });
    }

    /**
     * Close Connection
     */
    public void closeSession(SocketSession session) {

        ChannelFuture sendFuture = session.getChannel().close();
        sendFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                System.out.println("Sent to all: "+session.getUser().getNickname());
            }
        });

    }

    /**
     * send a message
     * @param ctx context
     * @param msg message to be sent
     */
    public void sendMsg(ChannelHandlerContext ctx, String msg) {
        ChannelFuture sendFuture = ctx.writeAndFlush(new TextWebSocketFrame(msg));
        sendFuture.addListener(f -> {
            System.out.println("Sent to all: "+msg);
        });
    }
}

