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

import com.yoshio3.sounds.SoundUtil;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author Yoshio Terada
 */
@ServerEndpoint("/uploadSoundWebSocketEndpoint/{from}/{to}")
public class SoundDataUploadWebSocketServerEndpoint {

    private static final String TRANSLATOR_WEBSOCKET_ENDPOINT = "wss://dev.microsofttranslator.com/speech/translate?";
    private static final byte[] SILENCE_BYTE = new byte[32000];
    private static final int SEND_BUFFER_SIZE = 64000;
    private static final Logger LOGGER = Logger.getLogger(SoundDataUploadWebSocketServerEndpoint.class.getPackage().getName());

    private String microsoftTranslatorURI;

    @OnOpen
    public void onOpen(@PathParam("from") String from, @PathParam("to") String to, Session session) {
        microsoftTranslatorURI = TRANSLATOR_WEBSOCKET_ENDPOINT + "from=" + from + "&to=" + to + "&api-version=1.0";
        LOGGER.log(Level.INFO, "SoundDataUploadWebSocketServerEndpoint Open WebSocket Connection");
    }

    /**
     * Received the Sound Data(wav) from Browser
     *
     * @param message sound data(wav) from browser
     * @param session WebSocket session
     * @throws URISyntaxException
     * @throws DeploymentException
     * @throws IOException
     * @throws InterruptedException
     */
    @OnMessage
    public void onMessage(ByteBuffer message, Session session) throws URISyntaxException, DeploymentException, IOException, InterruptedException {
        LOGGER.log(Level.FINE, "SoundDataUploadWebSocketServerEndpoint onMessage{0}", message);
        sendSoundDataToMicrosoftTranslator(session, message);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        LOGGER.log(Level.FINE, "SoundDataUploadWebSocketServerEndpoint Close WebSocket Connection : ", reason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable t) {
        LOGGER.log(Level.SEVERE, "SoundDataUploadWebSocketServerEndpoint onError()", t);
    }

    private void sendSoundDataToMicrosoftTranslator(Session soundUpSession, ByteBuffer message) throws DeploymentException, IOException, InterruptedException, URISyntaxException {

        LOGGER.log(Level.FINE, "MSG CAPACITY : {0}", message.capacity());
        byte[] originalSoundData = message.array();
        try {
            SoundUtil soundUtil = new SoundUtil();
            byte[] monoSound = soundUtil.convertMonoralSound(originalSoundData);
            if (monoSound != null) {
                URI serverEndpointUri = new URI(microsoftTranslatorURI);
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                Session translatorSession = container.connectToServer(new TranslatorWebSockerClientEndpoint(soundUpSession), serverEndpointUri);

                translatorSession.getBasicRemote().sendBinary(ByteBuffer.wrap(monoSound));
                for (int i = 0; i < 10; i++) {
                    translatorSession.getBasicRemote().getSendStream().write(SILENCE_BYTE);
                }
            }

        } catch (UnsupportedAudioFileException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
