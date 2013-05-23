package org.jetbrains.plugins.embeditor;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.ide.HttpRequestHandler;

import java.io.IOException;

/**
 * User: zolotov
 * Date: 5/23/13
 */
public class EditorRequestHandler extends HttpRequestHandler {
    @Override
    public boolean process(QueryStringDecoder queryStringDecoder, HttpRequest httpRequest, ChannelHandlerContext channelHandlerContext) throws IOException {
        return false;
    }
}
