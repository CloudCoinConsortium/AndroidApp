package global.cloudcoin.ccbank.Authenticator;

import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.Servant;

public class Authenticator extends Servant {

    String ltag = "Authencticator";

    public Authenticator(String rootDir, GLogger logger) {
        super("Authenticator", rootDir, logger);
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Authenticator");
                doAuthencticate();
                cb.callback(null);
            }
        });
    }

    public void doAuthencticate() {
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            return;
        }

        logger.info(ltag, "Auth!");
    }
}
