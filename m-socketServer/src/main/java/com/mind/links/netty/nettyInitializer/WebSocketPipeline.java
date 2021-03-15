package com.mind.links.netty.nettyInitializer;

import annotation.Desc;
import com.mind.links.netty.nettyHandler.NettyServerHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.stereotype.Component;

/**
 * @author : qiDing
 * date: 2021-01-02 10:13
 * @version v1.0.0
 */
@Component
@Desc("webSocket解码器")
public class WebSocketPipeline {

    @Desc("netty消息处理类")
    private static final String HANDLER_NAME = NettyServerHandler.class.getSimpleName();

    public void webSocketPipelineAdd(ChannelHandlerContext ctx) {
        WebSocketPipelineProperties wp = new WebSocketPipelineProperties();
        ctx.pipeline().addBefore(HANDLER_NAME, wp.httpServerCodec.getClass().getSimpleName(), wp.httpServerCodec)
                .addBefore(HANDLER_NAME, wp.chunkedWriteHandler.getClass().getSimpleName(), wp.chunkedWriteHandler)
                .addBefore(HANDLER_NAME, wp.httpObjectAggregator.getClass().getSimpleName(), wp.httpObjectAggregator)
                .addBefore(HANDLER_NAME, wp.webSocketFrameAggregator.getClass().getSimpleName(), wp.webSocketFrameAggregator)
                .addBefore(HANDLER_NAME, wp.webSocketServerProtocolHandler.getClass().getSimpleName(), wp.webSocketServerProtocolHandler);
    }

    @Desc("webSocket解码器配置(多例)")
    public static class WebSocketPipelineProperties {

        @Desc("http解码器")
        volatile HttpServerCodec httpServerCodec = new HttpServerCodec();

        @Desc("http聚合器")
        volatile ChunkedWriteHandler chunkedWriteHandler = new ChunkedWriteHandler();

        @Desc("http消息聚合器")
        volatile HttpObjectAggregator httpObjectAggregator = new HttpObjectAggregator(1024 * 62);

        @Desc("webSocket消息聚合器")
        volatile WebSocketFrameAggregator webSocketFrameAggregator = new WebSocketFrameAggregator(1024 * 62);

        @Desc("webSocket支持,设置路由")
        volatile WebSocketServerProtocolHandler webSocketServerProtocolHandler = new WebSocketServerProtocolHandler("/ws");

    }
}
