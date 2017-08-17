先日、Microsoft Translator を利用した WebSocket のアプリケーションを GitHub に公開しました。
実装方法について具体的に紹介します。

# Microsoft Translator WebSocket Application 実装機能の概要

今回作成したのは大きく分けて２つあります。

![MS-Translator](https://c1.staticflickr.com/5/4343/36229105460_21fc464b42.jpg)


1. ***フロントエンド実装：音声データの作成、結果表示***
2. ***サーバ実装：音声データの変換、Microsoft Translator へのリクエスト***  
  2.1: フロント・エンドから音声データを受け取る WebSocket サーバ実装  
  2.2: Microsoft Translator に対して接続・音声データを送信する WebSocket Client 実装 

フロントエンドの実装を今回は HTML/JavaScript を用いて実装していますが、WebSocket のクライアントであれば実装は JavaFX であっても、.Net アプリケーションでも任意のプログラミング言語で実装しても構いません。  
また、サーバの実装は Java の WebSocket ライブラリを利用して実装していますが、Microsoft Translator へのリクエストのノウハウが理解できれば他のプログラミング言語を使用して実装する事も可能かと思います。

## 2. サーバ側: Translator接続 WebSocket Client 実装の概要

 1. 接続 URL の作成
 2. Authorization ヘッダの作成
 3. X-ClientTraceId: {GUID} ヘッダの作成
 4. 音声データの送信  

![Server-Impl](https://c1.staticflickr.com/5/4342/36625812485_009363be97.jpg)

----
Microsoft Translator は<A HREF="http://docs.microsofttranslator.com/speech-translate.html">リファレンス・ガイド</A>にもあるように WebSocket で通信を行います。  

### 1. 接続 URL の作成  
Microsoft Translator へ接続するための URL を作成します。from で元の言語を指定し、to で翻訳する言語を指定します。たとえば英語から日本語への翻訳を行いたい場合の WebSocket 接続 URL は下記のようになります。  

wss://dev.microsofttranslator.com/speech/translate?features=partial&from=en-US&to=ja-JP&api-version=1.0;  

現在 (2017年8月時点) 対応している言語は下記です。
<table>
<tr><td>対応言語</Td><td>値</td></tr>
<tr><td> Arabic </Td><td>ar-EG</td></tr>
<tr><td> German </Td><td>de-DE</td></tr>
<tr><td> Spanish </Td><td>es-ES</td></tr>
<tr><td> English </Td><td>en-US</td></tr>
<tr><td> French </Td><td>fr-FR</td></tr>
<tr><td> Italian </Td><td>it-IT</td></tr>
<tr><td> Japanese </Td><td>ja-JP</td></tr>
<tr><td> Portuguese </Td><td>pt-BR</td></tr>
<tr><td> Russian </Td><td>ru-RU</td></tr>
<tr><td> Chinese Simplified </Td><td>zh-CN</td></tr>
<tr><td> Chinese Traditional </Td><td>zh-TW</td></tr>
</table>  
URL を指定する際、<b>features</b> パラメータを設定すると、追加情報を取得することができます。例えば最終的な翻訳結果だけではなく、部分的な翻訳結果を取得したい場合、features=partial を取得します。また、翻訳された結果を音声化したい場合は、features=TextToSpeech を付加します。  
（今回の実装では音声化までは不要なので、TextToSpeech は省略しています。）  

### 2. Authorization ヘッダの作成  
上記で作成した Microsoft Translator の URL に対して接続するためには、ヘッダにアクセス・トークンを付加し認証する必要があります。  

```
Authorization: Bearer {access token}
```  

この、Access Token を取得するためには、[Token API V1.0](https://dev.cognitive.microsoft.com/docs/services/57346a70b4769d2694911369/operations/57346edcb5816c23e4bf7421) を利用します。
たとえば、curl コマンドを利用してアクセス・トークンを取得する場合は、下記のコマンドを実行し取得可能です。  
(具体例：[Authentication Token API for Microsoft Cognitive Services Translator API
](http://docs.microsofttranslator.com/oauth-token.html))

```
$ curl -X POST 
  "https://api.cognitive.microsoft.com/sts/v1.0/issueToken" 
  -H "Ocp-Apim-Subscription-Key: {SUBSCRIPTION_KEY}" 
  --data-ascii ""
```

ここで、HTTP ヘッダに付加する "Ocp-Apim-Subscription-Key: "の "{SUBSCRIPTION_KEY}" の値は、Azure の管理ポータルなどから Microsoft Translator 用のサービスを作成した際に自動的に生成され、Azure 管理ポータルから取得する事ができます。


#### 2.1 Subscription Key の取得
Microsoft Azure の管理ポータルより、Microsoft Translator 用のサービス (Translator-Speach-API) を作成してください。作成したのち "Translator-Speach-API" のリソースを選択してください。選択すると下記の画面が表示されます。

![Image](https://c1.staticflickr.com/5/4385/36229070970_a746117f9b.jpg)

次に "RESOURCE MANAGEMENT" より "Kyes" を選択してください。選択すると下記の画面が表示されます。

![Image](https://c1.staticflickr.com/5/4368/36487708461_0de0b4ae79.jpg)

ここで "KEY 1", "KEY 2" のテキストフィールド内に表示されているランダムな文字列が Subscription Key になります。ここに記載されている内容を、”Ocp-Apim-Subscription-Key:” ヘッダに付加します。


#### 2.2 Access Token の取得

Subscription Key を入手したので、実際に Access Token を入手することができるようになりました。入手した Subscription Key を指定して Access Token を実際に入手してみてください。

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

#### 2.3 Authorization ヘッダの例
上記より、アクセス・トークンを入手できましたので、Microsoft Translator に対して接続するためのヘッダが作成できます。  
実際の Authorization ヘッダは下記のような内容になります。

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


### 3. X-ClientTraceId: {GUID} ヘッダの作成  
X-ClientTraceId はトラブルシューティングを行う際に利用できます。クライアント側はこの値をヘッダに付加し Microsoft Translator に接続してください。また、この ID はログ等に保存し、あとで参照できるようにしてください。


実際の X-ClientTraceId ヘッダは下記のような内容になります。

```
X-ClientTraceId: {UUID}
```  

### 4. 音声データの送信  
上記 1-3 を生成後、Microsoft Translator に対して WebSocket で接続すると翻訳 (音声→テキスト) ができるようになります。  

送信する音声データは、 wav 形式のデータを送信するのですが、送信する音声データは下記のフォーマットに従う必要があります。  
<table>
<tr><td>項目</td><td>値</td></tr>
<tr><td>音源(チャネル数)</td><td>モノラル</td></tr>
<tr><td>サンプリング・レート</td><td>16 kHz</td></tr>
<tr><td>フォーマット・サイズ</td><td>16bit 符号付きPCM</td></tr>
</table>  
上記は、<A HREF="http://docs.microsofttranslator.com/speech-translate.html">リファレンス・ガイド</A>にも記載がありますのでご注意ください。  
特に、最近のシステムで音声を録音すると、より高音質のデータとして保存されます（例：ステレオ、サンプリングレート：44.100 kHz、フォーマット・サイズが 16bit）。その場合は、上記のフォーマットに変換して Microsoft Translator に送信してください。  

また音声データの送信に関しては、wav のデータを都度わけて1つづつ送信することもできますが、ストリーミングでデータを流しつづけながら送信したい場合もあります。ストリーミングで音声を送信したい場合、wav ヘッダ情報の音声サイズを 0 にする事で、音波の波形データ (PCM) だけを送信できるようになります (下記 Java ソースコード説明の 4.4 をご参照)。  

# Microsoft Translator WebSocket Application Java 実装機能の詳細

上記実装方法の概要を理解したのち、下記 Java での実装の詳細をご覧ください。

* [Java による実装の詳細 Part1(日本語)](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/ExplanationOfImplebyJava-1.md)  
* [Java による実装の詳細 Part2(日本語)](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/ExplanationOfImplebyJava-2.md)

