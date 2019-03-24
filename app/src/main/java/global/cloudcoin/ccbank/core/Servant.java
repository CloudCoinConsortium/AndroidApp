package global.cloudcoin.ccbank.core;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Set;


public class Servant {

    private String ltag = "Servant";

    private String rootDir;
    private String name;

    protected RAIDA raida;

    protected GLogger logger;

    protected Config config;

    private Hashtable<String, String> configHT;

    protected String logDir, privateLogDir;

    protected CallbackInterface cb;

    protected Thread thread;

    protected boolean cancelRequest;

    public Servant(String name, String rootDir, GLogger logger) {
        this.name = name;
        this.rootDir = rootDir;
        this.logger = logger;
        this.config = null;
        this.cancelRequest = false;

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

    public void cancel() {
        this.cancelRequest = true;
    }

    public void resume() {
        this.cancelRequest = false;
    }

    public boolean isCancelled() {
        return this.cancelRequest;
    }

    public void launch() {
        launch(null);
    }

    public void launch(CallbackInterface cb) {}

    public void launchDetachedThread(Runnable runnable) {
        thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
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

        File logDirObj = new File(echoerLogDIr);
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

        xmlData = xmlData.replaceAll("\\n", "");
        xmlData = xmlData.replaceAll("\\r", "");
        xmlData = xmlData.replaceAll("\\t", "");
        xmlData = xmlData.replaceAll(" ", "");
        xmlData = xmlData.replaceAll(regex, "$1");

        String[] parts = xmlData.split("\n");
        for (String item : parts) {
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

    public String getConfigValue(String key) {
        return configHT.get(key);
    }

    public int getIntConfigValue(String key) {
        String val = getConfigValue(key);
        if (val == null)
            return -1;

        int value;
        try {
            value = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return -1;
        }

        return value;
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
                "    \tusername:dummy\n" +
                "    \tpassword:dummy\n" +
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

    public void setLogger(GLogger logger) {
        this.logger = logger;
    }


    private boolean doSetField(Field f, JSONObject o, Object targetObject) throws IllegalAccessException, JSONException {
        String name = f.getName();
        if (f.getType() == int.class) {
            int value = o.optInt(name);
            f.setAccessible(true);
            f.set(targetObject, value);
        } else if (f.getType() == String.class) {
            String value = o.optString(name);
            f.setAccessible(true);
            f.set(targetObject, value);
        } else if (f.getType() == int[].class) {
            int length;
            JSONArray a = o.optJSONArray(name);
            f.setAccessible(true);

            if (a != null)
                length = a.length();
            else
                length = 0;

            int[] ia = new int[length];
            for (int i = 0; i < length; i++) {
                ia[i] = a.getInt(i);
            }

            f.set(targetObject, ia);
        } else {
            logger.error(ltag, "Invalid type: " + f.getType());
            return false;
        }

        return true;
    }

    private Object setFields(Class c, JSONObject o) {
        Object targetObject;

        try {
            targetObject = c.newInstance();
            for (Field f : c.getDeclaredFields()) {
                if (!doSetField(f, o, targetObject))
                    return null;
            }

            if (c.getSuperclass() != null && c.getSuperclass() == CommonResponse.class) {
                for (Field f : c.getSuperclass().getDeclaredFields()) {
                    if (!doSetField(f, o, targetObject))
                        return null;
                }
            }
        } catch (IllegalAccessException e) {
            logger.error(ltag, "Illegal access exception");
            return null;
        } catch (InstantiationException e) {
            logger.error(ltag, "Illegal instantiation");
            return null;
        } catch (IllegalArgumentException e) {
            logger.error(ltag, "Illegal argument: " + e.getMessage());
            return null;
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse JSON: " + e.getMessage());
            return null;
        }

        return targetObject;
    }

    public Object[] parseArrayResponse(String string, Class c) {
        Object[] newObjectArray;
        JSONArray a;
        JSONObject o;

        if (string == null)
            return null;

        try {
            a = new JSONArray(string.trim());
            newObjectArray = new Object[a.length()];
            for (int i = 0; i < a.length(); i++) {
                o = a.getJSONObject(i);
                newObjectArray[i] = setFields(c, o);
                if (newObjectArray[i] == null)
                    return null;
            }
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse json: " + e.getMessage());
            return null;
        }

        return newObjectArray;
    }

    public Object parseResponse(String string, Class c) {
        JSONObject o;
        Object newObject;

        if (string == null)
            return null;

        try {
            o = new JSONObject(string.trim());
            newObject = setFields(c, o);
            if (newObject == null)
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

    protected CloudCoin getIDcc(String user, int sn) {
        String fullDirIDPath = AppCore.getUserDir(Config.DIR_ID, user);
        CloudCoin cc = null;

        File dirObj = new File(fullDirIDPath);
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            logger.debug(ltag, "Parsing " + file);

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse JSON: " + e.getMessage());
                continue;
            }

            logger.info(ltag, "Found SN: " + cc.sn);
            if (cc.sn != sn)
                continue;

            break;
        }

        return cc;
    }

    protected void setSenderRAIDA() {
        String[] urls = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            urls[i] = "https://s" + i + "." + Config.SENDER_DOMAIN;
        }

        raida.setExactUrls(urls);
    }

}
