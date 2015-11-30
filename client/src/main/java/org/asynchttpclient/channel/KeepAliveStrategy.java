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
package org.asynchttpclient.channel;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;

import org.asynchttpclient.Request;

public interface KeepAliveStrategy {

    /**
     * Determines whether the connection should be kept alive after this HTTP message exchange.
     * @param ahcRequest the Request, as built by AHC
     * @param nettyRequest the HTTP request sent to Netty
     * @param nettyResponse the HTTP response received from Netty
     * @return true if the connection should be kept alive, false if it should be closed.
     */
    boolean keepAlive(Request ahcRequest, HttpRequest nettyRequest, HttpResponse nettyResponse);

    /**
     * Connection strategy implementing standard HTTP 1.0/1.1 behavior.
     */
    enum DefaultKeepAliveStrategy implements KeepAliveStrategy {
        
        INSTANCE;

        /**
         * Implemented in accordance with RFC 7230 section 6.1
         * https://tools.ietf.org/html/rfc7230#section-6.1
         */
        @Override
        public boolean keepAlive(Request ahcRequest, HttpRequest request, HttpResponse response) {

            String responseConnectionHeader = connectionHeader(response);

            if (CLOSE.contentEqualsIgnoreCase(responseConnectionHeader)) {
                return false;
            } else {
                String requestConnectionHeader = connectionHeader(request);

                if (request.protocolVersion() == HttpVersion.HTTP_1_0) {
                    // only use keep-alive if both parties agreed upon it
                    return KEEP_ALIVE.contentEqualsIgnoreCase(requestConnectionHeader) && KEEP_ALIVE.contentEqualsIgnoreCase(responseConnectionHeader);

                } else {
                    // 1.1+, keep-alive is default behavior
                    return !CLOSE.contentEqualsIgnoreCase(requestConnectionHeader);
                }
            }
        }

        private String connectionHeader(HttpMessage message) {
            return message.headers().get(CONNECTION);
        }
    }
}
