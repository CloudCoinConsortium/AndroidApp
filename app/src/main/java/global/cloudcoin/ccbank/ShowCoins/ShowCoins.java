package global.cloudcoin.ccbank.ShowCoins;

import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.Servant;

public class ShowCoins extends Servant {
    protected CallbackInterface cb;

    String ltag = "ShowCoins";

    public ShowCoins(String rootDir, GLogger logger) {
        super("ShowCoins", rootDir, logger);
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN ShowCoins");
                doShowCoins();
                cb.callback();
            }
        });
    }

    public void doShowCoins() {


        logger.info(ltag, "Auth!");
    }

    public void showCoinsInFolder(String folder) {


    }
}
