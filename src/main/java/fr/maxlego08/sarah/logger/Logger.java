package fr.maxlego08.sarah.logger;

public interface Logger {

    /**
     * Sends an info message to the console.
     *
     * @param string the message to be sent
     */
    void info(String string);

    /**
     * Sends a warning to the console. Defaults to {@link #info(String)}.
     *
     * @param string the message to be sent
     */
    default void warn(String string) {
        this.info(string);
    }

    /**
     * Reports a failed operation. Defaults to {@link #info(String)}.
     *
     * @param string the message to be sent
     */
    default void error(String string) {
        this.info(string);
    }
}
