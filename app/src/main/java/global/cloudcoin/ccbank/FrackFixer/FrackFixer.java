package global.cloudcoin.ccbank.FrackFixer;

import org.json.JSONException;

import java.io.File;

import global.cloudcoin.ccbank.Grader.GraderResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class FrackFixer extends Servant {
    String ltag = "FrackFixer";
    FrackFixerResult fr;

    int[][] trustedServers;

    public FrackFixer(String rootDir, GLogger logger) {
        super("FrackFixer", rootDir, logger);


    }

    private int getNeightbour(int raidaIdx, int offset) {
        int result = raidaIdx + offset;

        if (result < 0)
            result += RAIDA.TOTAL_RAIDA_COUNT;

        if (result >= RAIDA.TOTAL_RAIDA_COUNT)
            result -= RAIDA.TOTAL_RAIDA_COUNT;

        return result;

    }

    public void initNeighbours() {
        int sideSize = (int) Math.sqrt(RAIDA.TOTAL_RAIDA_COUNT);
        int raidaNumber = 1;

        trustedServers = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            trustedServers[i] = new int[8];

            trustedServers[i][0] = getNeightbour(i, -6);
            trustedServers[i][1] = getNeightbour(i, -5);
            trustedServers[i][2] = getNeightbour(i, -4);
            trustedServers[i][3] = getNeightbour(i, -1);
            trustedServers[i][4] = getNeightbour(i, 1);
            trustedServers[i][5] = getNeightbour(i, 4);
            trustedServers[i][6] = getNeightbour(i, 5);
            trustedServers[i][7] = getNeightbour(i, 6);
        }
        logger.error(ltag, "sq="+sideSize);
   /*     trustedServers = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            switch (raidaNumber) {
                case 0: trustedServers = new int[] { 19, 20, 21, 24,  1,  4,  5,  6 }; break;
                case 1: trustedServers = new int[] { 20, 21, 22,  0,  2,  5,  6,  7 }; break;
                case 2: trustedServers = new int[] { 21, 22, 23,  1,  3,  6,  7,  8 }; break;
                case 3: trustedServers = new int[] { 22, 23, 24,  2,  4,  7,  8,  9 }; break;
                case 4: trustedServers = new int[] { 23, 24,  0,  3,  5,  8,  9, 10 }; break;
                case 5: trustedServers = new int[] { 24,  0,  1,  4,  6,  9, 10, 11 }; break;
                case 6: trustedServers = new int[] {  0,  1,  2,  5,  7, 10, 11, 12 }; break;
                case 7: trustedServers = new int[] {  1,  2,  3,  6,  8, 11, 12, 13 }; break;
                case 8: trustedServers = new int[] {  2,  3,  4,  7,  9, 12, 13, 14 }; break;
                case 9: trustedServers = new int[] {  3,  4,  5,  8, 10, 13, 14, 15 }; break;
                case 10: trustedServers = new int[] {  4,  5,  6,  9, 11, 14, 15, 16 }; break;
                case 11: trustedServers = new int[] {  5,  6,  7, 10, 12, 15, 16, 17 }; break;
                case 12: trustedServers = new int[] {  6,  7,  8, 11, 13, 16, 17, 18 }; break;
                case 13: trustedServers = new int[] {  7,  8,  9, 12, 14, 17, 18, 19 }; break;
                case 14: trustedServers = new int[] {  8,  9, 10, 13, 15, 18, 19, 20 }; break;
                case 15: trustedServers = new int[] {  9, 10, 11, 14, 16, 19, 20, 21 }; break;
                case 16: trustedServers = new int[] { 10, 11, 12, 15, 17, 20, 21, 22 }; break;
                case 17: trustedServers = new int[] { 11, 12, 13, 16, 18, 21, 22, 23 }; break;
                case 18: trustedServers = new int[] { 12, 13, 14, 17, 19, 22, 23, 24 }; break;
                case 19: trustedServers = new int[] { 13, 14, 15, 18, 20, 23, 24,  0 }; break;
                case 20: trustedServers = new int[] { 14, 15, 16, 19, 21, 24,  0,  1 }; break;
                case 21: trustedServers = new int[] { 15, 16, 17, 20, 22,  0,  1,  2 }; break;
                case 22: trustedServers = new int[] { 16, 17, 18, 21, 23,  1,  2,  3 }; break;
                case 23: trustedServers = new int[] { 17, 18, 19, 22, 24,  2,  3,  4 }; break;
                case 24: trustedServers = new int[] { 18, 19, 20, 23,  0,  3,  4,  5 }; break;
            }
        }
        */
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        fr = new FrackFixerResult();
        initNeighbours();

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN FrackFixer");
                doFrackFix();

                if (cb != null)
                    cb.callback(fr);
            }
        });
    }

    public void copyFromMainFr(FrackFixerResult nfr) {
        nfr.failed = fr.failed;
        nfr.fixed = fr.fixed;
    }

    public void doFrackFix() {
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            fr.status = FrackFixerResult.STATUS_ERROR;
            if (cb != null)
                cb.callback(fr);

            return;
        }

        String fullPath = AppCore.getUserDir(Config.DIR_FRACKED);
        CloudCoin cc;


        File dirObj = new File(fullPath);
        for (File file : dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                fr.failed++;
                continue;
            }

            logger.info(ltag, "doing cc=" + cc.sn);
            doFixCoin(cc);

            FrackFixerResult nfr = new FrackFixerResult();
            copyFromMainFr(nfr);
            if (cb != null)
                cb.callback(nfr);

        }
    }

    public void doFixCoin(CloudCoin cc) {

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (cc.getDetectStatus(i) != CloudCoin.STATUS_PASS) {
                logger.debug(ltag, "Fixing cc " + cc.sn + " on RAIDA" + i);
            }
        }


    }

/*
    private void getCorner(int raidaNumber ) {
        int[][] trustedServers;
        int[] trustedTriad1, trustedTriad2, trustedTriad3, trustedTriad4;



        trustedTriad1 = new int[]{trustedServers[0] , trustedServers[1] , trustedServers[3] };
        trustedTriad2 = new int[]{trustedServers[1] , trustedServers[2] , trustedServers[4] };
        trustedTriad3 = new int[]{trustedServers[3] , trustedServers[5] , trustedServers[6] };
        trustedTriad4 = new int[]{trustedServers[4] , trustedServers[6] , trustedServers[7] };
    }
*/

}
