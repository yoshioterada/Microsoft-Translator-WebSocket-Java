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
package com.yoshio3.websocket.message;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

/**
 *
 * @author Yoshio Terada
 */
public class MessageDecoder  implements Decoder.Text<MessageDataFromTranslator>{

    @Override
    public MessageDataFromTranslator decode(String jsonMessage) throws DecodeException {
        JsonObject jsonObject = Json
                .createReader(new StringReader(jsonMessage)).readObject();
        MessageDataFromTranslator message = new MessageDataFromTranslator();
        message.setId(jsonObject.getString("id"));
        message.setType(jsonObject.getString("type"));
        message.setRecognition(jsonObject.getString("recognition"));
        message.setTranslation(jsonObject.getString("translation"));
        return message;
    }

    @Override
    public boolean willDecode(String jsonMessage) {
        return true;
    }

    @Override
    public void init(EndpointConfig config) {
        ;
    }

    @Override
    public void destroy() {
        ;
    }    
}
