package com.example.webchat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class WebSocketServer {

    private static WebSocketServer wbss;

    private static final int READ_IDLE_TIME_OUT = 60; // 读超时
    private static final int WRITE_IDLE_TIME_OUT = 0;// 写超时
    private static final int ALL_IDLE_TIME_OUT = 0; // 所有超时

    public static WebSocketServer inst() {
        return wbss = new WebSocketServer();
    }

    public void run(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer <SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // Netty's 'http encoder and decoder
                        pipeline.addLast(new HttpServerCodec());
                        // ChunkedWriteHandler: chunked data transfer for long data
                        pipeline.addLast(new ChunkedWriteHandler());

                        // HttpObjectAggregator 是完全的解析Http消息体请求用的
                        // 把多个消息转换为一个单一的完全FullHttpRequest或是FullHttpResponse，
                        // 原因是HTTP解码器会在每个HTTP消息中生成多个消息对象HttpRequest/HttpResponse,HttpContent,LastHttpContent
                        pipeline.addLast(new HttpObjectAggregator(64 * 1024));

                        // WebSocket data compression
                        pipeline.addLast(new WebSocketServerCompressionHandler());

                        // WebSocketServerProtocolHandler: configure websocket's listen channel/package length
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true, 10 * 1024));

                        // when the channel doesn't hear any message in 60s, an IdleStateEvent is triggered
                        // handled by userEventTriggered from HeartbeatHandler
                        pipeline.addLast(
                                new IdleStateHandler(READ_IDLE_TIME_OUT, WRITE_IDLE_TIME_OUT, ALL_IDLE_TIME_OUT, TimeUnit.SECONDS));
                        pipeline.addLast(new WebSocketTextHandler());
                    }
                });
        Channel ch = b.bind(port).syncUninterruptibly().channel();
        ch.closeFuture().syncUninterruptibly();

        // return the runtime object of the current java app
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                SessionGroup.inst().shutdownGracefully();
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
    }
}


