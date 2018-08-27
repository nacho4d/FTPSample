package com.ibm.example;

import sun.rmi.log.LogHandler;

import java.util.logging.*;

public class LogUtil {

    private final static String NEW_LINE = System.getProperty("line.separator");

    static public class OneLineFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            // Cloud logging services do not require timestamps
            return "[" + record.getLevel().toString() + "] " + record.getLoggerName() + ": " + record.getMessage() + NEW_LINE;
        }

    }


    static public class LogHandler extends ConsoleHandler {

        public LogHandler() {
            setOutputStream(System.out);
            setLevel(Level.ALL);
            setFormatter(new OneLineFormatter());
        }

    }

        static LogHandler commonHandler = new LogHandler();

        /**
         * Create a special logger for cloud logging services
         *
         * @param name logger name
         * @return loggerCsvReaderTest.java
         */
        public static Logger getLogger(String name) {

            String[] components = name.split("\\.");
            String logName = components.length > 0 ? components[components.length - 1] : name;

            Logger logger = Logger.getLogger(logName);

            // Remove similar previous handlers to avoid repetitions (when re-deploying the same module)
            Handler[] handlers = logger.getHandlers();
            for (int i = handlers.length - 1; i >= 0; --i) {
                if (handlers[i].getClass().getName().equals(LogHandler.class.getName())) {
                    logger.removeHandler(handlers[i]);
                }
            }
            logger.addHandler(commonHandler);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
            return logger;
        }
}
