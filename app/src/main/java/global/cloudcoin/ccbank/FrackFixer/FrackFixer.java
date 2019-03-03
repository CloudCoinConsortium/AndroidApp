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

    int triadSize;

    private int[][] trustedServers;
    private int[][][] trustedTriads;

    public FrackFixer(String rootDir, GLogger logger) {
        super("FrackFixer", rootDir, logger);
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        fr = new FrackFixerResult();
        launchDetachedThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN (Detached) FrackFixer");
                doFrackFix();
            }
        });
    }

    private int getNeightbour(int raidaIdx, int offset) {
        int result = raidaIdx + offset;

        if (result < 0)
            result += RAIDA.TOTAL_RAIDA_COUNT;

        if (result >= RAIDA.TOTAL_RAIDA_COUNT)
            result -= RAIDA.TOTAL_RAIDA_COUNT;

        return result;

    }

    public boolean initNeighbours() {
        int sideSize;

        sideSize= (int) Math.sqrt(RAIDA.TOTAL_RAIDA_COUNT);
        if (sideSize * sideSize != RAIDA.TOTAL_RAIDA_COUNT) {
            logger.error(ltag, "Wrong RAIDA configuration");
            return false;
        }

        trustedServers = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        trustedTriads = new int[RAIDA.TOTAL_RAIDA_COUNT][][];

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            trustedServers[i] = new int[8];

            trustedServers[i][0] = getNeightbour(i, -sideSize - 1);
            trustedServers[i][1] = getNeightbour(i, -sideSize);
            trustedServers[i][2] = getNeightbour(i, -sideSize + 1);
            trustedServers[i][3] = getNeightbour(i, -1);
            trustedServers[i][4] = getNeightbour(i, 1);
            trustedServers[i][5] = getNeightbour(i, sideSize - 1);
            trustedServers[i][6] = getNeightbour(i, sideSize);
            trustedServers[i][7] = getNeightbour(i, sideSize + 1);

            trustedTriads[i] = new int[4][];
            trustedTriads[i][0] = new int[] { trustedServers[i][0], trustedServers[i][1], trustedServers[i][3] };
            trustedTriads[i][1] = new int[] { trustedServers[i][1], trustedServers[i][2], trustedServers[i][4] };
            trustedTriads[i][2] = new int[] { trustedServers[i][3], trustedServers[i][5], trustedServers[i][6] };
            trustedTriads[i][3] = new int[] { trustedServers[i][4], trustedServers[i][6], trustedServers[i][7] };

        }

        triadSize = trustedTriads[0][0].length;

        return true;
    }

    public void copyFromMainFr(FrackFixerResult nfr) {
        nfr.failed = fr.failed;
        nfr.fixed = fr.fixed;
    }

    public void doFrackFix() {
        if (!initNeighbours()) {
            fr.status = FrackFixerResult.STATUS_ERROR;
            if (cb != null)
                cb.callback(fr);

            return;
        }

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

        FrackFixerResult nfr = new FrackFixerResult();
        fr.status = FrackFixerResult.STATUS_FINISHED;
        copyFromMainFr(nfr);
        if (cb != null)
            cb.callback(nfr);
    }

    private void syncCoin(int raidaIdx, CloudCoin cc) {
        logger.info(ltag, "xx=" + cc.originalFile);

        cc.setDetectStatus(raidaIdx, CloudCoin.STATUS_PASS);
        cc.setPownStringFromDetectStatus();
        cc.calcExpirationDate();

        AppCore.deleteFile(cc.originalFile);
        if (!AppCore.saveFile(cc.originalFile, cc.getJson(false))) {
            logger.error(ltag, "Failed to save file: " + cc.originalFile);
            logger.error(ltag, "Coin details: " + cc.getJson());
            return;
        }

        logger.info(ltag, "saved");

    }

    public void doFixCoin(CloudCoin cc) {
        int corner, i;

        logger.debug(ltag, "Round1 for cc " + cc.sn);
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                continue;

            logger.debug(ltag, "Fixing cc " + cc.sn + " on RAIDA" + i);
            for (corner = 0; corner < 4; corner++) {
                logger.debug(ltag, "corner=" + corner);

                if (fixCoinInCorner(i, corner, cc)) {
                    logger.debug(ltag, "Fixed successfully");
                    syncCoin(i, cc);
                    break;
                }
            }
        }

        logger.debug(ltag, "Round2 for cc " + cc.sn);
        for (i = RAIDA.TOTAL_RAIDA_COUNT - 1; i >= 0; i--) {
            if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                continue;

            logger.debug(ltag, "Fixing cc " + cc.sn + " on RAIDA" + i);
            for (corner = 0; corner < 4; corner++) {
                logger.debug(ltag, "corner=" + corner);

                if (fixCoinInCorner(i, corner, cc)) {
                    logger.debug(ltag, "Fixed successfully");
                    syncCoin(i, cc);
                    break;
                }
            }
        }

        int cnt = 0;
        for (i = RAIDA.TOTAL_RAIDA_COUNT - 1; i >= 0; i--) {
            if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                cnt++;
        }

        if (cnt == RAIDA.TOTAL_RAIDA_COUNT) {
            logger.info(ltag, "Coin " + cc.sn + " is fixed. Moving to bank");
            AppCore.moveToBank(cc.originalFile);
            fr.fixed++;
            return;
        }

        fr.failed++;
        return;
    }

    public boolean fixCoinInCorner(int raidaIdx, int corner, CloudCoin cc) {
        int[] triad;
        int[] raidaFix;
        int neighIdx;

        String[] results;
        String[] requests;
        GetTicketResponse[] gtr;

        if (raida.isFailed(raidaIdx)) {
            logger.error(ltag, "RAIDA " + raidaIdx + " is failed. Skipping it");
            return false;
        }

        raidaFix = new int[1];
        raidaFix[0] = raidaIdx;

        requests = new String[triadSize];
        triad = trustedTriads[raidaIdx][corner];
        for (int i = 0; i < triadSize; i++) {
            neighIdx = triad[i];

            logger.debug(ltag, "Checking neighbour: " + neighIdx);
            if (raida.isFailed(neighIdx)) {
                logger.error(ltag, "Neighbour " + neighIdx + " is unavailable. Skipping it");
                return false;
            }

            if (!cc.ans[neighIdx].equals(cc.pans[neighIdx])) {
                logger.error(ltag, "AN&PAN mismatch. The coin can't be fixed: " + i);
                return false;
            }

            requests[i] = "get_ticket?nn=" + cc.nn + "&sn=" + cc.sn + "&an=" + cc.ans[neighIdx] +
                    "&pan=" + cc.pans[neighIdx] + "&denomination=" + cc.getDenomination();
        }

        results = raida.query(requests, null, null, triad);
        if (results == null) {
            logger.error(ltag, "Failed to get tickets. Setting triad to failed");
            for (int i = 0; i < triadSize; i++)
                raida.setFailed(triad[i]);

            return false;
        }

        if (results.length != triadSize) {
            logger.error(ltag, "Invalid response size: " + results.length);
            for (int i = 0; i < triadSize; i++)
                raida.setFailed(triad[i]);

            return false;
        }

        requests = new String[1];
        requests[0] = "fix?";

        gtr = new GetTicketResponse[triadSize];
        for (int i = 0; i < results.length; i++) {
            logger.info(ltag, "res=" + results[i]);

            gtr[i] = (GetTicketResponse) parseResponse(results[i], GetTicketResponse.class);
            if (gtr[i] == null) {
                logger.error(ltag, "Failed to parse response from: " + triad[i]);
                raida.setFailed(triad[i]);
                return false;
            }

            if (!gtr[i].status.equals("ticket")) {
                logger.error(ltag, "Failed to get ticket from RAIDA" + triad[i]);
                raida.setFailed(triad[i]);
                return false;
            }

            if (gtr[i].message == null || gtr[i].message.length() != 44) {
                logger.error(ltag, "Invalid ticket from RAIDA" + triad[i]);
                raida.setFailed(triad[i]);
                return false;
            }

            if (i != 0)
                requests[0] += "&";

            requests[0] += "fromserver" + (i + 1) + "=" + triad[i] + "&message" + (i + 1) + "=" + gtr[i].message;

        }

        requests[0] += "&pan=" + cc.ans[raidaIdx];

        logger.debug(ltag, "Doing actual fix on RAIDA" + raidaIdx);

        results = raida.query(requests, null, null, raidaFix);
        if (results == null) {
            logger.error(ltag, "Failed to fix on RAIDA" + raidaIdx);
            raida.setFailed(raidaIdx);
            return false;
        }

        FixResponse fresp = (FixResponse) parseResponse(results[0], FixResponse.class);
        if (fresp == null) {
            logger.error(ltag, "Failed to parse fix response" + raidaIdx);
            raida.setFailed(raidaIdx);
            return false;
        }

        if (!fresp.status.equals("success")) {
            logger.error(ltag, "Failed to fix on RAIDA" + raidaIdx + ": " + fresp.message);
            raida.setFailed(raidaIdx);
            return false;
        }

        logger.debug(ltag, "Fixed on RAIDA" + raidaIdx);

        return true;
    }

}
