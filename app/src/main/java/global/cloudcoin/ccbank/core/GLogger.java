package global.cloudcoin.ccbank.core;



public abstract class GLogger implements GLoggerInterface {


    public void info(String tag, String message) {
        onLog(GLoggerInterface.GL_INFO, tag, message);
    }

    public void debug(String tag, String message) {
        onLog(GLoggerInterface.GL_DEBUG, tag, message);
    }

    public void verbose(String tag, String message) {
        onLog(GLoggerInterface.GL_VERBOSE, tag, message);
    }

    public void error(String tag, String message) {
        onLog(GLoggerInterface.GL_ERROR, tag, message);
    }
}
