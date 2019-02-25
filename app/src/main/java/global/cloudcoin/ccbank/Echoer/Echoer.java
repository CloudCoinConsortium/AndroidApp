package global.cloudcoin.ccbank.Echoer;


import android.util.Log;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import global.cloudcoin.ccbank.core.GLogger;


public class Echoer extends Servant {

    protected CallbackInterface cb;

    String ltag = "Echoer";
    EchoResponse[] ers;
    long[] latencies;



    public Echoer(String rootDir, GLogger logger) {
        super("Echoer", rootDir, logger);

        ers = new EchoResponse[RAIDA.TOTAL_RAIDA_COUNT];
        latencies = new long[RAIDA.TOTAL_RAIDA_COUNT];
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Echoer");
                doEcho();
                cb.callback(null);
            }
        });
    }

    private void setRAIDAUrl(String ip, int basePort) {
        raida.setUrl(ip, basePort);
    }

    public void doEcho() {
        if (!doEchoReal()) {
            logger.info(ltag, "Switching to the Backup0 RAIDA");

            setRAIDAUrl(Config.BACKUP0_RAIDA_IP, Config.BACKUP0_RAIDA_PORT);
            if (!doEchoReal()) {
                logger.info(ltag, "Switching to the Backup1 RAIDA");

                setRAIDAUrl(Config.BACKUP1_RAIDA_IP, Config.BACKUP1_RAIDA_PORT);
                if (!doEchoReal()) {
                    logger.info(ltag, "Switching to the Backup2 RAIDA");

                    setRAIDAUrl(Config.BACKUP2_RAIDA_IP, Config.BACKUP2_RAIDA_PORT);
                    if (!doEchoReal()) {
                        logger.error(ltag, "All attempts failed. Giving up");
                    }
                }
            }
        }

        saveResults();
    }

    public boolean doEchoReal() {
        String[] results;
        String[] requests;

        int cntErr = 0;
        int i;

        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++)
            requests[i] = "echo";

        results = raida.query(requests);
        if (results == null) {
            logger.error(ltag, "Failed to query echo");
            return false;
        }

        latencies = raida.getLastLatencies();

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            ers[i] = null;
        }

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "RAIDA " + i + ": " + results[i] + " latency=" + latencies[i]);
            if (results[i] == null) {
                logger.error(ltag, "Failed to get result. RAIDA " + i + " is not ready");
                cntErr++;
                continue;
            }

            ers[i] = (EchoResponse) parseResponse(results[i], EchoResponse.class);

            if (!ers[i].status.equals(Config.RAIDA_STATUS_READY)) {
                logger.error(ltag, "RAIDA " + i + " is not ready");
                cntErr++;
            }

        }

        if (cntErr >= Config.MAX_ALLOWED_FAILED_RAIDAS) {
            logger.debug(ltag, "Failed: " + cntErr);

            return false;
        }

        return true;
    }



    private boolean saveResults() {
        String status, message;

        long latency;
        String intLatency;
        String data;

        String[] raidaURLs;

        raidaURLs = raida.getRAIDAURLs();

        cleanLogDir();

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (ers[i] == null) {
                status = "notready";
                latency = 0;
                intLatency = "U";
            } else {
                status = ers[i].status;
                latency = latencies[i];
                intLatency = ers[i].message.replaceAll("Execution Time.*=\\s*(.+)", "$1");
            }

            String fileName = i + "_" + status + "_" + latency + "_" + intLatency + ".txt";

            data = "{\"url\":\"" + raidaURLs[i] + "\"}";

            if (!writeLog(fileName, data)) {
                logger.error(ltag, "Failed to write echoer logfile: " + fileName);
                continue;
            }

            logger.info(ltag, "f="+fileName+ " d="+data);
        }

        return true;
    }

    private boolean writeLog(String fileIn, String data) {
        String file = logDir + File.separator + fileIn;
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            stream.write(data.getBytes());
            stream.close();
        } catch (FileNotFoundException e) {
            logger.error("ltag", "File Not Found");
            return false;
        } catch (IOException e) {
            Log.e(ltag, "Failed to write file: " + e.getMessage());
            return false;
        }

        return true;
    }

}
