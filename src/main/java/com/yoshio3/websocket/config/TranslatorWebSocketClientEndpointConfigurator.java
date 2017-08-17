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
package com.yoshio3.websocket.config;

import com.yoshio3.auth.AuthTokenService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.HandshakeResponse;

/**
 *
 * @author Yoshio Terada
 */
public class TranslatorWebSocketClientEndpointConfigurator extends ClientEndpointConfig.Configurator{
    private static final Logger LOGGER = Logger.getLogger(TranslatorWebSocketClientEndpointConfigurator.class.getName());
    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        AuthTokenService tokenService = new AuthTokenService();
        tokenService.getAccessTokenForTranslator().ifPresent(accessToken -> {
            headers.put("Authorization", Arrays.asList("Bearer " + accessToken));
        });
        String uuid = UUID.randomUUID().toString();
        headers.put("X-ClientTraceId", Arrays.asList(uuid));
        LOGGER.log(Level.INFO, "X-ClientTraceId: {0}", uuid);
    }

    @Override
    public void afterResponse(HandshakeResponse hr) {
        ;
    }    
}
