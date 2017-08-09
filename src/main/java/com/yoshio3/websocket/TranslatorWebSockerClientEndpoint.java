/*
 * Copyright 2017 Yoshio Terada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yoshio3.websocket;

import com.yoshio3.websocket.config.TranslatorWebSocketClientEndpointConfigurator;
import com.yoshio3.websocket.message.MessageDataFromTranslator;
import com.yoshio3.websocket.message.MessageDecoder;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 *
 * @author Yoshio Terada
 */
@ClientEndpoint(
        configurator = TranslatorWebSocketClientEndpointConfigurator.class,
        decoders = {MessageDecoder.class})
public class TranslatorWebSockerClientEndpoint {

    private Session session;
    private Session soundUpWebSocketSession;
    private static final Logger LOGGER = Logger.getLogger(TranslatorWebSockerClientEndpoint.class.getName());

    public TranslatorWebSockerClientEndpoint() {
        super();
    }

    public TranslatorWebSockerClientEndpoint(Session soundUpWebSocketSession) {
        this.soundUpWebSocketSession = soundUpWebSocketSession;
    }

    @OnOpen
    public void onOpen(Session session) throws IOException, InterruptedException, DeploymentException {
        this.session = session;
        LOGGER.log(Level.FINE, "MAX IDLE TIME{0}", session.getMaxIdleTimeout());
        LOGGER.log(Level.FINE, "MAX BUFFER SIZE{0}", session.getMaxBinaryMessageBufferSize());
        LOGGER.log(Level.INFO, "MS-Translator Connect");
    }

    @OnClose
    public void onClose(CloseReason reason) {
        this.session = null;
        LOGGER.log(Level.FINE, "MS-Translator Close: {0}", reason.getReasonPhrase());
    }

    @OnError
    public void onError(Throwable throwable) {
        this.session = null;
        LOGGER.log(Level.SEVERE, null, throwable);
    }

    /**
     * Received the response message from Microsoft Translator
     * 
     * @param message response data from Translator (decoded Object from JSON) 
     * @throws IOException 
     */
    
    @OnMessage
    public void onMessage(MessageDataFromTranslator message) throws IOException {
        LOGGER.log(Level.INFO, "MS-Translator : {0} ", message);
        JsonObject jsonObj = Json.createObjectBuilder()
                .add("type", message.getType())
                .add("origin", message.getRecognition())
                .add("translated", message.getTranslation())
                .build();
        soundUpWebSocketSession.getBasicRemote().sendText(jsonObj.toString());
    }
}
