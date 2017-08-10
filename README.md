# Microsoft Translator Java Web Socket Application.

Video (English to Japanese) : [Link of Demo Video](https://youtu.be/WmCg5I3h61U).  
[![](https://img.youtube.com/vi/WmCg5I3h61U/0.jpg)](https://www.youtube.com/watch?v=w3IBKvA0vuQ)  

Video (Japanese to English) : [Link of Demo Video](https://www.youtube.com/watch?v=u7oIap52PrM).  
[![](http://img.youtube.com/vi/u7oIap52PrM/0.jpg)](https://www.youtube.com/watch?v=u7oIap52PrM)  

## 1. Please change the propertis file 

At firsr, please get the subscription key for **"Translator-Speach-API"** from Microsoft Azure?

After you got the access key, please modify the [app-resources.properties](https://github.com/yoshioterada/Microsoft-Translator-WebSocket-Java/blob/master/src/main/resources/app-resources.properties) file?
 
## 2. Packaging  
After changed the properties file, please execute the following command?  

```
$ mvn clean package
```  

Then you can see the following file on target directory  

```
Microsoft-Translator-WebSocket-MSA-1.0-SNAPSHOT.jar
```  

## 3. Execution
Please execute following command?  

```
$ java -jar Microsoft-Translator-WebSocket-MSA-1.0-SNAPSHOT.jar --autoBindHttp --autoBindSsl --sslPort 8181
```   

Then you can see following output on console.

```
[2017-07-28T10:34:45.204+0900] [] [INFO] [] [PayaraMicro] [tid: _ThreadID=1 _ThreadName=main] [timeMillis: 1501205685204] [levelValue: 800] [[
  
Payara Micro URLs
http://192.168.11.2:8080/Microsoft-Translator-WebSocket-MSA-1.0-SNAPSHOT
https://192.168.11.2:8181/Microsoft-Translator-WebSocket-MSA-1.0-SNAPSHOT
 
'Microsoft-Translator-WebSocket-MSA-1.0-SNAPSHOT' REST Endpoints
Application Microsoft-Translator-WebSocket-MSA-1.0-SNAPSHOT has no deployed Jersey applications
```  

## 4. Access to the URL
Then please access to the following URL with Firefox or Chorome?  

[http://localhost:8080/Microsoft-Translator-WebSocket-MSA-1.0-SNAPSHOT/](http://localhost:8080/Microsoft-Translator-WebSocket-MSA-1.0-SNAPSHOT/)  

Then you will be able to see the Microsoft Translator Web Application Top Page. 

**Note :  
If you upload the application to Cloud and if you would like to  use the Chorome, you need to deploy it on HTTPS available server. Firefox may be available on Cloud too.
And you may face some issue. Now I'm investigating. So please try to this on your local machine?**

