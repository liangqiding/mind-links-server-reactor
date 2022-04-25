package com.mind.links.netty.nettyInitializer;

import annotation.Desc;
import com.mind.links.netty.nettyHandler.*;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * Netty编码解码器
 *
 * @author qiding
 */
@Desc("编码解码器")
@Component
@RequiredArgsConstructor
public class  NettyServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Desc("通道主动处理程序(单例)")
    private final ChannelActiveHandler channelActiveHandler;

    @Desc("频道无效的处理程序(单例)")
    private final ChannelInactiveHandler channelInactiveHandler;

    @Desc("IO处理程序(单例)")
    private final NettyServerHandler nettyServerHandler;

    @Desc("异常处理程序(单例)")
    private final ExceptionHandler exceptionHandler;

    @Override
    protected void initChannel(SocketChannel channel) {
        final NettyInitializerProperties nip = new NettyInitializerProperties();
        channel.pipeline()
                .addLast(channelActiveHandler.getClass().getSimpleName(), channelActiveHandler)
                .addLast(channelInactiveHandler.getClass().getSimpleName(), channelInactiveHandler)
                .addLast(nip.socketChooseHandler.getClass().getSimpleName(), nip.socketChooseHandler)
                .addLast(nip.idleStateHandler.getClass().getSimpleName(), nip.idleStateHandler)
                .addLast(nettyServerHandler.getClass().getSimpleName(), nettyServerHandler)
                .addLast(exceptionHandler.getClass().getSimpleName(), exceptionHandler);
    }

    /**
     * 为了最大程度地减少对象的创建
     */
    @Desc("通用编码解码器配置")
    public static class NettyInitializerProperties {

        @Desc("心跳机制(多例)")
        volatile IdleStateHandler idleStateHandler = new IdleStateHandler(60, 0, 0);

        @Desc("协议选择处理程序(多例)")
        volatile SocketChooseHandler socketChooseHandler = new SocketChooseHandler();

    }
}


