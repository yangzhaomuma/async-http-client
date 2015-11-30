/*
 * Copyright (c) 2014 AsyncHttpClient Project. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 *     http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.asynchttpclient.netty.request.body;

import static org.asynchttpclient.util.Assertions.assertNotNull;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

import org.asynchttpclient.request.body.Body;

/**
 * Adapts a {@link Body} to Netty's {@link ChunkedInput}.
 */
public class BodyChunkedInput implements ChunkedInput<ByteBuf> {

    public static final int DEFAULT_CHUNK_SIZE = 8 * 1024;

    private final Body body;
    private final long length;
    private final int chunkSize;
    private long progress = 0L;
    private boolean endOfInput;

    public BodyChunkedInput(Body body) {
        assertNotNull(body, "body");
        this.body = body;
        this.length = body.getContentLength();
        if (length <= 0)
            chunkSize = DEFAULT_CHUNK_SIZE;
        else
            chunkSize = (int) Math.min(length, (long) DEFAULT_CHUNK_SIZE);
    }

    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {

        if (endOfInput)
            return null;

        ByteBuf buffer = ctx.alloc().buffer(chunkSize);
        Body.BodyState state = body.transferTo(buffer);
        progress += buffer.readableBytes();
        switch (state) {
            case STOP:
                endOfInput = true;
                return buffer;
            case SUSPEND:
                //this will suspend the stream in ChunkedWriteHandler
                return null;
            case CONTINUE:
                return buffer;
            default:
                throw new IllegalStateException("Unknown state: " + state);
        }
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return endOfInput;
    }

    @Override
    public void close() throws Exception {
        body.close();
    }
    
    @Override
    public long progress() {
        return progress;
    }
    
    @Override
    public long length() {
        return length;
    }
}
