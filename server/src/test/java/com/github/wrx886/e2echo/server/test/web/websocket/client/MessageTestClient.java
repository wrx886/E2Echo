package com.github.wrx886.e2echo.server.test.web.websocket.client;

import java.net.URI;
import com.github.wrx886.e2echo.server.config.WebSocketConfig;

public class MessageTestClient extends BaseTestClient {

    public MessageTestClient(String url) throws Exception {
        super(new URI(url + WebSocketConfig.MessageHandlerPath));
    }

}
