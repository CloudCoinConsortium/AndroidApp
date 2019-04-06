package global.cloudcoin.ccbank.LossFixer;

import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.Servant;

public class LossFixer extends Servant {
    String ltag = "LossFixer";
    LossFixerResult lr;


    public LossFixer(String rootDir, GLogger logger) {
        super("LossFixer", rootDir, logger);
    }

    public void launch(String user, CallbackInterface icb) {
        lr = new LossFixerResult();

        final String fuser = user;

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN LossFixer");

                doLossFix(fuser);

                if (cb != null)
                    cb.callback(lr);
            }
        });
    }

    public void doLossFix(String user) {
        logger.debug(ltag, "Lossfix started");
    }

}
