package global.cloudcoin.ccbank.Echoer;

import android.util.Log;

import global.cloudcoin.ccbank.core.Servant;
import global.cloudcoin.ccbank.core.GLogger;


public class Echoer extends Servant {

    public Echoer(String rootDir, GLogger logger) {
        super("Echoer", rootDir, logger);

        Log.v("XXX", "ECHOER");
    }

    public void run() {
        Log.v("ZZZZZZ", "ECHHHHO");
    }

}
