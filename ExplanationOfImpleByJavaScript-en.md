# 1 Front-end implementation: Create voice data, display results

![](https://c1.staticflickr.com/5/4332/36330464070_5333b0b12e_z.jpg)

Create a client application that creates wav audio data and uploads it through WebSocket Server. It can be implemented on any WebSocket client for WebSocket servers, but here is an example of implementing a client in HTML + JavaScript.

## 1.1 JavaScript libraries usage

There are ways to use WebRTC [getUserMedia](http://caniuse.com/#feat=stream) and [Web Audio](http://caniuse.com/#feat=audio-api) to record WAV audio in HTML + JavaScript, but in this time I decided to implement it using the [MediaStreamRecorder.js](https://github.com/streamproc/MediaStreamRecorder) library to simplify the implementation.


## 1.2 Voice recording Operation description

Create the following [GUI](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/webapp/index.html): "FROM" and "TO" which select the source and target language. If user  press the "Start" button to record the audio and send the recorded data to the WebSocket server. If user press the "Stop" button, it stop the recording and close the WebSocket connection.

![UI](https://c1.staticflickr.com/5/4379/36587385061_b42082aa74.jpg)

## 1.3 Implementation Description of voice recording

Please implemente  [index.html](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/webapp/index.html)? Please refer to the source code.

When the "Start" button is pressed, it calls the following method.

```
document.querySelector('#start-recording').onclick = function () {
    initWebSocket();
    this.disabled = true;
    captureUserMedia(mediaConstraints, onMediaSuccess, onMediaError);
};
```

After invoked the method, it call initWebSocket() method to connect the WebSocket server.


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

After connected with the WebSocket server,it start the recording by invoking the getUserMedia in the captureUserMedia() method.

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

onMediaSuccess() creates wav audio data (In this time, sampling 44.1 khz, stereo, 16bit) at every 1-second as intervals.
When data becomes available in mediarecorder.ondataavailable,
***websocket.send(blob);*** is invoked and send the recorded data to the WebSocket server endpoint.


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

If you receive a translation result JSON from the WebSocket server endpoint, websocket.onmessage is invoked. And it called writeToScreen() method, to be displayed the translated text on the screen.


```
function initWebSocket() {
    //... Continue

    websocket.onmessage = function (e) {
        //Display the Translated text to browser
        writeToScreen(e);
    };
}
```

The Writetoscreen() method analyzes the JSON which received from the WebSocket server endpoint and outputs the screen.

There are two types of type: "partial" and "final" returned from the Microsoft Translator.

An example of a data reply from a Microsoft Translator:

```
Info:   MS-Translator : Message{type=partial, id=0.1, recognition=we, translation=私たち} 
Info:   MS-Translator : Message{type=final, id=0, recognition=We want to., translation=私たちがしたい。} 
Info:   MS-Translator : Message{type=partial, id=1.3, recognition=in microsoft hold, translation=マイクロソフトホールド} 
Info:   MS-Translator : Message{type=final, id=1, recognition=In Microsoft hold you back., translation=マイクロソフトでは、バックを保持します。}
```

The "partial" means partial translation, and "final" represents the translation result.

I also cusomized the JSON data as follows:

JSON data to reply to the browser

```
{"type":"final","origin":"In Microsoft hold you back."
 ,"translated":"マイクロソフトでは、バックを保持します。"}
```

The above means before translated data to the "origin", and the text data of the translated result to "translated".

In JavaScript, I determines the type of data, and then writes the partial translation results (partial) or final translation results (final) to a different location.

"partial" is overwritten at the top of the screen, "final" is implemented to append the latest translation on the first line.

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

Finally, when the stop button is pressed, call the following. In this case, the recording stops and the WebSocket connection is closed.

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

***Note:  
This time, voice translation was done by recording the voice on the browser and sending it to the server.
If you are implementing in a standalone application, you can send data to Microsoft Translator directly via Translatorwebsockerclientendpoint by creating audio data in sampling rate 16KHz, mono, 16bit, and receiving translation results when recording within an application.
Depending on the type of application you are creating, choose the appropriate implementation method.***


[Go Back to Top Page](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/ExplanationOfImplementation-en.md)