package com.ibm.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Logger;

import static java.lang.Integer.parseInt;

public class App {

    // Taken from https://www.karamasoft.com/ultimatespell/samples/longtext/longtext.aspx
    static String fileContents =
            "Miusov, as a man man of breeding and deilcacy, could not but feel some inwrd qualms, \n" +
            "when he reached the Father Superior's with Ivan: he felt ashamed of havin lost his temper. He felt that\n" +
            "he ought to have disdaimed that despicable wretch, Fyodor Pavlovitch, too much to have been upset by him \n" +
            "in Father Zossima's cell, and so to have forgotten himself. \"Teh monks were not to blame, in any case,\"\n" +
            "he reflceted, on the steps." +
            "\"And if they're decent people here (and the Father Superior, I understand, is a nobleman) why not be friendly \n" +
            "and courteous withthem? I won't argue, I'll fall in with everything, I'll win them by politness, and\n" +
            "show them that I've nothing to do with that Aesop, thta buffoon, that Pierrot, and have merely been takken in\n" +
            "over this affair, just as they have.\"\n" +
            "\n" +
            "He determined to drop his litigation with the monastry, and relinguish his claims to the wood-cuting and fishery\n" +
            "rihgts at once. He was the more ready to do this becuase the rights had becom much less valuable, and he had \n" +
            "indeed the vaguest idea where the wood and river in quedtion were.\n" +
            "\n" +
            "\n" +
            "These excellant intentions were strengthed when he enterd the Father Superior's diniing-room, though, stricttly\n" +
            "speakin, it was not a dining-room, for the Father Superior had only two rooms alltogether; they were, however, \n" +
            "much larger and more comfortable than Father Zossima's. But tehre was was no great luxury about the furnishng of\n" +
            "these rooms eithar. The furniture was of mohogany, covered with leather, in the old-fashionned style of 1820 the\n" +
            "floor was not even stained, but evreything was shining with cleanlyness, and there were many chioce flowers in\n" +
            "the windows; the most sumptuous thing in the room at the moment was, of course, the beatifuly decorated table.\n" +
            "The cloth was clean, the service shone; there were three kinds of well-baked bread, two bottles of wine, two\n" +
            "of excellent mead, and a large glass jug of kvas -- both the latter made in the monastery, and famous in the\n" +
            "neigborhood. There was no vodka. Rakitin related afterwards that there were five dishes: fish-suop made of \n" +
            "sterlets, served with little fish paties; then boiled fish served in a spesial way; then salmon cutlets, \n" +
            "ice pudding and compote, and finally, blanc-mange. Rakitin found out about all these good things, for he \n" +
            "could not resist peeping into the kitchen, where he already had a footing. He had a footting everywhere, \n" +
            "and got informaiton about everything. He was of an uneasy and envious temper. He was well aware of his own\n" +
            "considerable abilities, and nervously exaggerated them in his self-conceit. He knew he would play a\n" +
            "prominant part of some sort, but Alyosha, who was attached to him, was distressed to see that his friend \n" +
            "Rakitin was ";

    public static void main( String[] args ) throws IOException {

        final String ftpHost = System.getenv("ftpHost");
        final String ftpPort = System.getenv("ftpPort");
        final String ftpDirectory = System.getenv("ftpDirectory");
        final String ftpUsername = System.getenv("ftpUsername");
        final String ftpPassword = System.getenv("ftpPassword");

        FTPConfiguration configuration = new FTPConfiguration() {
            @Override
            public String getFtpAddress() {
                return ftpHost == null ? "localhost" : ftpHost;
            }

            @Override
            public int getFtpPort() {
                int port = 21;
                try {
                    port = parseInt(ftpPort);
                } catch (Exception e) {
                    System.out.println("Bad port found. " + ftpPort + ". Falling back to " + port);
                }
                return port;
            }

            @Override
            public String getFtpUsernameOut() {
                return ftpUsername == null ? "my-account-name" : ftpUsername;
            }

            @Override
            public String getFtpPasswordOut() {
                return ftpPassword == null ? "pass" : ftpPassword;
            }

            @Override
            public String getFtpWorkingDirectoryOut() {
                return ftpDirectory == null ? "/" : ftpDirectory;
            }
        };

        System.out.println("Configuration:" +
                "\n host:" + configuration.getFtpAddress() +
                "\n port:" + configuration.getFtpPort() +
                "\n directory:" + configuration.getFtpWorkingDirectoryOut() +
                "\n username:" + configuration.getFtpUsernameOut() +
                "\n password:" + configuration.getFtpPasswordOut());

        long time = (new Date()).getTime();
        String fileName = "IFAKHB001_TEST_" + time + ".csv";
        System.out.println("Filename: " + fileName);

        byte[] encryptedBytes = fileContents.getBytes(StandardCharsets.UTF_8);
        FTPTraceListener listener = new FTPTraceListener();
        Logger logger = LogUtil.getLogger("App");
        FTPHelper ftpHelper = new FTPHelper();
        ftpHelper.writeBytesToCsvFile(configuration, encryptedBytes, fileName, listener, logger);
    }
}
