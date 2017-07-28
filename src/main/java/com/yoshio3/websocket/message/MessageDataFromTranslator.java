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

import java.io.Serializable;

/**
 *
 * @author Yoshio Terada
 */
public class MessageDataFromTranslator  implements Serializable{
    /*
    "type":"final",
    "id":"0",
    "recognition":"今日は火曜日です。",
    "translation":"Today is Tuesday."
    */
    
    private String type;
    private String id;
    private String recognition;
    private String translation;

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the recognition
     */
    public String getRecognition() {
        return recognition;
    }

    /**
     * @param recognition the recognition to set
     */
    public void setRecognition(String recognition) {
        this.recognition = recognition;
    }

    /**
     * @return the translation
     */
    public String getTranslation() {
        return translation;
    }

    /**
     * @param translation the translation to set
     */
    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @Override
    public String toString() {
        return "Message{" + "type=" + type + ", id=" + id + ", recognition=" + recognition + ", translation=" + translation + '}';
    }  
}
