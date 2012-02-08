package com.cgbystrom.sockjs.transports;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for streaming transports
 *
 * Handles HTTP chunking and response size limiting for browser "garbage collection".
 */
public class StreamingTransport extends BaseTransport {
    /**
     *  Max size of response content sent before closing the connection.
     *  Since browsers buffer chunked/streamed content in-memory the connection must be closed
     *  at regular intervals. Call it "garbage collection" if you will.
     */
    protected final int maxResponseSize;

    /** Track size of content chunks sent to the browser. */
    protected AtomicInteger numBytesSent = new AtomicInteger(0);

    /** For streaming/chunked transports we need to send HTTP header only once (naturally) */
    protected AtomicBoolean headerSent = new AtomicBoolean(false);

    public StreamingTransport() {
        this.maxResponseSize = 128 * 1024; // 128 KiB
    }

    public StreamingTransport(int maxResponseSize) {
        this.maxResponseSize = maxResponseSize;
    }

    protected void logResponseSize(Channel channel, ChannelBuffer content) {
        numBytesSent.addAndGet(content.readableBytes());

        if (numBytesSent.get() >= maxResponseSize) {
            // Close the connection to allow the browser to flush in-memory buffered content from this XHR stream.
            channel.write(HttpChunk.LAST_CHUNK).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    protected HttpResponse createResponse(String contentType) {
        HttpResponse response = super.createResponse(contentType);
        response.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
        return response;
    }
}
