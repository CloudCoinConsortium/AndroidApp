package global.cloudcoin.ccbank;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.GLoggerInterface;

public class ALogger extends GLogger implements GLoggerInterface {

    @Override
    public void onLog(int level, String tag, String message) {
        String levelStr;
        if (level == GL_DEBUG) {
            Log.d(tag, message);
            levelStr = "[DEBUG]";
        } else if (level == GL_VERBOSE) {
            Log.v(tag, message);
            levelStr = "[VERBOSE]";
        } else {
            Log.i(tag, message);
            levelStr = "[INFO]";
        }

        logCommon(tag + " " + levelStr + " " + message);
    }
}
