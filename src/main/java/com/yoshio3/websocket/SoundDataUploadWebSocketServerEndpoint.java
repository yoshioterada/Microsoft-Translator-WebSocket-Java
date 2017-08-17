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
import com.yoshio3.sounds.StreamMSTranslateSender;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author Yoshio Terada
 */
@ServerEndpoint("/uploadSoundWebSocketEndpoint/{from}/{to}")
public class SoundDataUploadWebSocketServerEndpoint {

    private static final Logger LOGGER = Logger.getLogger(SoundDataUploadWebSocketServerEndpoint.class.getPackage().getName());

    @EJB
    StreamMSTranslateSender receiver;

    @OnOpen
    public void onOpen(@PathParam("from") String from, @PathParam("to") String to, Session session) {
        try {
            LOGGER.log(Level.INFO, "SoundDataUploadWebSocketServerEndpoint Open WebSocket Connection");
            receiver.enable(from, to, session);
        } catch (URISyntaxException | DeploymentException | IOException ex) {
            Logger.getLogger(SoundDataUploadWebSocketServerEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    public void onMessage(ByteBuffer message, Session session) throws URISyntaxException, DeploymentException, IOException, InterruptedException, UnsupportedAudioFileException {
        LOGGER.log(Level.FINE, "SoundDataUploadWebSocketServerEndpoint onMessage{0}", message);
        sendSoundDataToMicrosoftTranslator(session, message);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        try {
            receiver.disable();
            LOGGER.log(Level.FINE, "SoundDataUploadWebSocketServerEndpoint Close WebSocket Connection : ", reason.getReasonPhrase());
        } catch (IOException ex) {
            Logger.getLogger(SoundDataUploadWebSocketServerEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        try {
            receiver.disable();
            LOGGER.log(Level.SEVERE, "SoundDataUploadWebSocketServerEndpoint onError()", t);
        } catch (IOException ex) {
            Logger.getLogger(SoundDataUploadWebSocketServerEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendSoundDataToMicrosoftTranslator(Session soundUpSession, ByteBuffer message) throws DeploymentException, IOException, InterruptedException, URISyntaxException {
        LOGGER.log(Level.FINE, "MSG CAPACITY : {0}", message.capacity());
        byte[] originalSoundData = message.array();
        try {
            SoundUtil soundUtil = new SoundUtil();
            byte[] monoSound;
            if (soundUtil.is16KMonoralSound(originalSoundData)) {
                monoSound = soundUtil.trimWAVHeader(originalSoundData);
            } else {
                // Convert 4.41 Khz -> 1.6Khz; Stereo -> Mono;
                monoSound = soundUtil.convertPCMDataFrom41KStereoTo16KMonoralSound(originalSoundData);
            }
            receiver.receivedBytes(monoSound);
        } catch (UnsupportedAudioFileException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
