package com.ibm.example;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

class FTPHelper {

    // Delays in milliseconds
    static long[] FTP_STORE_ATTEMPTS_DELAYS = new long []{ 0, 5*1000, 10*1000, 15*1000, 15*1000};

    private static void ftpLog(FTPTraceListener listener, String message) {
        if (listener != null && listener.isActive()) {
            listener.log(message);
        }
    }

    private static MyFTPClient getConnectedMyFtpClient(
            String ftpAddress,
            int ftpPort,
            String ftpUserId,
            String ftpPassword,
            String workingDirectory,
            FTPTraceListener listener,
            Logger logger) throws IOException {

        MyFTPClient ftpClient = null;

        try {
            ftpLog(listener, "[FTPHelper] starting...");

            // Setup FTP client
            ftpClient = new MyFTPClient();
            ftpLog(listener, "[FTPHelper] ftpClient created.");

            ftpClient.addCommunicationListener(listener);
            ftpLog(listener, "[FTPHelper] ftpClient communication listener added.");

            ftpClient.connect(ftpAddress, ftpPort, ftpUserId, ftpPassword);
            ftpLog(listener, "[FTPHelper] ftpClient connected and logged in.");

            if (workingDirectory != null && !workingDirectory.isEmpty() && !"/".equals(workingDirectory)) {
                ftpClient.cwd(workingDirectory);
                ftpLog(listener, "[FTPHelper] ftpClient changed working directory.");
            }
        } catch (IOException e) {
            disconnectMyFtpClient(ftpClient, logger);
            logger.severe(listener.toString());
            throw e;
        }
        return ftpClient;
    }

    private static void disconnectMyFtpClient(MyFTPClient ftpClient, Logger logger) {
        if (ftpClient == null) {
            return;
        }
        try {
            ftpClient.disconnect();
        } catch (IOException innerE) {
            logger.info("[FTPHelper] Tried to disconnect ftp client. " + innerE.getMessage());
            innerE.printStackTrace();
        }
    }

    public void writeBytesToCsvFile(
            FTPConfiguration configuration,
            byte[] fileContents,
            String filename,
            FTPTraceListener listener,
            Logger logger) throws IOException {

        MyFTPClient ftpClient = null;
        InputStream inputStreamEnd = null;
        try {
            String ftpAddress = configuration.getFtpAddress();
            int ftpPort = configuration.getFtpPort();
            String ftpUserId = configuration.getFtpUsernameOut();
            String ftpPassword = configuration.getFtpPasswordOut();
            String workingDirectory = configuration.getFtpWorkingDirectoryOut();
            ftpClient = getConnectedMyFtpClient(ftpAddress, ftpPort, ftpUserId, ftpPassword, workingDirectory, listener,
                    logger);


            ftpLog(listener, "[FTPHelper] starting writeBytesToCsvFile. " + ftpClient.toString());

            // Store file
            ftpClient.bin();
            storeFileAndRetry(ftpClient, filename, fileContents, FTP_STORE_ATTEMPTS_DELAYS, listener, logger);

        } finally {
            disconnectMyFtpClient(ftpClient, logger);
            closeQuietly(inputStreamEnd, logger);
        }
    }

    private void storeFileAndRetry(MyFTPClient ftpClient, String filename, byte[] fileContents, long delayArray[], FTPTraceListener listener, Logger logger)
            throws IOException {
        InputStream inputStream = null;
        String fileBytesString = fileContents == null ? "null" : String.valueOf(fileContents.length);
        List<Long> delays = Arrays.stream(delayArray).boxed().collect(Collectors.toList());
        int attempt = 0;
        try {
            do {
                ++attempt;
                long delay = delays.remove(0);
                sleep(delay, logger);

                // Retry store file
                logger.info("[FTPHelper] Will send file contents (bytes:" + fileBytesString + ", attempt:" + attempt + ")");
                closeQuietly(inputStream, logger);
                inputStream = new ByteArrayInputStream(fileContents);
                boolean csvOk = ftpClient.stor(inputStream, filename);
                if (!csvOk) {
                    throw new IOException("[FTPHelper] ftpClient.stor csv file didnt work!");
                }

                // Retry check file size
                long size = checkFileSizeIsNotZero(ftpClient, filename, attempt, logger);
                if (size <= 0) {
                    // Failure. Change stored from true to false
                    logger.info("[FTPHelper] File size check failed: " + size);

                } else {
                    // Success
                    logger.info("[FTPHelper] Successfully stored file (attempt:" + attempt + ")");
                    return;
                }

            } while (!delays.isEmpty());
        } catch (Exception e) {
            System.out.println("Exception at storeFileAndRetry.... " + e.getMessage());
            throw new IOException(e);
        } finally {
            closeQuietly(inputStream, logger);
        }

        throw new IOException("[FTPHelper] Could not store file " + filename + " and/or its size was incorrect too many times (" + delayArray.length + "). ");
    }

    private void closeQuietly(InputStream is, Logger logger) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (Exception e) {
            logger.severe("[FTPHelper] Tried to close input stream. " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sleep(long timeout, Logger logger) {
        if (timeout <= 0) {
            return;
        }
        logger.info("[FTPHelper] Will wait for " + (timeout / 1000.0) + " seconds. (now: " + new Date().toString() + ")");
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);// Wait for n milliseconds and retry
        } catch (InterruptedException e) {
            logger.info("[FTPHelper] wait was interrupted. " + e.getMessage() + " (now: " + new Date().toString() + ")");
        }
        logger.info("[FTPHelper] Wait finished. (now: " + new Date().toString() + ")");
    }

    long checkFileSizeIsNotZero(MyFTPClient ftpClient, String filename, int tryNum, Logger logger) throws IOException {
        ftpClient.sendLine("SIZE " + filename);
        String res = ftpClient.readLine();
        StringTokenizer tokenizer = new StringTokenizer(res, " ");
        String resCode = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
        if (!"213".equals(resCode)) {
            throw new IOException("Unexpected server response: " + res);
        }
        String resSize = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
        try {
            return parseInt(resSize);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
