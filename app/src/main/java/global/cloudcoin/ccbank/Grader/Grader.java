package global.cloudcoin.ccbank.Grader;


import org.json.JSONException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class Grader extends Servant {
    String ltag = "Grader";
    GraderResult gr;
    StringBuilder sb;
    String receiptId;

    public Grader(String rootDir, GLogger logger) {
        super("Grader", rootDir, logger);


    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        gr = new GraderResult();
        sb = new StringBuilder();

        receiptId = AppCore.generateHex();

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Grader");
                doGrade();

                if (cb != null)
                    cb.callback(gr);
            }
        });
    }

    public void doGrade() {
        String fullPath = AppCore.getUserDir(Config.DIR_DETECTED);
        CloudCoin cc;
        boolean graded = false;

        File dirObj = new File(fullPath);
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            graded = true;

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                gr.totalUnchecked++;
                continue;
            }

            gradeCC(cc);
        }

        if (graded)
            saveReceipt();
    }

    public void saveReceipt() {
        StringBuilder rsb = new StringBuilder();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:a");
        SimpleDateFormat formatterTz = new SimpleDateFormat("z");
        Date date = new Date(System.currentTimeMillis());

        String cdate = formatter.format(date);
        String cdateFormat = formatterTz.format(date);

        rsb.append("{\"receipt_id\": \"");
        rsb.append(receiptId);
        rsb.append("\", \"time\": \"");
        rsb.append(cdate);
        rsb.append("\", \"timezone\": \"");
        rsb.append(cdateFormat);
        rsb.append("\", \"total_authentic\": ");
        rsb.append(gr.totalAuthentic);
        rsb.append(", \"total_fracked\": ");
        rsb.append(gr.totalFracked);
        rsb.append(", \"total_counterfeit\": ");
        rsb.append(gr.totalCounterfeit);
        rsb.append(", \"total_lost\": ");
        rsb.append(gr.totalLost);
        rsb.append(", \"total_unchecked\": ");
        rsb.append(gr.totalUnchecked);
        rsb.append(", \"receipt_detail\": [");
        rsb.append(sb);
        rsb.append("]}");

        String fileName = AppCore.getUserDir(Config.DIR_RECEIPTS) + File.separator + receiptId + ".txt";
        if (!AppCore.saveFile(fileName, rsb.toString())) {
            logger.error(ltag, "Failed to save file " + fileName);
        }
    }

    public void addCoinToReceipt(CloudCoin cc, String status, String dstFolder) {
        if (!sb.toString().equals(""))
            sb.append(",");

        sb.append("{\"nn.sn\": \"");
        sb.append(cc.nn);
        sb.append(".");
        sb.append(cc.sn);
        sb.append("\", \"status\": \"");
        sb.append(status);
        sb.append("\", \"pown\": \"");
        sb.append(cc.getPownString());
        sb.append("\", \"note\": \"Moved to ");
        sb.append(dstFolder);
        sb.append("\"}");
    }

    public void gradeCC(CloudCoin cc) {
        String dstFolder;
        int untried, counterfeit, passed, error;

        untried = counterfeit = passed = error = 0;
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            switch (cc.getDetectStatus(i)) {
                case CloudCoin.STATUS_ERROR:
                    error++;
                    break;
                case CloudCoin.STATUS_FAIL:
                    counterfeit++;
                    break;
                case CloudCoin.STATUS_UNTRIED:
                    untried++;
                    break;
                case CloudCoin.STATUS_PASS:
                    passed++;
                    break;
            }
        }

        boolean includePans = false;
        String ccFile;

        if (passed >= Config.PASS_THRESHOLD) {
            if (passed != RAIDA.TOTAL_RAIDA_COUNT) {
                logger.debug(ltag, "Coin " + cc.sn + " is fracked");

                gr.totalFracked++;
                gr.totalFrackedValue += cc.getDenomination();
                dstFolder = Config.DIR_FRACKED;

                addCoinToReceipt(cc, "fracked", dstFolder);
            } else {
                logger.debug(ltag, "Coin " + cc.sn + " is authentic");

                gr.totalAuthentic++;
                gr.totalAuthenticValue += cc.getDenomination();
                dstFolder = Config.DIR_BANK;

                addCoinToReceipt(cc, "authentic", dstFolder);
            }

            cc.calcExpirationDate();

            ccFile = AppCore.getUserDir(dstFolder) + File.separator + cc.getFileName();
        } else {
            if (passed + counterfeit > Config.PASS_THRESHOLD) {
                logger.debug(ltag, "Coin " + cc.sn + " is counterfeit");

                gr.totalCounterfeit++;
                dstFolder = Config.DIR_COUNTERFEIT;

                addCoinToReceipt(cc, "counterfeit", dstFolder);
            } else {
                logger.debug(ltag, "Coin " + cc.sn + " is lost");

                gr.totalLost++;
                dstFolder = Config.DIR_LOST;

                addCoinToReceipt(cc, "lost", dstFolder);
                includePans = true;
            }

            ccFile = AppCore.getUserDir(dstFolder) + File.separator +
                + System.currentTimeMillis() + "-" + cc.getFileName();
        }

        cc.setAnsToPansIfPassed();

        logger.info(ltag, "Saving grader coin: " + ccFile + " include=" + includePans);
        if (!AppCore.saveFile(ccFile, cc.getJson(includePans))) {
            logger.error(ltag, "Failed to save file: " + ccFile);
            return;
        }

        AppCore.deleteFile(cc.originalFile);
    }
}
