package global.cloudcoin.ccbank.ShowEnvelopeCoins;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import global.cloudcoin.ccbank.ShowCoins.ShowCoinsResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class ShowEnvelopeCoins extends Servant {
    String ltag = "ShowEnvelopeCoins";

    ShowEnvelopeCoinsResult result;

    public ShowEnvelopeCoins(String rootDir, GLogger logger) {
        super("ShowEnvelopeCoins", rootDir, logger);

        result = new ShowEnvelopeCoinsResult();
        result.coins = new int[0];

    }

    public void launch(String user, int sn, String envelope, CallbackInterface icb) {
        this.cb = icb;

        final String fuser = user;
        final int fsn = sn;
        final String fenvelope = envelope;

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN ShowEnvelopeCoins");
                doShowEnvelopeCoins(fuser, fsn, fenvelope);


                if (cb != null)
                    cb.callback(result);
            }
        });
    }

    public void doShowEnvelopeCoins(String user, int sn, String envelope) {
        CloudCoin cc;
        String[] results;
        Object[] o;
        CommonResponse errorResponse;
        ShowEnvelopeCoinsResponse[] srs;

        setSenderRAIDA();
        cc = getIDcc(user, sn);
        if (cc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("showcoinsinenvelope?nn=");
        sb.append(cc.nn);
        sb.append("&sn=");
        sb.append(cc.sn);
        sb.append("&an=");
        sb.append(cc.ans[Config.RAIDANUM_TO_QUERY_BY_DEFAULT]);
        sb.append("&denomination=");
        sb.append(cc.getDenomination());
        sb.append("&envelope_name=");
        sb.append(URLEncoder.encode(envelope));

        results = raida.query(new String[] { sb.toString() }, null, null, new int[] {Config.RAIDANUM_TO_QUERY_BY_DEFAULT});
        if (results == null) {
            logger.error(ltag, "Failed to query showcoinsinenvelope");
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }

        String resultMain = results[0];
        if (resultMain == null) {
            logger.error(ltag, "Failed to query showcoinsinenvelope");
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }

        o = parseArrayResponse(resultMain, ShowEnvelopeCoinsResponse.class);
        if (o == null) {
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            errorResponse = (CommonResponse) parseResponse(resultMain, CommonResponse.class);
            if (errorResponse == null) {
                logger.error(ltag, "Failed to get error");
                return;
            }

            logger.error(ltag, "Error: " + errorResponse.status);
            return;
        }

        result.coins = new int[o.length];
        srs = new ShowEnvelopeCoinsResponse[o.length];
        for (int j = 0; j < o.length; j++) {
            String strStatus;

            srs[j] = (ShowEnvelopeCoinsResponse) o[j];
            strStatus = srs[j].status;
            if (!strStatus.equals("success")) {
                logger.info(ltag, "Warning: coin " + srs[j].sn + " is failed");
            }

            result.coins[j] = srs[j].sn;

        }

        createResultFile(envelope, sn, result.coins);
        result.status = ShowEnvelopeCoinsResult.STATUS_FINISHED;

        logger.info(ltag, "us=" + user + " sn=" +sn + " e="+envelope + " r="+results[0]);
    }

    public void createResultFile(String envelopeName, int sn, int[] sns) {
        String fileName = AppCore.getMD5(envelopeName) + "_" + sn + "_coins.txt";
        String path = privateLogDir + File.separator + fileName;

        logger.info(ltag, "Saving " + path);
        File file = new File(path);
        if (file.exists())
            file.delete();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sns.length; i++) {
            sb.append(sns[i]);
            sb.append("\n");
        }

        if (!AppCore.saveFile(path, sb.toString())) {
            logger.error(ltag, "Failed to save file: " + path);
            return;
        }

    }



}
