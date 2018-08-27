package com.ibm.example;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class MyFTPClient {
    /**
     * Create an instance of SimpleFTP.
     */
    public MyFTPClient() {

    }

    /**
     * Connects to an FTP server and logs in with the supplied username
     * and password.
     */
    public synchronized void connect(String host, int port, String user, String pass) throws IOException {
        if (socket != null) {
            throw new IOException("SimpleFTP is already connected. Disconnect first.");
        }
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        String response = readLine();
        if (!response.startsWith("220 ")) {
            throw new IOException("SimpleFTP received an unknown response when connecting to the FTP server: " + response);
        }

        sendLine("USER " + user);

        response = readLine();
        if (!response.startsWith("331 ")) {
            throw new IOException("SimpleFTP received an unknown response after sending the user: " + response);
        }

        sendLine("PASS " + pass);

        response = readLine();
        if (!response.startsWith("230 ")) {
            throw new IOException("SimpleFTP was unable to log in with the supplied password: " + response);
        }

        // Now logged in.
    }


    /**
     * Disconnects from the FTP server.
     */
    public synchronized void disconnect() throws IOException {
        try {
            if (socket != null) {
                sendLine("QUIT");
            }
        }
        finally {
            socket = null;
        }
    }


    /**
     * Returns the working directory of the FTP server it is connected to.
     */
    public synchronized String pwd() throws IOException {
        sendLine("PWD");
        String dir = null;
        String response = readLine();
        if (response.startsWith("257 ")) {
            int firstQuote = response.indexOf('\"');
            int secondQuote = response.indexOf('\"', firstQuote + 1);
            if (secondQuote > 0) {
                dir = response.substring(firstQuote + 1, secondQuote);
            }
        }
        return dir;
    }


    /**
     * Changes the working directory (like cd). Returns true if successful.
     */
    public synchronized boolean cwd(String dir) throws IOException {
        sendLine("CWD " + dir);
        String response = readLine();
        return (response.startsWith("250 "));
    }


    /**
     * Sends a file to be stored on the FTP server.
     * Returns true if the file transfer was successful.
     * The file is sent in passive mode to avoid NAT or firewall problems
     * at the client end.
     */
    public synchronized boolean stor(File file) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("SimpleFTP cannot upload a directory.");
        }

        String filename = file.getName();

        return stor(new FileInputStream(file), filename);
    }

    public String resolvePassiveNatAddress(String hostname) throws UnknownHostException {
        String newHostname = hostname;
        InetAddress host = InetAddress.getByName(newHostname);
        // reply is a local address, but target is not - assume NAT box changed the PASV reply
        if (host.isSiteLocalAddress()) {
            //InetAddress remote = socket.getRemoteAddress();
            InetAddress remote = socket.getInetAddress();
            if (!remote.isSiteLocalAddress()){
                newHostname = remote.getHostAddress();
                System.out.println("(NAT local address work-around) replacing address:" + hostname + " with:" + newHostname);
            }
        }
        return newHostname;
    }


    /**
     * Sends a file to be stored on the FTP server.
     * Returns true if the file transfer was successful.
     * The file is sent in passive mode to avoid NAT or firewall problems
     * at the client end.
     */
    public synchronized boolean stor(InputStream inputStream, String filename) throws IOException {

        BufferedInputStream input = new BufferedInputStream(inputStream);

        sendLine("PASV");
        String response = readLine();
        //String res2 = readLine();
        //String res3 = readLine();
        //System.out.println("response: " + response + " " + res2 + " " + res3);
        if (!response.startsWith("227 ")) {
            throw new IOException("SimpleFTP could not request passive mode: " + response);
        }

        String ip = null;
        int port = -1;
        int opening = response.indexOf('(');
        int closing = response.indexOf(')', opening + 1);
        if (closing > 0) {
            String dataLink = response.substring(opening + 1, closing);
            StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
            try {
                ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken();
                port = Integer.parseInt(tokenizer.nextToken()) * 256 + Integer.parseInt(tokenizer.nextToken());
            }
            catch (Exception e) {
                throw new IOException("SimpleFTP received bad data link information: " + response);
            }
        }

        sendLine("STOR " + filename);
        Socket dataSocket;
        try {
            dataSocket = new Socket(ip, port);
        } catch (Exception e) {
            ip = resolvePassiveNatAddress(ip);
            dataSocket = new Socket(ip, port);
        }

        response = readLine();
        if (!response.startsWith("150 ")) {
            throw new IOException("SimpleFTP was not allowed to send the file: " + response);
        }

        BufferedOutputStream output = new BufferedOutputStream(dataSocket.getOutputStream());
        //byte[] buffer = new byte[4096];
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
            logSent("=== wrote " + bytesRead + " bytes");
        }
        output.flush();
        output.close();
        input.close();

        response = readLine();
        return response.startsWith("226 ");
    }


    /**
     * Enter binary mode for sending binary files.
     */
    public synchronized boolean bin() throws IOException {
        sendLine("TYPE I");
        String response = readLine();
        return (response.startsWith("200 "));
    }


    /**
     * Enter ASCII mode for sending text files. This is usually the default
     * mode. Make sure you use binary mode if you are sending images or
     * other binary data, as ASCII mode is likely to corrupt them.
     */
    public synchronized boolean ascii() throws IOException {
        sendLine("TYPE A");
        String response = readLine();
        return (response.startsWith("200 "));
    }


    /**
     * Sends a raw command to the FTP server.
     */
    void sendLine(String line) throws IOException {
        if (socket == null) {
            throw new IOException("SimpleFTP is not connected.");
        }
        try {
            writer.write(line + "\r\n");
            writer.flush();
            logSent(line);
        }
        catch (IOException e) {
            socket = null;
            throw e;
        }
    }

    String readLine() throws IOException {
        String line = reader.readLine();
        logRead(line);

        return line;
    }

    private Socket socket = null;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;

    interface FTPCommunicationListener {
        void received(String message);
        void sent(String message);
    }

    private List<FTPCommunicationListener> listeners = new ArrayList<>();

    public void addCommunicationListener(FTPCommunicationListener listener) {
        listeners.add(listener);
    }

    private void logRead(String receivedMessage) {
        for (FTPCommunicationListener l : listeners) {
            l.received(receivedMessage);
        }
    }

    private void logSent(String sentMessage) {
        for (FTPCommunicationListener l : listeners) {
            l.sent(sentMessage);
        }
    }

}

