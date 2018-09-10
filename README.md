# FTPSample

Program that reproduces a problem when uploading files. Most of the times 0 bytes files appear in server.

- The program will attempt to upload a text file.
- As in the real program it has retry functionality (max 5 attempts)
- This program requires Java 8 (JDK 1.8) and maven.

## To run the program

1. Make sure to define the following variables to connect to the right server/gateway

    ```
    export ftpHost='sg-au-syd-X-X.integration.ibmcloud.com'
    export ftpPort='12345'
    export ftpDirectory='MY/DIRECTORY'
    export ftpUsername='MY_USER'
    export ftpPassword='MY_PASSWORD'
    ```

2. Call App.java main method. This can be done via several ways. One is using `maven`

    ```
    cd FTPSample
    mvn clean install
    ```

3. That is all, now check logs :)


## In case maven is not installed.

1. Just install it via brew or apt-get or [manually](https://maven.apache.org/install.html).
2. Once installed check its version and make sure Java 8 is used. (**This program requires JDK 1.8**)

```
$ mvn --version
Apache Maven 3.5.2 (138edd61fd100ec658bfa2d307c43b76940a5d7d; 2017-10-18T16:58:13+09:00)
Maven home: /usr/local/Cellar/maven/3.5.2/libexec
Java version: 1.8.0_161, vendor: Oracle Corporation
Java home: /Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre
Default locale: en_JP, platform encoding: UTF-8
OS name: "mac os x", version: "10.13.6", arch: "x86_64", family: "mac"
```
