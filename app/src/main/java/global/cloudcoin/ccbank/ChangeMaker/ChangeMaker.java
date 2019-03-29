package global.cloudcoin.ccbank.ChangeMaker;

import org.json.JSONException;

import java.util.ArrayList;

import global.cloudcoin.ccbank.ChangeMaker.ChangeMakerResult;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoins;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoinsResponse;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.Servant;

public class ChangeMaker extends Servant {
    String ltag = "ChangeMaker";
    ChangeMakerResult cr;


    public ChangeMaker(String rootDir, GLogger logger) {
        super("ChangeMaker", rootDir, logger);
    }

    public void launch(String user, int method, CallbackInterface icb) {
        this.cb = icb;

        final String fuser = user;
        final int fmethod = method;


        cr = new ChangeMakerResult();


        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Sender");
                doSend(fuser, fmethod);


                if (cb != null)
                    cb.callback(cr);
            }
        });
    }

    public void doSend(String user, int method) {
        logger.info(ltag, "Method " + method);


        String resultMain;
        CloudCoin cc;
        String[] results;
        Object[] o;
        CommonResponse cresponse;
        ShowEnvelopeCoinsResponse[] srs;

        setSenderRAIDA();

/*
        results = raida.query(new String[] { "show_change" }, null, null, new int[] {Config.RAIDANUM_TO_QUERY_BY_DEFAULT});
        if (results == null) {
            logger.error(ltag, "Failed to query showchange");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

        resultMain = results[0];
        if (resultMain == null) {
            logger.error(ltag, "Failed to query showchange");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }
        */

        resultMain = "{\n" +
                "  \"server\":\"RAIDA1\",\n" +
                "  \"status\":\"shown\",\n" +
                "  \"s250\":[16230602,16675880,16192311,15169770],\n" +
                "  \"s100\":[13230602,13675880,16192311,15169770],\n" +
                "  \"s25\":[10230602,10675880,10192311,15169770,3,4,5,6,7,8],\n" +
                "  \"s5\":[8230602,8675880,6192311,15169770,1111,222,888,999,100,101,1,1,1,1,1,2,2,2,2,2,3,3,3,3,3,4,4,4,4,4,5,5,5,5,5,6,6,6,6,6,7,7,7,7,7],\n" +
                "  \"s1\":[230602,675880,192311,15169770,33,44,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50],\n" +
                "  \"message\":\"Change:This report shows the serial numbers that are available to make change now.\",\n" +
                "  \"version\":\"some version number here\",\n" +
                "  \"time\":\"2016-44-19 7:44:PM\"\n" +
                "}\n";

        cresponse = (CommonResponse) parseResponse(resultMain, CommonResponse.class);
        if (cresponse == null) {
            logger.error(ltag, "Failed to get response");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

        if (!cresponse.status.equals("shown")) {
            logger.error(ltag, "Failed to get response: " + cresponse.status);
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

        ShowChangeResponse scr = (ShowChangeResponse) parseResponse(resultMain, ShowChangeResponse.class);
        int[] sns;

        logger.info(ltag, "sss="+scr.s5[1]);

       // logger.info(ltag, "ss1s="+scr.s5[1] + " sns="+sns);
        String r;
        //try {
            switch (method) {
                case Config.CHANGE_METHOD_5A:
                    sns = getA(scr.s1, 5);
                    break;
                case Config.CHANGE_METHOD_25A:
                    sns = getA(scr.s5, 5);
                    break;
                case Config.CHANGE_METHOD_25B:
                    sns = get25B(scr.s5, scr.s1);
                    break;
                case Config.CHANGE_METHOD_25C:
                    sns = get25C(scr.s5, scr.s1);
                    break;
                case Config.CHANGE_METHOD_25D:
                    sns = get25D(scr.s1);
                    break;
                case Config.CHANGE_METHOD_100A:
                    sns = getA(scr.s25, 4);
                    break;
                case Config.CHANGE_METHOD_100B:
                    sns = get100B(scr.s25, scr.s5);
                    break;
                case Config.CHANGE_METHOD_100C:
                    sns = get100C(scr.s25, scr.s5, scr.s1);
                    break;
                case Config.CHANGE_METHOD_100D:
                    sns = get100D(scr.s1);
                    break;
                case Config.CHANGE_METHOD_250A:
                    sns = get250A(scr.s100, scr.s25);
                    break;
                case Config.CHANGE_METHOD_250B:
                    sns = get250B(scr.s25, scr.s5, scr.s1);
                    break;
                case Config.CHANGE_METHOD_250C:
                    sns = get250C(scr.s25, scr.s5, scr.s1);
                    break;
                case Config.CHANGE_METHOD_250D:
                    sns = get250D(scr.s1);
                    break;
                default:
                    logger.error(ltag, "Invalid method: " + method);
                    cr.status = ChangeMakerResult.STATUS_ERROR;
                    return;
            }
        /*} catch (JSONException e) {
            logger.error(ltag, "JSON failed: " + e.getMessage());
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }*/

        if (sns == null) {
            logger.info(ltag, "No coins");
            cr.status = ChangeMakerResult.STATUS_ERROR;
       //     return;
        }

        for (int i = 0; i < sns.length; i++) {
            logger.info(ltag, "sn="+sns[i]);
        }

        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Failed to query RAIDA");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

    }

    private int[] getA(int[] a, int cnt) {
        int[] sns;
        int i, j;

        sns = new int[cnt];
        for (i = 0, j = 0; i < a.length; i++) {
            if (a[i] == 0)
                continue;

            sns[j] = a[i];
            a[i] = 0;
            j++;

            if (j == cnt)
                break;
        }

        if (j != cnt)
            return null;

        return sns;
    }

    private int[] get25B(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[9];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i] = sns[i];

        sns = getA(sb, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 5] = sns[i];

        return rsns;
    }

    private int[] get25C(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[17];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 5] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 10] = sns[i];

        sns = getA(sb, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i + 15] = sns[i];

        return rsns;
    }

    private int[] get25D(int[] sb) {
        int[] sns, rsns;
        int j;

        rsns = new int[25];
        for (j = 0; j < 5; j++) {
            sns = getA(sb, 5);
            if (sns == null)
                return null;

            for (int i = 0; i < 5; i++)
                rsns[j * 5 + i] = sns[i];
        }

        return rsns;
    }

    private int[] get100B(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[8];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i] = sns[i];

        sns = getA(sb, 3);
        if (sns == null)
            return null;

        for (int i = 0; i < 3; i++)
            rsns[i + 5] = sns[i];

        return rsns;
    }

    private int[] get100C(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[16];

        sns = get25B(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 9; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 9] = sns[i];

        sns = getA(sb, 2);
        for (int i = 0; i < 2; i++)
            rsns[i + 14] = sns[i];

        return rsns;
    }

    private int[] get100D(int[] sb) {
        int[] sns, rsns;
        int j;

        rsns = new int[100];
        for (j = 0; j < 4; j++) {
            sns = get25D(sb);
            if (sns == null)
                return null;

            for (int i = 0; i < 25; i++)
                rsns[j * 25 + i] = sns[i];
        }

        return rsns;
    }

    private int[] get250A(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[4];

        sns = getA(ss, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i] = sns[i];

        sns = getA(sb, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i + 2] = sns[i];

        return rsns;
    }

    private int[] get250B(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[22];
        sns = get25B(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 9; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 9] = sns[i];

        sns = getA(sb, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 14] = sns[i];

        sns = getA(sb, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 18] = sns[i];

        return rsns;
    }

    private int[] get250C(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[42];
        sns = get25C(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 17; i++)
            rsns[i] = sns[i];

        sns = get25B(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 9; i++)
            rsns[i + 17] = sns[i];

        sns = get100B(sb, ss);
        if (sns == null)
            return null;

        for (int i = 0; i < 8; i++)
            rsns[i + 26] = sns[i];

        sns = get100B(sb, ss);
        if (sns == null)
            return null;

        for (int i = 0; i < 8; i++)
            rsns[i + 34] = sns[i];

        return rsns;
    }

    private int[] get250D(int[] sb) {
        int[] sns, rsns;

        rsns = new int[250];

        sns = get25D(sb);
        if (sns == null)
           return null;

        for (int i = 0; i < 25; i++)
           rsns[i] = sns[i];

        sns = get25D(sb);
        if (sns == null)
            return null;

        for (int i = 0; i < 25; i++)
            rsns[i + 25] = sns[i];


        sns = get100D(sb);
        if (sns == null)
            return null;

        for (int i = 0; i < 100; i++)
            rsns[i + 50] = sns[i];

        sns = get100D(sb);
        if (sns == null)
            return null;

        for (int i = 0; i < 100; i++)
            rsns[i + 150] = sns[i];

        return rsns;
    }
}
