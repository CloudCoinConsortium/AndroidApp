package global.cloudcoin.ccbank;

import android.util.Log;

import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.GLoggerInterface;

public class ALogger extends GLogger implements GLoggerInterface {

    @Override
    public void onLog(int level, String tag, String message) {
        if (level == GL_DEBUG) {
            Log.d(tag, message);
        } else if (level == GL_VERBOSE) {
            Log.v(tag, message);
        } else {
            Log.i(tag, message);
        }
    }
}
