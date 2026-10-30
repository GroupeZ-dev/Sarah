package fr.maxlego08.sarah.logger;

public class JULogger {

    public static Logger from(java.util.logging.Logger logger) {
        return new Logger() {
            @Override
            public void info(String string) {
                logger.info(string);
            }

            @Override
            public void warn(String string) {
                logger.warning(string);
            }

            @Override
            public void error(String string) {
                logger.severe(string);
            }
        };
    }

}
