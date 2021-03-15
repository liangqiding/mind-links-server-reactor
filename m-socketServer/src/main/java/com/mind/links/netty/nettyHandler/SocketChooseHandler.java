package com.mind.links.netty.nettyHandler;

import annotation.Desc;
import com.mind.links.netty.nettyInitializer.WebSocketPipeline;
import context.SpringBeanFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 *  * 协议初始化解码器. *
 *  * 用来判定实际使用什么协议.</b> *
 *
 * @author : qiDing
 * date: 2021-01-02 10:06
 * @version v1.0.0
 */
@Component
@Desc("协议选择器")
@Slf4j
public class SocketChooseHandler extends ByteToMessageDecoder {

    @Desc("协议最大长度")
    private static final int MAX_LENGTH = 23;

    @Desc("webSocket握手的协议前缀")
    private static final String WEB_SOCKET_PREFIX = "GET /";

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        String protocol = getBufStart(in);
        if (protocol.startsWith(WEB_SOCKET_PREFIX)) {
            SpringBeanFactory.getBean(WebSocketPipeline.class).webSocketPipelineAdd(ctx);
            ctx.pipeline().remove(IdleStateHandler.class);
        }
        in.resetReaderIndex();
        ctx.pipeline().remove(this.getClass());
    }

    private String getBufStart(ByteBuf in){
        int length = in.readableBytes();
        if (length > MAX_LENGTH) {
            length = MAX_LENGTH;
        }
        // 标记读位置
        in.markReaderIndex();
        byte[] content = new byte[length];
        in.readBytes(content);
        return new String(content);
    }
}
