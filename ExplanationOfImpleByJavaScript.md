
# 1 フロントエンド実装：音声データの作成、結果表示

![](https://c1.staticflickr.com/5/4332/36330464070_5333b0b12e_z.jpg)

WAV の音声データを作成し、WebSocket を通じてアップロードをする、クライアントのアプリケーションを作成します。WebSocket のサーバに対しては任意の WebSocket クライアントで実装できますが、ここでは HTML + JavaScript でクライアントを実装する例を紹介します。

## 1.1 利用する JavaScript ライブラリ

HTML + JavaScript で wav 音声を録音するために、WebRTC [getUserMedia](http://caniuse.com/#feat=stream) と [Web Audio](http://caniuse.com/#feat=audio-api) を利用する方法などがありますが、今回、私は実装を簡単にするために、[MediaStreamRecorder.js](https://github.com/streamproc/MediaStreamRecorder) のライブラリを利用して実装する事にしました。

## 1.2 音声録音の動作説明

下記の [GUI](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/webapp/index.html) を作成します。FROM, TO でそれぞれ、翻訳元、翻訳先の言語を選択し、"START"ボタンを押下すると、音声を録音し、録音したデータを WebSocket でサーバに送信します。"STOP"を押下すると録音を停止し、WebSocket 通信も終了します。

![UI](https://c1.staticflickr.com/5/4379/36587385061_b42082aa74.jpg)


## 1.3 音声録音の実装説明

録音の実装は、[index.html](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/webapp/index.html) で実装しています。全コードは[ソースコード](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/webapp/index.html)をご参照ください。

このアプリケーションは、"START" ボタンを押下すると、下記のメソッドを呼び出します。

```
document.querySelector('#start-recording').onclick = function () {
    initWebSocket();
    this.disabled = true;
    captureUserMedia(mediaConstraints, onMediaSuccess, onMediaError);
};
```

具体的には、START ボタンを押下した時に initWebSocket() を呼び出し、WebSocket サーバとの接続を開始します。

```
function initWebSocket() {
    if (websocket !== null) {
        return;
    }
    var wsUri = "ws://"
            + document.location.hostname + ":"
            + document.location.port
            + document.location.pathname
            + 'uploadSoundWebSocketEndpoint/'
            + document.querySelector('#from-lang').value + '/'
            + document.querySelector('#to-lang').value;

    websocket = new WebSocket(wsUri);
    websocket.binaryType = 'arraybuffer';
    websocket.onconnect = function (e) {
        console.log('connect:' + e.msg);
    };
    websocket.onerror = function (e) {
        console.log(e.msg);
    };
    websocket.onclose = function (e) {
        console.log('close:' + e.msg);
    };
    websocket.onmessage = function (e) {
        //翻訳結果を画面に出力
        writeToScreen(e);
    };
}
```

WebSocket サーバとの通信を開始した後に、captureUserMedia() メソッド内で getUserMedia を呼び出し録音を開始します。

```
function captureUserMedia(mediaConstraints, 
                            successCallback, 
                            errorCallback) {
    navigator
        .mediaDevices
        .getUserMedia(mediaConstraints)
        .then(successCallback)
        .catch(errorCallback);
}
```

onMediaSuccess() で 1秒間隔で wav の音声データ(サンプリング 44.1KHz, ステレオ, 16bit)を作成しています。  
mediaRecorder.ondataavailable でデータが利用可能になった時に ***websocket.send(blob);*** を呼び出し、録音データを WebSocket サーバ・エンドポイントに送信しています。

```
var timeInterval = 1000;

function onMediaSuccess(stream) {
    var audio = document.createElement('audio');
    audio = mergeProps(audio, {
        controls: true,
        muted: true,
        src: URL.createObjectURL(stream)
    });
    audio.play();
    mediaRecorder = new MediaStreamRecorder(stream);
    mediaRecorder.stream = stream;
    mediaRecorder.recorderType = StereoAudioRecorder;
    mediaRecorder.mimeType = 'audio/wav';
    // don't force any mimeType; use above "recorderType" instead.
    // mediaRecorder.mimeType = 'audio/webm'; // audio/ogg or audio/wav or audio/webm
    mediaRecorder.audioChannels = !!document.getElementById('left-channel').checked ? 1 : 2;
    mediaRecorder.ondataavailable = function (blob) {
        // Upload File to Server
        websocket.send(blob);
    };

    // get blob after specific time interval
    mediaRecorder.start(timeInterval);
    document.querySelector('#stop-recording').disabled = false;
}
```

WebSocket サーバ・エンドポイントから翻訳結果の JSON を受信した場合、websocket.onmessage が呼び出されます。このメソッド内で writeToScreen() を呼び出し結果を画面に表示しています。

```
function initWebSocket() {
    //... Continue

    websocket.onmessage = function (e) {
        //翻訳結果を画面に出力
        writeToScreen(e);
    };
}
```

writeToScreen() メソッドでは、WebSocket サーバ・エンドポイントから受信した、JSON を解析し、画面出力行っています。  
Microsoft Translator から返ってくる、type には "partial" と "final" の２種類があります。

Microsoft Translator から返信されるデータの例：

```
情報:   MS-Translator : Message{type=partial, id=0.1, recognition=we, translation=私たち} 
情報:   MS-Translator : Message{type=final, id=0, recognition=We want to., translation=私たちがしたい。} 
情報:   MS-Translator : Message{type=partial, id=1.3, recognition=in microsoft hold, translation=マイクロソフトホールド} 
情報:   MS-Translator : Message{type=final, id=1, recognition=In Microsoft hold you back., translation=マイクロソフトでは、バックを保持します。}
```

ここで、"partial" は部分翻訳を意味し、"final" は翻訳結果を表します。  
Microsoft Translator から返信される上記の JSON のデータは、下記のようにカスタマイズしてブラウザに返信します。

ブラウザに返信する JSON データ

```
{"type":"final","origin":"In Microsoft hold you back."
 ,"translated":"マイクロソフトでは、バックを保持します。"}
```

ブラウザに返信する JSON データでは、翻訳元のテキストデータを origin に、翻訳した結果のテキストデータを translated に代入しています。  

実装では、返信されるデータの type を判定し、部分翻訳の結果 (partial) と最終的な翻訳結果 (final) を違う場所に書き出すようにしています。

partial は画面上部に上書きします、final は最新の翻訳結果が一番先頭行に記載されるように追記していくよう実装しています。

```
function writeToScreen(event) {
    var obj = JSON.parse(event.data);
    var element = document.createElement('div');
    element.className = "message";
    element.textContent = obj.origin + ' : ' + obj.translated;
    element.style.backgroundColor = "white";

    if (obj.type === "partial") {
        var objBody = document.getElementById("partial");
        objBody.innerHTML = obj.origin
    } else if (obj.type === "final") {
        var objBody = document.getElementById("insertpos");
        objBody.insertBefore(element, objBody.firstChild);
    }
}
```

最後に、停止ボタンが押下された際に下記を呼び出します。ここでは、録音を停止し WebSocket の接続も閉じています。

```
document.querySelector('#stop-recording').onclick = function () {
    this.disabled = true;
    mediaRecorder.stop();
    mediaRecorder.stream.stop();
    document.querySelector('#start-recording').disabled = false;
    if (websocket !== null) {
        websocket.close();
        websocket = null;
    }
};
```

***Note :  
今回は、ブラウザ上で音声を録音しサーバに送信する事で、音声翻訳を行いました。  
仮に、スタンドアローンのアプリケーションで実装する場合は、アプリケーション内で録音をする際、サンプリング・レート 16KHz, モノラル、16bit で音声データを作成する事で、
[TranslatorWebSockerClientEndpoint](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/java/com/yoshio3/websocket/TranslatorWebSockerClientEndpoint.java) 経由で直接 Microsoft Translator にデータを送信し、翻訳結果を受け取ることができます。  
ご自身が作成するアプリケーションの種類に応じて、適切な実装方法をお選びください。***

