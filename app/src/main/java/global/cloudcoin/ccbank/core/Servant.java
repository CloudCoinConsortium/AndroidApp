package global.cloudcoin.ccbank.core;

import android.telecom.Call;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;


public class Servant {

    private String ltag = "Servant";

    final static int STATUS_RUNNING = 1;
    final static int STATUS_WAITING = 2;

    private String rootDir;
    private String name;
    private int status;

    protected RAIDA raida;

    protected GLogger logger;

    protected Config config;

    private Hashtable<String, String> configHT;

    protected String logDir, privateLogDir;

    protected CallbackInterface cb;

    protected Thread thread;

    public Servant(String name, String rootDir, GLogger logger) {
        this.name = name;
        this.rootDir = rootDir;
        this.status = STATUS_WAITING;
        this.logger = logger;
        this.config = null;

        configHT = new Hashtable<String, String>();

        AppCore.createDirectory(Config.DIR_MAIN_LOGS + File.separator + name);
        AppCore.createDirectory(Config.DIR_ACCOUNTS + File.separator +
                Config.DIR_DEFAULT_USER + File.separator + Config.DIR_LOGS + File.separator + name);

        this.logDir = AppCore.getLogDir() + File.separator + name;
        this.privateLogDir = AppCore.getPrivateLogDir() + File.separator + name;

        this.raida = new RAIDA(logger);

        logger.info(ltag, "Instantiated servant " + name);

        readConfig();
    }


    public void launchThread(Runnable runnable) {
        thread = new Thread(runnable);
        thread.start();
    }

    private boolean checkLatency(int latency, int intLatency) {
        if (latency + intLatency > Config.MAX_ALLOWED_LATENCY) {
            return false;
        }
        return true;
    }

    public boolean updateRAIDAStatus() {
        String[] urls;
        String echoerLogDIr = AppCore.getLogDir() + File.separator + "Echoer";
        String[] parts;
        int raidaNumber;
        String status;
        int latency, intLatency;
        int cntValid = 0;

        urls = new String[RAIDA.TOTAL_RAIDA_COUNT];

        File logDirObj = new File(logDir);
        for (File file : logDirObj.listFiles()) {
            if (!file.isDirectory()) {
                String fileName = file.getName();
                logger.debug(ltag, "Checking " + file);

                parts = fileName.toString().split("_");
                if (parts.length != 4) {
                    logger.error(ltag, "Invalid file, skip it: " + file);
                    continue;
                }

                try {
                    raidaNumber = Integer.parseInt(parts[0]);
                    status = parts[1];
                    latency = Integer.parseInt(parts[2]);

                    String[] sparts = parts[3].split("\\.");
                    intLatency = Integer.parseInt(sparts[0]);
                } catch (NumberFormatException e) {
                    logger.error(ltag, "Can't parse file name: " + fileName);
                    continue;
                }

                if (raidaNumber < 0 || raidaNumber > RAIDA.TOTAL_RAIDA_COUNT - 1) {
                    logger.error(ltag, "Invalid raida number: " + raidaNumber);
                    continue;
                }

                if (!status.equals(Config.RAIDA_STATUS_READY)) {
                    logger.error(ltag, "RAIDA" + raidaNumber + " is not ready. Skip it");
                    continue;
                }

                if (!checkLatency(latency, intLatency)) {
                    logger.error(ltag, "RAIDA" + raidaNumber + ". The latency is too high: " + latency + ", " + intLatency);
                    continue;
                }

                String url;
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String data = new String(bytes);

                    JSONObject o = new JSONObject(data);
                    url = o.getString("url");

                    logger.info(ltag, "urll=" + url);
                } catch (JSONException e) {
                    logger.error(ltag, "Failed to parse JSON " + fileName + ": " + e.getMessage());
                    continue;
                } catch (IOException e) {
                    logger.error(ltag, "Failed to read file " + fileName + ": " + e.getMessage());
                    continue;
                }

                urls[raidaNumber] = url;
                cntValid++;
            }
        }

        if (RAIDA.TOTAL_RAIDA_COUNT - cntValid > Config.MAX_ALLOWED_FAILED_RAIDAS) {
            logger.error(ltag, "Only " + cntValid + " raidas are online. Can't proceed");
            return false;
        }

        raida.setExactUrls(urls);

