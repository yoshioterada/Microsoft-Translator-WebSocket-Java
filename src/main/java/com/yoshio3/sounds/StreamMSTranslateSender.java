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
package com.yoshio3.sounds;

import com.yoshio3.websocket.TranslatorWebSockerClientEndpoint;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 *
 * @author Yoshio Terada
 */
@Singleton
public class StreamMSTranslateSender {

    private final BlockingQueue<byte[]> audioQueue = new ArrayBlockingQueue<>(256);
    private ExecutorService newSingleThreadExecutor;

    private static final String TRANSLATOR_WEBSOCKET_ENDPOINT = "wss://dev.microsofttranslator.com/speech/translate?features=partial&";
    private Session sessionForSoundDataUploadWebSocketServerEndpoint;
    private Session translatorSession;
    private boolean isRunning = false;

    /**
     * Enable Microsoft Translator Sender 
     * @param from: From Language (ex. : en-US)
     * @param to: To Language (ex.: ja-JP)
     * @param sessionForSoundDataUploadWebSocketServerEndpoint WebSocket Session of SoundDataUploadWebSocketServerEndpoint (not TranslatorWebSockerClientEndpoint)
     * @throws IOException
     * @throws URISyntaxException
     * @throws DeploymentException 
     */
    public void enable(String from, String to, Session sessionForSoundDataUploadWebSocketServerEndpoint) throws IOException, URISyntaxException, DeploymentException {
        StringBuilder strBuilder = new StringBuilder();
        String microsoftTranslatorURI = strBuilder.append(TRANSLATOR_WEBSOCKET_ENDPOINT)
                .append("from=")
                .append(from)
                .append("&to=")
                .append(to)
                .append("&api-version=1.0")
                .toString();

        URI serverEndpointUri = new URI(microsoftTranslatorURI);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        this.translatorSession = container.connectToServer(new TranslatorWebSockerClientEndpoint(sessionForSoundDataUploadWebSocketServerEndpoint), serverEndpointUri);
        isRunning = true;

        SoundUtil soundUtil = new SoundUtil();
        byte[] header = soundUtil.createWAVHeaderForInfinite16KMonoSound();
        sendToMicrosoftTranslator(ByteBuffer.wrap(header));
        checkQueue();
    }

    public void disable() throws IOException {
        isRunning = false;
        sessionForSoundDataUploadWebSocketServerEndpoint.close();
        translatorSession.close();
        this.sessionForSoundDataUploadWebSocketServerEndpoint = null;
        newSingleThreadExecutor.shutdown();
        System.out.println("EJB END");
    }

    public void receivedBytes(byte[] soundData) {
        audioQueue.add(soundData);
    }

    public void sendToMicrosoftTranslator(ByteBuffer byteBuff) throws IOException {
        translatorSession.getBasicRemote().sendBinary(byteBuff);
    }

    private void checkQueue() {
        System.out.println("EJB checkQueue() START");
        SoundUtil soundUtil = new SoundUtil();

        newSingleThreadExecutor = Executors.newSingleThreadExecutor();
        newSingleThreadExecutor.submit(() -> {
            try {
                while (isRunning) {
                    //Queue にデータが入った場合
                    if (audioQueue.size() > 0) {
                        byte[] audioData = audioQueue.take();
                        sendToMicrosoftTranslator(ByteBuffer.wrap(audioData));
                    } else {
                        Thread.sleep(500);
                    }
                }
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(StreamMSTranslateSender.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Thread Exit");
        });
    }
}
