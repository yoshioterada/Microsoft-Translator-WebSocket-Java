# 2.1 Learn more about Java WebSocket server implementations that receive voice data from the front end

![](https://c1.staticflickr.com/5/4403/36229193860_5d8555b72e.jpg)

In this implementation, the audio data of wav is recorded in the browser every second, and the recorded audio data is sent to the WebSocket server at intervals of 1 second. In this case, the audio data recorded in the browser cannot be sent to the Microsoft Translator, as the sampling data is a stereo sound at 44.1 khz. So we convert the voice data to the 16KHz with mono sound on the server side. It also stores the converted data in a FIFO queue and sends the queued data to Microsoft Translator sequentially.

***Note:  
The above is described from the browser, the WebSocket server to be implemented this time can be connected from any WebSocket client.
Implementation of WebSocket server for retrieving voice data from*** 

## 1.websocket clients

The WebSocket server that you implement will specify the source language (from) and the destination language (to) in the path of the URL and can invoke.

For example of the URI to connect WebSocket endpoint is follows.

```
ws://HOST-NAME:PORT-NUMBER/CONTEXT-PATH
  /uploadSoundWebSocketEndpoint/en-US/ja-JP
```

In order to use the above URL, I created [SoundDataUploadWebSocketServerEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/SoundDataUploadWebSocketServerEndpoint.java) class (Server Endpoint).

```
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
```

The OnOpen () method is invoked when WebSocket Server is connected from WebSocket Client (browser in this implementation). At this time, the OnOpen() method is connected to Microsoft Translator.


![](https://c1.staticflickr.com/5/4405/36579348856_10d4aae9b2.jpg)

The process flow is as follows.

```
WebSocket Client(Browser) -> 
WebSocket Server(SoundDataUploadWebSocketServerEndpoint) -> 
Microsoft Translator(TranslatorWebSockerClientEndpoint)
```

Then, when the wav voice data is sent from the WebSocket Client to WebSocket Server, the OnMessage () method is invoked. After that it call the sendSoundDataToMicrosoftTranslator() method.

In the sendSoundDataToMicrosoftTranslator() method, it determines whether the audio data is a mono sound with sampling frequency is 16KHz or not. If it is different, converts the data, and then stores it in Blockingqueue. The byte[] in the Blockingqueue contains only the PCM audio data that is removed from the wav header.

***This time, we use the Blockingqueue as a queue to take out the FIFO, but you can use the messaging provider, such as MQ, to use the external queue service.***

```
public class StreamMSTranslateSender {

    private final BlockingQueue<byte[]> audioQueue = new ArrayBlockingQueue<>(256);


    public void receivedBytes(byte[] soundData) {
        audioQueue.add(soundData);
    }
```

The voice data stored in the queue is monitored with the [StreamMSTranslateSender](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/sounds/StreamMSTranslateSender.java)#checkqueue() method.
If there is data in the queue, sendToMicrosoftTranslator(Bytebuffer.wrap(audiodata)) was invoked and sends data to the Microsoft Translator.

```
    private ExecutorService newSingleThreadExecutor;
    
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
```

***Attention:  
This time, I wrote "newSingleThreadExecutor = Executors.newSingleThreadExecutor()" to create a thread. As you know, if you are familiar with Enterprise Java, this method is generated as a thread outside of application server management and is a deprecated implementation method in a Java EE environment. In a production environment, Executorservice should be obtained from Managedexecutorservice as below.***


```
@Resource(lookup = "concurrent/__defaultManagedExecutorService")
private ManagedExecutorService executor;
```

## 2. Send translation results to client

![Response-From-Translator](https://c1.staticflickr.com/5/4347/36456812462_81bd1f00bb.jpg)

When a translation result is returned from the Microsoft Translator to [TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java), the result is returned to the WebSocket Client (this time the browser) in the [TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java)#onMessage() method immediately. The source code of the corresponding part is extracted below.

```                
@ClientEndpoint(
        configurator = TranslatorWebSocketClientEndpointConfigurator.class,
        decoders = {MessageDecoder.class})
public class TranslatorWebSockerClientEndpoint {

    private Session soundUpWebSocketSession;

    public TranslatorWebSockerClientEndpoint(Session soundUpWebSocketSession) {
        this.soundUpWebSocketSession = soundUpWebSocketSession;
    }

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
```

The onMessage() method is invoked when the result of the translation is returned from the Microsoft Translator. Here we generate JSON data to reply to the WebSocket Client and reply immediately. The WebSocket Client is actually replying to the following.

```
soundUpWebSocketSession.getBasicRemote().sendText(jsonObj.toString());
```
