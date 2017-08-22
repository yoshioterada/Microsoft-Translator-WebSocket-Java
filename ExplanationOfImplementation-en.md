I published a WebSocket application using Microsoft Translator on GitHub. Here's a concrete introduction to how to implement it.

# Microsoft Translator WebSocket Application Implementation features Overview

There are two major parts of this project.


![MS-Translator](https://c1.staticflickr.com/5/4343/36229105460_21fc464b42.jpg)


1. ***Front-End implementation: Create voice data, display results***
2. ***Server implementation: Convert voice data, request Microsoft Translator***
2.1: WebSocket server implementation that receives voice data from the front end  
2.2: WebSocket Client implementation to send connection and voice data to Microsoft Translator

The frontend implementation is implemented using HTML/JavaScript, but not only HTML but also youcan be implemented in JavaFX or in any programming language. For example .Net applications or WebSocket clients which wrote on any programing language.  
In addition, the implementation of the server is implemented using the Java WebSocket library, but if you understand how to request the Microsoft Translator, you may be able to implement it by using other programming languages.

## 2. Server side: Translator Connection WebSocket Client Implementation overview

1. Create a URL for connecting Microsoft Translator
2. CreatE an Authorization Header
3. Create a X-clienttraceid: {GUID} header 
4. Send voice data to Microsoft Translator

![Server-Impl](https://c1.staticflickr.com/5/4342/36625812485_009363be97.jpg)

You can communicate with Microsoft Translator by using WebSocket protocol. Please refer to the <A HREF="http://docs.microsofttranslator.com/speech-translate.html">Reference Guide</A>.


### 1. Create a connection URL

Create a URL to connect to the Microsoft Translator. Specify the original language in "from" and specify the language to translate "to". For example, if you want to translate from English to Japanese, the WebSocket connection URL will be following:


```
wss://dev.microsofttranslator.com/speech/translate?
features=partial&from=en-US&to=ja-JP&api-version=1.0;
```

Currently (as of August 2017), the following languages are supported:

|Supported language |values|
|---|---|
|Arabic| Ar-eg|
|German| de|
|Spanish| ES|
|English| en-US|
|French| FR|
|Italian| specifically|
|Japanese| ja|
|Portuguese| PT|
|Russian| ru|
|Chinese Simplified| ZH|
|Chinese Traditional| ZH|


In the URL, you can set the "features" parameter to obtain additional information. For example, if you want to get partial translation results instead of just the final translation results, please specify  features=partial. If you want to voice the translated results, add features=Texttospeech.   
Note : "Text to speech" is omitted in my sample implementation. Because it is not necessary in my app.

### 2. Create a Authorization header

To connect to the URL of the Microsoft Translator, you must attach the access token to the header for authentication.

```
Authorization: Bearer {access token}
```

To obtain this access token, you have to use the [Token API V1.0](https://dev.cognitive.microsoft.com/docs/services/57346a70b4769d2694911369/operations/57346edcb5816c23e4bf7421). For example, if you use the Curl command to obtain an access token, you can run the following command to retrieve it:

Example: Authentication Token API for Microsoft Cognitive Services Translator API

```
$ curl -X POST
"https://api.cognitive.microsoft.com/sts/v1.0/issueToken"
-H "Ocp-Apim-Subscription-Key: {Subscription_key}"
--data-ascii ""
```

The Header value of "{Subscription_key}" for "Ocp-Apim-Subscription-Key:" is automatically generated when you create a service for Microsoft Translator.  
You can be obtained from the Azure management portal.

#### 2.1 Getting Subscription Key

Please create a service for Microsoft Translator from the Microsoft Azure management portal? Select the "Translator-speach-api" resource after you create it. You will see the following screen when you select.

![Image](https://c1.staticflickr.com/5/4385/36229070970_a746117f9b.jpg)

Then please select "Kyes" from "RESOURCE MANAGEMENT". You will see the following screen when you select.

![Image](https://c1.staticflickr.com/5/4368/36487708461_0de0b4ae79.jpg)

The random string displayed in the text field at "Key 1", "Key 2". It  becomes Subscription key. The contents listed here are appended to the "Ocp-apim-subscription-key:" header.

#### 2.2 Getting Access Tokens

Now that you have the Subscription Key, you can actually get access tokens. Try to obtain the Access Token actually by specifying the Subscription Key that you obtained.

```
$ curl -X POST 
  "https://api.cognitive.microsoft.com/sts/v1.0/issueToken" 
  -H "Ocp-Apim-Subscription-Key: 8c74************************d25e" 
  --data-ascii ""  

eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzY29wZSI6Imh0dHBzOi8vZGV2Lm1pY
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
QDAChEx0PKxQ5ZGtArRi-Ok 
```

#### 2.3 Authorization Header Example

Now you have access tokens. You can create a header to connect to the Microsoft Translator.
The actual Authorization header is as follows.

```
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzY29wZSI6Imh0dHBzOi8vZGV2Lm1pY
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
**********************************************************************  
QDAChEx0PKxQ5ZGtArRi-Ok 
```  

### 3. Create a X-clienttraceid: {GUID} header

If you specify the "X-ClientTraceId", you can be used to troubleshoot. The client side attaches this value to the header and connects to Microsoft Translator. You should also save this ID to a log on local, etc. so that you can refer to it later.

The actual X-clienttraceid header is as follows.

```
X-clienttraceid: {UUID}
```

### 4. Sending voice data

After generated the above 1-3 steps, you will be able to translate (voice to text) when connected with WebSocket to the Microsoft Translator.

The audio data you send will send data in wav format, but the audio data you send should follow the format below.

|Item| Value|
|---|---|
|Sound source (number of channels)| mono|
|Sampling rate| 16 KHz|
|Format Size| 16bit signed PCM|

Please note that the above is also mentioned in the [Reference Guide](http://docs.microsofttranslator.com/speech-translate.html). In particular, when you record audio on a recent system, it is saved as a higher-quality data (e.g., stereo, sampling rate: 44.100 kHz, Format size 16bit). If this is the case, please convert it to the above format and send it to Microsoft Translator.

In terms of transmitting audio data, wav data can be sent one at a time, but you may want to transmit data by streaming. If you want to send audio in streaming, you can send only the wave form data (PCM) of the sound wave by making the the wav header with audio size of 0.


# Learn more about Microsoft Translator WebSocket Application Java Implementation Features

After you have an overview of the above implementation, learn more about the implementation in Java below.

Java server-side Implementation Details Part1 (Japanese)

Java server-side Implementation Details PART2 (Japanese)

Learn more about Frontend implementations with JavaScript Part3 (Japanese)