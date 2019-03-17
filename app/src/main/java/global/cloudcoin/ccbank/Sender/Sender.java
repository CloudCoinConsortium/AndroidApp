package global.cloudcoin.ccbank.Sender;

import org.json.JSONException;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;

import global.cloudcoin.ccbank.Authenticator.AuthenticatorResponse;
import global.cloudcoin.ccbank.Sender.SenderResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class Sender extends Servant {
    String ltag = "Sender";
    SenderResult sr;
    int[] valuesPicked;
    ArrayList<CloudCoin> coinsPicked;

    public Sender(String rootDir, GLogger logger) {
        super("Sender", rootDir, logger);
    }

    public void launch(String user, int tosn, int[] values, String envelope, CallbackInterface icb) {
        this.cb = icb;

        final String fuser = user;
        final int ftosn = tosn;
        final int[] fvalues = values;
        final String fenvelope = envelope;

        sr = new SenderResult();
        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Sender");
                doSend(fuser, ftosn, fvalues, fenvelope);


                if (cb != null)
                    cb.callback(sr);
            }
        });
    }

    public void doSend(String user, int tosn, int[] values, String envelope) {
        /*
        if (!updateRAIDAStatus()) {
            sr.status = SenderResult.STATUS_ERROR;
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            return;
        }*/

        String fullSentPath = AppCore.getUserDir(Config.DIR_SENT, user);
        String fullFrackedPath = AppCore.getUserDir(Config.DIR_FRACKED, user);
        String fullBankPath = AppCore.getUserDir(Config.DIR_BANK, user);

        if (values.length != AppCore.getDenominations().length) {
            logger.error(ltag, "Invalid params");
            sr.status = SenderResult.STATUS_ERROR;
            return;
        }

        if (!pickCoinsInDir(fullBankPath, values)) {
            logger.debug(ltag, "Not enough coins in the bank dir");
            if (!pickCoinsInDir(fullFrackedPath, values)) {
                logger.error(ltag, "Not enough coins in the Fracked dir");
                sr.status = SenderResult.STATUS_ERROR;

                return;
            }
        }

        setSenderRAIDA();

        if (!processSend(coinsPicked, tosn, envelope)) {
            sr.status = SenderResult.STATUS_ERROR;
            return;
        }

        sr.status = SenderResult.STATUS_FINISHED;
    }

    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }

    private void moveCoins(ArrayList<CloudCoin> ccs) {
        int passed, failed;

        for (CloudCoin cc : ccs) {
            String ccFile = cc.originalFile;
            passed = failed = 0;
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                    passed++;
                else if (cc.getDetectStatus(i) == CloudCoin.STATUS_FAIL)
                    failed++;
            }

            logger.info(ltag, "Doing " + cc.originalFile + " pass="+passed + " f="+failed);
            if (passed >= Config.PASS_THRESHOLD) {
                logger.info(ltag, "Moving to Sent: " + cc.sn);
                //AppCore.moveToFolder(cc.originalFile, Config.DIR_SENT);
            } else if (failed > 0) {
                if (failed >= RAIDA.TOTAL_RAIDA_COUNT - Config.PASS_THRESHOLD) {
                    logger.info(ltag, "Moving to Counterfeit: " + cc.sn);
                    //AppCore.moveToFolder(cc.originalFile, Config.DIR_COUNTERFEIT);
                } else {
                    logger.info(ltag, "Moving to Fracked: " + cc.sn);
                    //AppCore.moveToFolder(cc.originalFile, Config.DIR_FRACKED);
                }
            }

        }
    }


    public boolean processSend(ArrayList<CloudCoin> ccs, int tosn, String envelope) {
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
            requests[i] = "send";
            sbs[i] = new StringBuilder();
        }

        for (CloudCoin cc : ccs) {
            for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                if (!first) {
                    sbs[i].append("&");
                } else {
                    sbs[i].append("to_sn=");
                    sbs[i].append(tosn);
                    sbs[i].append("&envelope=");
                    sbs[i].append(URLEncoder.encode(envelope));
                    sbs[i].append("&");
                }

                sbs[i].append("nns[]=");
                sbs[i].append(cc.nn);

                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);

                sbs[i].append("&denomination[]=");
                sbs[i].append(cc.getDenomination());

                sbs[i].append("&ans[]=");
                sbs[i].append(cc.ans[i]);
            }

            first = false;
        }

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                //globalResult.totalRAIDAProcessed++;
                //if (myCb != null) {
                //    AuthenticatorResult ar = new AuthenticatorResult();
                //    copyFromGlobalResult(ar);
                //    myCb.callback(ar);
                //}
            }
        });

        if (results == null) {
            logger.error(ltag, "Failed to query send");
            return false;
        }

        CommonResponse errorResponse;
        SenderResponse[][] ar;
        Object[] o;

        ar = new SenderResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_UNTRIED);
                    continue;
                }
            }

            o = parseArrayResponse(results[i], SenderResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }

            for (int j = 0; j < o.length; j++) {
                String strStatus;
                int status;

                ar[i] = new SenderResponse[o.length];
                ar[i][j] = (SenderResponse) o[j];

                strStatus = ar[i][j].status;

                if (strStatus.equals("pass")) {
                    status = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals("fail")) {
                    status = CloudCoin.STATUS_FAIL;
                } else {
                    status = CloudCoin.STATUS_ERROR;
                    logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                }

                ccs.get(j).setDetectStatus(i, status);
                logger.info(ltag, "raida" + i + " v=" + ar[i][j].status + " m="+ar[i][j].message);
            }
        }

        moveCoins(ccs);

        return true;
    }

    private boolean collectedEnough(int[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != valuesPicked[i]) {
                return false;
            }
        }

        return true;
    }

    private void pickCoin(int idx, int[] values, CloudCoin cc) {
        if (values[idx] > valuesPicked[idx]) {
            logger.debug(ltag, "Picking coin " + cc.sn);

            valuesPicked[idx]++;
            coinsPicked.add(cc);
        }
    }

    public boolean pickCoinsInDir(String dir, int[] values) {
        logger.debug(ltag, "Looking into dir: " + dir);

        CloudCoin cc;
        int denomination;

        File dirObj = new File(dir);
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                continue;
            }

            denomination = cc.getDenomination();
            if (denomination == 1) {
                pickCoin(Config.IDX_1, values, cc);
            } else if (denomination == 5) {
                pickCoin(Config.IDX_5, values, cc);
            } else if (denomination == 25) {
                pickCoin(Config.IDX_25, values, cc);
            } else if (denomination == 100) {
                pickCoin(Config.IDX_100, values, cc);
            } else if (denomination == 250) {
                pickCoin(Config.IDX_250, values, cc);
            }

            if (collectedEnough(values)) {
                logger.debug(ltag, "Collected enough. Stop");
                return true;
            }
        }

        return false;
    }
}
