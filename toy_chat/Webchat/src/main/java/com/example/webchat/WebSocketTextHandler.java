package com.example.webchat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

public class WebSocketTextHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        SocketSession session = SocketSession.getSession(ctx);
        TypeToken<HashMap<String, String>> typeToken = new TypeToken<HashMap<String, String>>() {
        };

        Gson gson=new Gson();
        java.util.Map<String,String> map = gson.fromJson(msg.text(), typeToken.getType());
        User user = null;
        switch (map.get("type")) {
            case "msg":
                Map<String, String> result = new HashMap<>();
                user = session.getUser();
                result.put("type", "msg");
                result.put("msg", map.get("msg"));
                result.put("sendUser", user.getNickname());
                SessionGroup.inst().sendToOthers(result, session);
                break;
            case "init":
                String room = map.get("room");
                session.setGroup(room);
                String nick = map.get("nick");
                user = new User(session.getId(), nick);
                session.setUser(user);
                SessionGroup.inst().addSession(session);
                break;
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {


        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {

            new SocketSession(ctx.channel());
        } else if (evt instanceof IdleStateEvent) {
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            if (stateEvent.state() == IdleState.READER_IDLE) {
                System.out.println("bb22");
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}

