# 2.2 Microsoft Translator Java Implementation Details

Java implements the WebSocket Client to connect to Microsoft Translator, send voice data, and receive translation results. The most important implementation to connect to Microsoft Translator and send and receive translation data is 1-4. After 5, the implementation of the part that retrieves the voice data from the client is what changes depending on the client's implementation. Please understand up to 1-4 first.

1. Create a connection URL
2. Creating a Authorization Header
3. X-clienttraceid: {GUID} header creation
4. Send voice data
5. Implementing a WebSocket server to retrieve voice data from a browser


![Server-Impl](https://c1.staticflickr.com/5/4342/36625812485_009363be97.jpg)

## 1. Create a URL for connection

In order to specify the multiple source language (from) and the destination language (to), you can specify them in the method arguments. Implemented by [StreamMSTranslateSender#enable()](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/sounds/StreamMSTranslateSender.java))


```
private static final String TRANSLATOR_WEBSOCKET_ENDPOINT = "wss://dev.microsofttranslator.com/speech/translate?features=partial&";

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
    // Continue ... 
}
```  
## 2. Create a Authorization header

To obtain the Authentication token, you get the method described in the Authentication token API for Microsoft Cognitive Services Translator API as described earlier.
### 2.1 Getting Subscription Key

Create a new Cognitive Service-> Microsoft Translator from the Azure management portal. Get the Connection access key (Subscription_key) from the next Microsoft Translator you created.

![Image](https://c1.staticflickr.com/5/4385/36229070970_a746117f9b.jpg)

Then please select "Kyes" from "RESOURCE MANAGEMENT". You will see the following screen when you select.

![Image](https://c1.staticflickr.com/5/4368/36487708461_0de0b4ae79.jpg)


### 2.2 Getting Access Tokens

Next,YOu connect to the following URL by usin JAX-RS Client API:

```
https://api.cognitive.microsoft.com/sts/v1.0/issueToken
```

In order connect the server, you must specify the Access Token with "Ocp-Apim-Subscription-Key:" header.  

For example, You will write the following code with JAX-RS Client. 
[AuthTokenService で実装](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/auth/AuthTokenService.java)

```
public class AuthTokenService {

    private final static String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    private final static String AUTH_URL = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";
    private final static String SUBSCRIPTION_KEY;

    static {
        SUBSCRIPTION_KEY = PropertyReader.getPropertyValue("SUBSCRIPTION_KEY");
    }

    public Optional<String> getAccessTokenForTranslator() {
        Client client = ClientBuilder.newBuilder()
                .register(JacksonFeature.class)
                .build();
        Entity<String> entity = Entity.entity("", MediaType.TEXT_PLAIN_TYPE);
        Response response = client.target(AUTH_URL)
                .request()
                .header(OCP_APIM_SUBSCRIPTION_KEY, SUBSCRIPTION_KEY)
                .post(entity);
        if (isRequestSuccess(response)) {
            return Optional.of(response.readEntity(String.class));
        } else {
            return Optional.empty();
        }
    }

    private boolean isRequestSuccess(Response response) {
        Response.StatusType statusInfo = response.getStatusInfo();
        Response.Status.Family family = statusInfo.getFamily();
        return family != null && family == Response.Status.Family.SUCCESSFUL;
    }
}
```

## 3. Create a X-clienttraceid: {GUID} header

Next, Please create a X-ClientTranceId header. This creates and sets the UUID value.

```
String uuid = UUID.randomUUID().toString();
```

## 4. Sending voice data

In the 1-3 above, the URL, Authentication Token, and X-clienttraceid information needed to connect to the Microsoft Translator are aligned.

Please implement the WebSocket client to connect to the Microsoft Translator as follows:

### 4.1 WebSocket Client Implementation

Implements the WebSocket client Class is [TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java) which connect to the WebSocket server endpoint of the Microsoft Translator.

When connecting to the WebSocket server endpoint (Microsoft Translator), the header is appended with the authentication information (access token) and X-clienttraceid. The header creation is implemented in the [TranslatorWebSocketClientEndpointConfigurator](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/config/TranslatorWebSocketClientEndpointConfigurator.java) class.

In addition, when receiving a message from the WebSocket server endpoint (receive the translation result in JSON), the decoding process is implemented in [MessageDecoder](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/message/MessageDecoder.java) to decode (map) the received JSON data to a Java object.

JSON data that is actually returned

```
{
  type: "final"  
  id: "23",  
  recognition: "what was said", 
  translation: "translation of what was said",
  audioStreamPosition: 319680,
  audioSizeBytes: 35840,
  audioTimeOffset: 2731600000,
  audioTimeSize: 21900000
}
```

Since the following data is not available on my program, it is not included in the object [MessageDataFromTranslator](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/message/MessageDataFromTranslator.java) to be mapped.   
(If you need them, please define additional fields, setters and getters to the [MessageDataFromTranslator](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/message/MessageDataFromTranslator.java).) 

|Item| description|
|---|---|
|Audiotimeoffset| Time offset of the start of the recognition in ticks (1 tick = 100 nanoseconds). The offset is relative to the beginning of streaming.|
|Audiotimesize| Duration in Ticks (100 nanoseconds) of the recognition.
|Audiostreamposition| Byte offset of the start of the recognition. The offset is relative to the beginning of the stream.|
|Audiosizebytes| Size in bytes of the recognition.|

By implementing the above decoder, the OnMessage () method allows you to manipulate the decoded [MessageDataFromTranslator](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/message/MessageDataFromTranslator.java) object when you receive a message from the server endpoint.


```
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

### 4.2 Connecting to Microsoft Translator

You are ready to connect to the Microsoft Translator with the above implementation. So, we actually implement the code to connect to Translator. The actual connection code is implemented in the [StreamMSTranslateSender](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/sounds/StreamMSTranslateSender.java)#enable () method, so please refer to the source code for more information.

Here are some of the most important points.
The code to connect the sever, I created the following code on [TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java).

```
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
        this.translatorSession = container.connectToServer(new TranslatorWebSockerClientEndpoint(session), serverEndpointUri);
```

The connection point with the server is below. The WebSocket Client Implementation ([TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java)) is connected to the specified server URL (serverEndpointUri).

```
container.connectToServer(new TranslatorWebSockerClientEndpoint(session),  
   serverEndpointUri);
```

***Attention:
Translatorwebsockerclientendpoint (session) specifies a session in the constructor, which is another WebSocket session to be implemented later (Implementation [SoundDataUploadWebSocketServerEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/SoundDataUploadWebSocketServerEndpoint.java) to work with clients such as browsers). This session is not required if you are connecting to a Microsoft Translator from a single Java application.***

###4.3 Sending audio data to Microsoft Translator

In the above, when the Microsoft Translator and the connection are completed successfully, the return value is generated as a Session (translatorsession).

```
this.translatorSession = container.connectToServer(new TranslatorWebSockerClientEndpoint(session), serverEndpointUri);
```

For this session, you can send the audio data in binary to the Microsoft Translator by calling sendBinary() method as follows:

```
public void sendToMicrosoftTranslator(ByteBuffer byteBuff) throws IOException {
    translatorSession.getBasicRemote().sendBinary(byteBuff);
}
```


### 4.4 Send As streaming data

You can now send audio to Microsoft Translator by passing binary data from the wav file to the above sendToMicrosoftTranslator() method. However, if you are sending wav files individually, you may experience problems.

For example, suppose you have a 10-second audio data (320kbyte) A and a two-second audio data (64k) B, and a-> B and a meaningful data in the order in which they are sent. If you send B after the data of a is complete, it is not a problem. If B's transmission begins while transferring a, the data in B may be smaller, so you may arrive at the Translator first, and the results of B will be returned first. (in order to avoid this, you may need to implement a separate sequence.)

In order to avoid this, you may want to send the wav files as consecutive stream data instead of sending them each time. To address these needs, you can send a wav header with a size of 0 to Translator.

Once you have sent this wav header, you will be able to upload only the audio data portion (without the wav header) each time. I implemented these functionality on Soundutil class.

```
public class SoundUtil {

    public byte[] createWAVHeaderForInfinite16KMonoSound() throws IOException {
        int chunk = 0;
        int dataSize = 0;
        // Please refer to WAV format. Following is Japanese explanation.
        // http://www.wdic.org/w/TECH/WAV
        // http://docs.microsofttranslator.com/speech-translate.html
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        bOut.write("RIFF".getBytes()); // RIFF Header
        bOut.write(getIntByteArray(chunk)); //Total File Size(datasize + 44) - 8;
        bOut.write("WAVE".getBytes()); //WAVE Header
        bOut.write("fmt ".getBytes()); //FORMAT Header
        bOut.write(getIntByteArray(FORMAT_CHUNK_SIZE));
        bOut.write(getFloatByteArray(FORMAT));
        bOut.write(getFloatByteArray(CHANNEL));
        bOut.write(getIntByteArray(SAMPLERATE));
        int ByteRate = SAMPLERATE * 2 * CHANNEL; // Sampling * 2byte * Channel
        bOut.write(getIntByteArray(ByteRate));
        int BlockSize = (int) CHANNEL * BIT_PER_SAMPLE / 8;
        bOut.write(getFloatByteArray(BlockSize));
        bOut.write(getFloatByteArray(BIT_PER_SAMPLE));
        bOut.write("data".getBytes()); // Data Header
        bOut.write(getIntByteArray(dataSize));
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
}
```

The 
[StreamMSTranslateSender](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/sounds/StreamMSTranslateSender.java)#enable () method sends only one call after connecting to the Microsoft Translator (creating a dedicated wav header).

```
    public void enable(String from, String to, Session sessionForSoundDataUploadWebSocketServerEndpoint) throws IOException, URISyntaxException, DeploymentException {
        //... Continue
        this.translatorSession = container.connectToServer(new TranslatorWebSockerClientEndpoint(sessionForSoundDataUploadWebSocketServerEndpoint), serverEndpointUri);
        this.sessionForSoundDataUploadWebSocketServerEndpoint = sessionForSoundDataUploadWebSocketServerEndpoint;
        isRunning = true;

        SoundUtil soundUtil = new SoundUtil();
        byte[] header = soundUtil.createWAVHeaderForInfinite16KMonoSound();
        sendToMicrosoftTranslator(ByteBuffer.wrap(header));
```

### 4.5 conversion to sampling frequency and mono sound

To work with voice data in Java, use the Java sound API. Using the Sound API, you can check or change the sampling frequency and channel.
It is implemented in the Is16kmonoralsound () method to determine whether the sampling frequency is 16KHz and whether it is a mono sound. The Convertedbyte41kstereoto16kmonoralsound () method implements the sampling frequency from 44.1 khz to 16 KHz and from stereo to mono sound.


```
public class SoundUtil {
    private final static int CHANNEL = 1; //mono:1 stereo:2
    private final static int SAMPLERATE = 16000;
    private final static int BIT_PER_SAMPLE = 16;

    public boolean is16KMonoralSound(AudioInputStream inStream) {
        long frameLength = inStream.getFrameLength();
        AudioFormat format = inStream.getFormat();

        int sampleSizeInBits = format.getSampleSizeInBits();
        float frameRate = format.getFrameRate();
        int channels = format.getChannels();

        return sampleSizeInBits == BIT_PER_SAMPLE
                && frameRate == SAMPLERATE
                && channels == CHANNEL;
    }


    public byte[] convertPCMDataFrom41KStereoTo16KMonoralSound(byte[] soundBinary) throws UnsupportedAudioFileException, IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(soundBinary);
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bais);
        return convertedByte41KStereoTo16KMonoralSound(sourceStream);
    }

    private byte[] convertedByte41KStereoTo16KMonoralSound(AudioInputStream sourceStream) throws UnsupportedAudioFileException, IOException {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(SAMPLERATE, BIT_PER_SAMPLE, CHANNEL, true, false);
        byte[] soundData;
        if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)) {
                soundData = getByteFromAudioInputStream(audioInputStream);
            }
            return createWAVBinaryDataFor16KMonoSound(soundData);
        } else {
            return getByteFromAudioInputStream(sourceStream);
        }
    }
}
```

The above is the implementation of the basic connection to the Microsoft Translator.

The implementation from this point will be different depending on how the client is implemented. If you are implementing it as a standalone application, such as JavaFX or .Net, you should consider how to handle voice data within that standalone implementation separately.

[Next explanation (Part2)](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/ExplanationOfImplebyJava-2-en.md)
