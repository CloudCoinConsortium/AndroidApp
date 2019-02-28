package global.cloudcoin.ccbank.Authenticator;

import org.json.JSONException;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
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
             //   cb.callback(null);
            }
        });
    }


    public void processDetect(ArrayList<CloudCoin> ccs) {
        String[] results;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;

        int i;
        boolean first = true;

        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "multi_detect";
            sbs[i] = new StringBuilder();
        }

        for (CloudCoin cc : ccs) {
            cc.setPansToAns();

            for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                if (!first)
                    sbs[i].append("&");

                sbs[i].append("nns[]=");
                sbs[i].append(cc.nn);

                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);

                sbs[i].append("&denomination[]=");
                sbs[i].append(cc.getDenomination());

                sbs[i].append("&ans[]=");
                sbs[i].append(cc.ans[i]);

                sbs[i].append("&pans[]=");
                sbs[i].append(cc.pans[i]);
            }

            first = false;
        }
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts);
        if (results == null) {
            logger.error(ltag, "Failed to query multi_detect");
            return;
        }

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
/*
            ers[i] = (EchoResponse) parseResponse(results[i], EchoResponse.class);
            if (!ers[i].status.equals(Config.RAIDA_STATUS_READY)) {
                logger.error(ltag, "RAIDA " + i + " is not ready");
                cntErr++;
            }
            */
        }

    }

    public void doAuthencticate() {
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            return;
        }



        String fullPath = AppCore.getUserDir(Config.DIR_SUSPECT);

        CloudCoin cc;
        ArrayList<CloudCoin> ccs;

        ccs = new ArrayList<CloudCoin>();

        int cnt = 0;
        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;


        File dirObj = new File(fullPath);
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                AppCore.moveToTrash(file.toString());
                continue;
            }

            ccs.add(cc);

            logger.info(ltag, "asize="+ccs.size());

            maxCoins = 3;
            if (ccs.size() == maxCoins) {
                logger.info(ltag, "Processing");

                processDetect(ccs);

                ccs.clear();
            }
        }

        for (int i = 0; i < 3; i++) {
            if (cb != null) {
                logger.info(ltag, "go");
                AuthenticatorResult ar = new AuthenticatorResult();
                ar.totalFilesProcessed = i;
                ar.totalRAIDAProcessed = i;

                ar.totalCoinsInFile = 10;
                ar.totalCoinsProcessedInFile = i;

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {

                }


                cb.callback(ar);
            }
        }


        logger.info(ltag, "Auth!");
    }
}