        return true;
    }

    private boolean readConfig() {
        String configFilename = AppCore.getConfigDir() + File.separator + "config.txt";
        byte[] data;
        String xmlData;

        File file = new File(configFilename);
        try {
            if (!file.exists()) {
                xmlData = getDefaultConfig();
                if (!file.createNewFile()) {
                    logger.error(ltag, "Failed to create config file");
                    return false;
                }
                PrintWriter out = new PrintWriter(configFilename);
                out.print(xmlData);
                out.close();
            } else {
                data = Files.readAllBytes(Paths.get(configFilename));
                xmlData = new String(data);
            }
        } catch (IOException e) {
            logger.error(ltag, "Failed to read config file: " + e.getMessage());
            return false;
        }

        return parseConfigData(xmlData);
    }

    private boolean parseConfigData(String xmlData) {
        String tagName = this.name.toUpperCase();
        String regex = ".*?<" + tagName + ">(.*)</" + tagName + ">.*";
       //String regex = "<" + tagName + ">(.*)</" + tagName + ">";



        logger.info(ltag, "REGEX="+regex);
        xmlData = xmlData.replaceAll("\\n", "");
        xmlData = xmlData.replaceAll("\\r", "");
        xmlData = xmlData.replaceAll("\\t", "");
        xmlData = xmlData.replaceAll(" ", "");
        logger.info(ltag,"config="+xmlData);
        xmlData = xmlData.replaceAll(regex, "$1");
        //xmlData = Pattern.compile(regex, Pattern.DOTALL).matcher(xmlData).replaceAll("$1");

        logger.info(ltag,"config="+xmlData);

        String[] parts = xmlData.split("\n");
        for (String item : parts) {
            logger.info(ltag,"s="+item);

            String[] subParts = item.split(":");
            if (subParts.length < 2) {
                logger.error(ltag, "Failed to parse config");
                return false;
            }

            String k, rest = "";

            k = subParts[0].trim();
            rest = subParts[1].trim();

            for (int i = 2; i < subParts.length; i++)
                rest += ":" + subParts[i].trim();

            configHT.put(k, rest);
        }




        return true;
    }

    private String getDefaultConfig() {
        String config = "    <AUTHENTICATOR>\n" +
                "    \tmax-coins-to-multi-detect:400\n" +
                "     </AUTHENTICATOR>\n" +
                "\n" +
                "    <BACKUPER>\n" +
                "    </BACKUPER>\n" +
                "    \n" +
                "    <DEPOSITOR>\n" +
                "        combroker-url:https://bank.cloudcoin.global\n" +
                "    </DEPOSITOR>\n" +
                "    \n" +
                "    <DIRECTORYMONITOR>\n" +
                "    </DIRECTORYMONITOR>\n" +
                "    \n" +
                "    <ECHOER>\n" +
                "    \tstart:https://raida1.CloudCoin.global\n" +
                "    </ECHOER>\n" +
                "    \n" +
                "    <EMAILER>\n" +
                "    \tsmtp-server:mail.cloudcoin.global\n" +
                "    \tusername:testmail1\n" +
                "    \tpassword:ijjlaijijoijeijijijijdf\n" +
                "    </EMAILER>\n" +
                "    \n" +
                "    <EXPORTER>\n" +
                "    </EXPORTER>\n" +
                "    \n" +
                "    <FRACKFIXER>\n" +
                "    </FACKFIXER>\n" +
                "\n" +
                "    <GALLERIST>\n" +
                "    </GALLERIST>\n" +
                "    \n" +
                "    <GRADER>\n" +
                "    </GRADER>\n" +
                "    \n" +
                "    <LOSSFIXER>\n" +
                "    </LOSSFIXER>\n" +
                "    \n" +
                "    <MINDER>\n" +
                "    </MINDER>\n" +
                "    \n" +
                "    <PAYFORWARD>\n" +
                "    </PAYFORWARD>\n" +
                "    \n" +
                "    <SHOWCOINS>\n" +
                "    </SHOWCOINS>\n" +
                "    \n" +
                "    <TRANSFERER>\n" +
                "    </TRANSFERER>\n" +
                "    \n" +
                "    <TRANSLATOR>\n" +
                "    \tlanguage:english\n" +
                "    </TRANSLATOR>\n" +
                "    \n" +
                "    <UNPACKER>\n" +
                "    </UNPACKER>\n" +
                "    \n" +
                "    <VAULTER>\n" +
                "    </VAULTER>\n" +
                "\n" +
                "    <FOLDERS>\n" +
                "        home:\n" +
                "        bank:default\n" +
                "        import:default\n" +
                "        imported:default\n" +
                "        export:default\n" +
                "        trash:default\n" +
                "        suspect:default\n" +
                "    </FOLDERS>";

        return config;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setLogger(GLogger logger) {
        this.logger = logger;
    }


    public Object parseResponse(String string, Class c) {
        JSONObject o;

        Object newObject;

        try {
            newObject = c.newInstance();
            o = new JSONObject(string);
            for (Field f : c.getDeclaredFields()) {
                String name = f.getName();
                String value = o.optString(name);

                logger.info(ltag, "got value " + value);


                f.setAccessible(true);
                f.set(newObject, value);

            }
        } catch (IllegalAccessException e) {
            logger.error(ltag, "Illegal access ex");
            return null;
        } catch (InstantiationException e) {
            logger.error(ltag, "Illegal instantiation");
            return null;
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse json: " + e.getMessage());
            return null;
        }

        return newObject;
    }

    protected void cleanDir(String dir) {
        File dirObj = new File(dir);
        if (dirObj == null) {
            logger.error(ltag, "No dir found: " + dir);
            return;
        }

        for (File file: dirObj.listFiles()) {
            if (!file.isDirectory()) {
                logger.debug(ltag, "Deleting " + file);
                file.delete();
            }
        }
    }

    protected void cleanLogDir() {
        cleanDir(logDir);
    }

    protected void cleanPrivateLogDir() {
        logger.info(ltag, "p="+privateLogDir);
        cleanDir(privateLogDir);
    }

}
