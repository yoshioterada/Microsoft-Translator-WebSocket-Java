
# 2.2 Microsoft Translator Java 実装の詳細

Microsoft Translator へ接続し、音声データの送信、翻訳結果の受信を行うための WebSocket Client を Java で実装します。Microsoft Translator へ接続し、翻訳データの送受信をおこなうために最も重要な実装は 1-4 です。5 以降は、クライアントから音声データを取得する部分の実装で、クライアントの実装に応じて変わる内容です。まずは 1-4 までをご理解ください。


 1. 接続 URL の作成
 2. Authorization ヘッダの作成
 3. X-ClientTraceId: {GUID} ヘッダの作成
 4. 音声データの送信
 5. ブラウザから音声データを取得する WebSocket サーバの実装

![Server-Impl](https://c1.staticflickr.com/5/4342/36625812485_009363be97.jpg)
----

## 1. 接続 URL の作成  

翻訳元の言語 (FROM) と翻訳先の言語 (TO) をメソッドの引数で指定し、様々な言語を選択できるように実装します。[StreamMSTranslateSender#enable() で実装](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/sounds/StreamMSTranslateSender.java)  

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
## 2. Authorization ヘッダの作成  
Authentication Token を取得するためには前述したように、[Authentication Token API for Microsoft Cognitive Services Translator API](http://docs.microsofttranslator.com/oauth-token.html) に記載されている方法で取得します。

### 2.1 Subscription Key の取得
Azure の管理ポータルから新規に Cognitive Service -> Microsoft Translator を作成します。次に作成した Microsoft Translator から接続用のアクセス・キー (SUBSCRIPTION_KEY) を取得してください。


![Image](https://c1.staticflickr.com/5/4385/36229070970_a746117f9b.jpg)

"RESOURCE MANAGEMENT" の "Kyes" から取得します。

![Image](https://c1.staticflickr.com/5/4368/36487708461_0de0b4ae79.jpg)

### 2.2 Access Token の取得

次に、JAX-RS Client API を利用し下記の URL に接続します。  

https://api.cognitive.microsoft.com/sts/v1.0/issueToken  

接続の際、ヘッダ Ocp-Apim-Subscription-Key: に取得した Subscription Key を指定して送信します。  JAX-RS Client で接続する場合下記のようなコードになります。  
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

## 3. X-ClientTraceId: {GUID} ヘッダの作成

次に X-ClientTranceId ヘッダを作成します。こちらは UUID の値を作成し設定します。

```
String uuid = UUID.randomUUID().toString();
```


## 4. 音声データの送信

上記 1-3 で、Microsoft Translator に対して接続するために必要な URL, Authentication Token, X-ClientTraceId 情報が揃いました。

Microsoft Translator に接続するための WebSocket クライアントを下記のように実装します。

### 4.1 WebSocket クライアントの実装

Microsoft Translator の WebSocket サーバ・エンドポイントに接続するための、WebSocket クライアントを [TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java) で実装します。

WebSocket サーバ・エンドポイント (Microsoft Translator) に接続する際、ヘッダに認証情報(アクセス・トークン)や X-ClientTraceId を付加します。ヘッダの作成は [TranslatorWebSocketClientEndpointConfigurator](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/config/TranslatorWebSocketClientEndpointConfigurator.java) クラスで実装します。  

また、WebSocket サーバ・エンドポイントからメッセージを受信した際 (翻訳結果を JSON で受信)、受信した JSON データを Java オブジェクトにデコード (マッピング) するために、デコード用の処理を [MessageDecoder](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/message/MessageDecoder.java) で実装します。

実際に返却される JSON のデータ

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

今回、下記のデータは、私の実装するプログラム上利用しないため、マッピングするオブジェクト [MessageDataFromTranslator](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/message/MessageDataFromTranslator.java) 内には含めていません。(必要な場合、追加でフィールド、Setter, Getter を定義してください。)  

|項目|説明|
|----|----|
|audioTimeOffset|Time offset of the start of the recognition in ticks (1 tick = 100 nanoseconds). The offset is relative to the beginning of streaming.|
|audioTimeSize|Duration in ticks (100 nanoseconds) of the recognition.|
|audioStreamPosition|Byte offset of the start of the recognition. The offset is relative to the beginning of the stream.|
|audioSizeBytes|Size in bytes of the recognition.  |


上記デコーダを実装することで、onMessage() のメソッドで、サーバ・エンドポイントからメッセージを受信した際、デコードされた [MessageDataFromTranslator](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/message/MessageDataFromTranslator.java) オブジェクトを操作できるようになります。   

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



### 4.2 Microsoft Translator への接続

上記までの実装で Microsoft Translator へ接続するための準備が整いました。それでは、実際に Translator へ接続するためのコードを実装します。実際の接続用コードは [StreamMSTranslateSender](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/sounds/StreamMSTranslateSender.java)#enable() メソッドで実装していますので、詳しくはソースコードをご参照ください。  

特に重要な箇所を下記に抜粋します。  
上記で作成した [TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java) をクライアント・エンドポイントとしてサーバに接続するためのコードを下記に記述します。

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

サーバとの接続箇所は下記です。指定したサーバの URL (serverEndpointUri) に対して、WebSocket Client の実装 ([TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java)) を指定して接続しています。

```
container.connectToServer(new TranslatorWebSockerClientEndpoint(session),  
   serverEndpointUri);
```

***注意:  
TranslatorWebSockerClientEndpoint (session) でコンストラクタに session を指定していますが、これは、後ほど実装する別の WebSocket セッションです (ブラウザ等のクライアントと連携するための実装 [SoundDataUploadWebSocketServerEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/SoundDataUploadWebSocketServerEndpoint.java) )。単一の Java アプリケーションから Microsoft Translator と接続する場合は、この session は不要です。***


### 4.3 Microsoft Translator へ音声データの送信

上記で、Microsoft Translator と接続が正常に完了すると、返り値として Session (translatorSession) が生成されます。

```
this.translatorSession = container.connectToServer(new TranslatorWebSockerClientEndpoint(session), serverEndpointUri);
```

このセッションに対して、下記のように sendBinary() を呼び出すことで、Microsoft Translator に対して音声データをバイナリで送信できます。


```
public void sendToMicrosoftTranslator(ByteBuffer byteBuff) throws IOException {
    translatorSession.getBasicRemote().sendBinary(byteBuff);
}
```

### 4.4 ストリーミングデータとして送信

上記の sendToMicrosoftTranslator() メソッドに対して、wav ファイルのバイナリデータを渡すことで、Translator に音声を送信することができるようになりました。しかし wav ファイルを個別に送信する場合は、問題が発生する場合があります。

たとえば、10秒の音声データ(320Kbyte) A と、2秒の音声データ(64Kbyte) B があり、A -> B と送信順に意味のあるデータの場合を考えてみましょう。A のデータが完全に送信完了した後に、B を送信すれば問題ないのですが、A を転送している最中に B の送信が始まった場合、B のデータの方がデータサイズが小さいため、先に Translator に到着し、先に B の結果が返ってくる可能性があります。(これを避けるためには順序を保証する実装が別途必要になるかもしれません)

このような事をさけるためには、wav ファイルを都度送信するのではなく、連続したストリーム・データとして送信したい場合があります。このようなニーズに対応するため、Microsoft Translator では wav ヘッダ・ファイルで、サイズ 0 を指定したデータを扱えるようになっています。

この wav ヘッダを一度送信した後は、音声データ部分だけ (wavヘッダなし) を都度アップできるようになります。この専用の wav ヘッダを作成するために [SoundUtil](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/sounds/SoundUtil.java) クラスでメソッドを実装しています。


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

[StreamMSTranslateSender](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/sounds/StreamMSTranslateSender.java)#enable() メソッドで、Microsoft Translator に接続後に一度だけ呼び出して(専用 wav ヘッダを作成し)送信しています。

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


### 4.5 サンプリング周波数とモノラル・サウンドへの変換
Java で音声データを操作するためには、Java Sound API を利用します。Sound API を利用すると、サンプリング周波数やチャネル等をかんたんに確認したり変更できます。  
サンプリング周波数が 16KHz で、モノラルサウンドか否かを確認するのは、is16KMonoralSound() メソッドで実装しています。また、サンプリング周波数を 44.1 KHz から 16 Khz 、ステレオからモノラル・サウンドに変換している箇所は、convertedByte41KStereoTo16KMonoralSound() メソッドで実装しています。


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

上記が、基本的な Microsoft Translator への接続し、翻訳結果を受信する部分までの実装になります。

ここから先の実装は、クライアントの実装方法に応じて異なる内容になります。仮にJavaFX や .Net などのスタンドアローンのアプリケーションとして実装する場合、別途、そのスタンドアローン実装内で、音声データをどのように扱うかをご検討ください。

[Part2 へと続く](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/ExplanationOfImplebyJava-2.md)