
# 2.1 フロント・エンドから音声データを受け取る Java WebSocket サーバ実装の詳細

![](https://c1.staticflickr.com/5/4403/36229193860_5d8555b72e.jpg)

今回の実装では、ブラウザで wav の音声データを 1秒毎に録音し、録音された音声データを WebSocket サーバに 1 秒間隔で送信しています。この際、ブラウザで録音した音声データは、サンプリングデータが 44.1kHz でステレオ・サウンドであるため、そのままでは Microsoft Translator に送信できません。そこで、サーバ側で音声データを 16KHz のモノラル・サウンドに変換します。さらに変換したデータを FIFO のキューに格納し、キューに格納されたデータを順に Microsoft Translator に送信しています。

***ご注意：
上記ではブラウザからと記述していますが、今回実装する WebSocket サーバは任意の WebSocket クライアントから接続ができます。***


##1.WebSocket クライアントから音声データを取得する WebSocket サーバの実装

実装する WebSocket サーバは、URL のパスで翻訳元の言語(FROM)、と翻訳先の言語(TO)を指定し呼び出しを行います。

WebSocket エンドポイントの接続先 URI 例  

```
ws://HOST-NAME:PORT-NUMBER/CONTEXT-PATH
  /uploadSoundWebSocketEndpoint/en-US/ja-JP
```

上記の、接続先 URI で接続できるように [SoundDataUploadWebSocketServerEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/SoundDataUploadWebSocketServerEndpoint.java) クラスを作成し、@ServerEndpoint アノテーションでパスを記載します。

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

WebSocket Server は WebSocket Client (本実装ではブラウザ) から接続された際に、onOpen()メソッドが呼び出されます。この時 onOpen() メソッドで Microsoft Translator まで一気に接続しています。

![](https://c1.staticflickr.com/5/4405/36579348856_10d4aae9b2.jpg)

処理フローは下記の通りです。

```
WebSocket Client(Browser) -> 
WebSocket Server(SoundDataUploadWebSocketServerEndpoint) -> 
Microsoft Translator(TranslatorWebSockerClientEndpoint)
```

その後、WebSocket Client から wav 音声データが WebSocket Server に送信されると onMessage() メソッドが呼び出され、sendSoundDataToMicrosoftTranslator() を呼び出しています。

sendSoundDataToMicrosoftTranslator()メソッドは、Client から送信された音声データが、サンプリング周波数 16KHz でモノラル・サウンドか否かを判定し、違う場合はデータを変換した後、BlockingQueue に格納します。格納する byte[] は wav ヘッダを取り除いた PCM の音声データのみです。

***今回、FIFO で取り出す Queue としてBlockingQueue を利用していますが、MQ などのメッセージング・プロバイダを利用しても、外部の Queue サービスを利用しても構いません。***

```
public class StreamMSTranslateSender {

    private final BlockingQueue<byte[]> audioQueue = new ArrayBlockingQueue<>(256);


    public void receivedBytes(byte[] soundData) {
        audioQueue.add(soundData);
    }
```

Queue に格納された 音声データは、[StreamMSTranslateSender](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/sounds/StreamMSTranslateSender.java)#checkQueue() メソッドで監視し、Queue にデータが存在する場合、sendToMicrosoftTranslator(ByteBuffer.wrap(audioData)) で Microsoft Translator にデータを送信します。

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

***ご注意  
今回、newSingleThreadExecutor = Executors.newSingleThreadExecutor() でスレッドを生成しています。Enterprise Java に精通している方はご存知の通り、この方法はアプリケーション・サーバ管理外のスレッドとして生成され、Java EE 環境では非推奨の実装方法です。本番環境では、ExecutorService は下記のように ManagedExecutorService から取得してください。***

```
@Resource(lookup = "concurrent/__defaultManagedExecutorService")
private ManagedExecutorService executor;
```

また、何らかのエラー発生時、クローズリクエストを受け付けた場合、それぞれ onError(), onClose() メソッドが呼び出され、Queue チェックを行うスレッドを停止するためのメソッドが呼び出されます。

```
receiver.disable();
```

##2.クライアントに翻訳結果を送信

![Response-From-Translator](https://c1.staticflickr.com/5/4347/36456812462_81bd1f00bb.jpg)

Microsoft Translator から [TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java) に対して翻訳結果が返ってきた際、[TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java)#onMessage() メソッド内で、ただちに WebSocket Client (今回はブラウザ) に結果を返しています。下記に該当部分のソースコードを抜粋します。

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

Microsoft Translator から翻訳結果が返ってくると、onMessage() メソッドが呼び出されます。ここで WebSocket Client に返信するための JSON データを生成し、ただちに返信します。実際に WebSocket Client に返信している箇所は下記です。

```
soundUpWebSocketSession.getBasicRemote().sendText(jsonObj.toString());
```
