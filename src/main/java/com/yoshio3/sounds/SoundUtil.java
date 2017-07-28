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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Microsoft Translator will receive the Sound Data as follows.
 * 
 *  In order to convert the sound data, this class will be used.
 * 
 *  SamplRate   : 16KHz
 *  Bit/Sample  : 16
 *  Chanel(mono): 1
 * 
 * @author Yoshio Terada
 */
public class SoundUtil {
    
    private final static int FORMAT_CHUNK_SIZE = 16;
    private final static int FORMAT = 1;
    private final static int CHANNEL = 1; //mono:1 stereo:2
    private final static int SAMPLERATE = 16000;
    private final static int BIT_PER_SAMPLE = 16;
    
    /**
     * Convert the Sampling Data, Bit/Sample, Mono Channel
     * 
     * @param soundBinary original sound binary data
     * @return converted binary sound data
     * @throws UnsupportedAudioFileException
     * @throws IOException 
     */

    public byte[] convertMonoralSound(byte[] soundBinary) throws UnsupportedAudioFileException, IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(soundBinary);
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bais);
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(16000, 16, 1, true, false);

        if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = audioInputStream.read(bytes, 0, bytes.length)) != -1) {
                try {
                    bout.write(bytes, 0, bytesRead);
                } catch (Exception ex) {
                    Logger.getLogger(SoundUtil.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            byte[] soundData = bout.toByteArray();
            bout.close();
            return createNewSoundBinaryData(soundData);
        }else{
            return null;
        }
    }

    private byte[] createNewSoundBinaryData(byte[] soundData) throws IOException {
        int totalFileSize = soundData.length + 44;
        int chunk = totalFileSize - 8;
        int dataSize = totalFileSize - 126;

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        bOut.write("RIFF".getBytes()); // RIFF Header
        bOut.write(getIntByteArray(chunk)); //Total File Size(datasize + 44) - 8;
        bOut.write("WAVE".getBytes()); //WAVE Header
        bOut.write("fmt ".getBytes()); //FORMAT Header
        bOut.write(getIntByteArray(FORMAT_CHUNK_SIZE));
        bOut.write(getFloatByteArray(FORMAT));
        bOut.write(getFloatByteArray(CHANNEL));
        bOut.write(getIntByteArray(SAMPLERATE));
//        int ByteRate = SAMPLERATE * CHANNEL * BIT_PER_SAMPLE / 2;
        int ByteRate = 32000;
        bOut.write(getIntByteArray(ByteRate));
        int BlockSize = (int) CHANNEL * BIT_PER_SAMPLE / 8;
        bOut.write(getFloatByteArray(BlockSize));
        bOut.write(getFloatByteArray(BIT_PER_SAMPLE));
        bOut.write("data".getBytes()); // Data Header
        bOut.write(getIntByteArray(dataSize));
        bOut.write(soundData);

        return bOut.toByteArray();
    }

    private byte[] getIntByteArray(int intValue) {
        byte[] array = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(intValue).array();
        return array;
    }

    private byte[] getFloatByteArray(int data) {
        byte[] array = ByteBuffer
                .allocate(2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putChar((char) data)
                .array();
        return array;
    }

    /*
    public static Optional<Path> getAppropriateSoundFormatData(byte[] bytes) throws IOException, UnsupportedAudioFileException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        AudioFileFormat format = AudioSystem.getAudioFileFormat(bais);

        int frameLength = format.getFrameLength();
        AudioFormat sourceFormat = format.getFormat();

        AudioInputStream sourceStream = new AudioInputStream(bais, sourceFormat, frameLength);
        AudioFormat targetFormat = new AudioFormat(16000, 16, 1, false, true);

        if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            AudioFormat newFormat = audioInputStream.getFormat();

            String fileName = UUID.randomUUID().toString() + ".wav";
            Path path = Paths.get("/tmp", fileName);
            File file = path.toFile();
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file);
            return Optional.of(path);
        }
        return Optional.empty();
    }*/
}
