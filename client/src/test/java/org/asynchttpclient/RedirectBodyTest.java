package org.asynchttpclient;

import static org.asynchttpclient.Dsl.*;
import static org.testng.Assert.*;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.asynchttpclient.filter.FilterContext;
import org.asynchttpclient.filter.FilterException;
import org.asynchttpclient.filter.ResponseFilter;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.annotations.Test;

public class RedirectBodyTest extends AbstractBasicTest {

    private static final String TRANSACTION_ID_HEADER = "X-TRANSACTION-ID";
    private final ConcurrentHashMap<String, String> receivedContentTypes = new ConcurrentHashMap<String, String>();

    private String body = "hello there";
    private String contentType = "text/plain";

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new AbstractHandler() {
            @Override
            public void handle(String pathInContext, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

                String redirectHeader = httpRequest.getHeader("X-REDIRECT");
                if (redirectHeader != null) {
                    httpResponse.setStatus(Integer.valueOf(redirectHeader));
                    httpResponse.setContentLength(0);
                    httpResponse.setHeader(HttpHeaderNames.LOCATION.toString(), getTargetUrl());

                } else {
                    String transactionId = httpRequest.getHeader(TRANSACTION_ID_HEADER);
                    String contentType = request.getContentType();
                    if (transactionId != null && contentType != null) {
                        receivedContentTypes.put(transactionId, contentType);
                    }

                    httpResponse.setStatus(200);
                    int len = request.getContentLength();
                    httpResponse.setContentLength(len);

                    if (len > 0) {
                        byte[] buffer = new byte[len];
                        IOUtils.read(request.getInputStream(), buffer);
                        httpResponse.getOutputStream().write(buffer);
                    }
                }
                httpResponse.getOutputStream().flush();
                httpResponse.getOutputStream().close();
            }
        };
    }

    private Response sendRequest(AsyncHttpClient client, String redirectCode, String transactionId) throws Exception {
        return client.preparePost(getTargetUrl())//
                .setHeader(HttpHeaderNames.CONTENT_TYPE, contentType)//
                .setHeader("X-REDIRECT", redirectCode)//
                .setHeader(TRANSACTION_ID_HEADER, transactionId)//
                .setBody(body)//
                .execute()//
                .get(TIMEOUT, TimeUnit.SECONDS);
    }

    private ResponseFilter redirectOnce = new ResponseFilter() {
        @Override
        public <T> FilterContext<T> filter(FilterContext<T> ctx) throws FilterException {
            ctx.getRequest().getHeaders().remove("X-REDIRECT");
            return ctx;
        }
    };

    @Test(groups = "standalone")
    public void regular301LosesBody() throws Exception {
        try (AsyncHttpClient client = asyncHttpClient(config().setFollowRedirect(true).addResponseFilter(redirectOnce))) {
            String id = UUID.randomUUID().toString();
            Response response = sendRequest(client, "301", id);
            assertEquals(response.getResponseBody(), "");
            assertNull(receivedContentTypes.get(id));
        }
    }

    @Test(groups = "standalone")
    public void regular302LosesBody() throws Exception {
        try (AsyncHttpClient client = asyncHttpClient(config().setFollowRedirect(true).addResponseFilter(redirectOnce))) {
            String id = UUID.randomUUID().toString();
            Response response = sendRequest(client, "302", id);
            assertEquals(response.getResponseBody(), "");
            assertNull(receivedContentTypes.get(id));
        }
    }

    @Test(groups = "standalone")
    public void regular302StrictKeepsBody() throws Exception {
        try (AsyncHttpClient client = asyncHttpClient(config().setFollowRedirect(true).setStrict302Handling(true).addResponseFilter(redirectOnce))) {
            String id = UUID.randomUUID().toString();
            Response response = sendRequest(client, "302", id);
            assertEquals(response.getResponseBody(), body);
            assertEquals(receivedContentTypes.get(id), contentType);
        }
    }

    @Test(groups = "standalone")
    public void regular307KeepsBody() throws Exception {
        try (AsyncHttpClient client = asyncHttpClient(config().setFollowRedirect(true).addResponseFilter(redirectOnce))) {
            String id = UUID.randomUUID().toString();
            Response response = sendRequest(client, "307", id);
            assertEquals(response.getResponseBody(), body);
            assertEquals(receivedContentTypes.get(id), contentType);
        }
    }
}
