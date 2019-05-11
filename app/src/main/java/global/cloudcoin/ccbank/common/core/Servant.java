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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;


public class Servant {

    private String ltag = "Servant";

    public  String user;
    
    private String name;

    protected RAIDA raida;

    protected GLogger logger;

    protected Config config;

    private Hashtable<String, String> configHT;

    protected String logDir, privateLogDir;

    protected CallbackInterface cb;

    protected Thread thread;

    protected boolean cancelRequest;

    protected ArrayList<CloudCoin> coinsPicked;

    protected int[] valuesPicked;

    public Servant(String name, String rootDir, GLogger logger) {
        this.name = name;
        this.logger = logger;
        this.config = null;
        this.cancelRequest = false;
      
        configHT = new Hashtable<String, String>();

        this.raida = new RAIDA(logger);

        File f = new File(rootDir);
        changeUser(f.getName());
        
        AppCore.createDirectory(Config.DIR_MAIN_LOGS + File.separator + name);

                
        this.logDir = AppCore.getLogDir() + File.separator + name;
        
        logger.info(ltag, "Instantiated servant " + name + " for " + this.user); 
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

    public void changeUser(String user) {
        logger.debug(ltag, "Servant " + name + " changing user to " + user);
        
        this.user = user;
        this.privateLogDir = AppCore.getPrivateLogDir(this.user) + File.separator + name;
        
        AppCore.createDirectoryPath(this.privateLogDir);
        
        configHT = new Hashtable<String, String>();
        
        readConfig();
        setConfig();
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

    public void putConfigValue(String key, String value) {
        configHT.put(key, value);
    }
    
    
    
    public String getConfigText() {
        String data = "<" + name.toUpperCase() + ">";
        
        Set<String> keys = configHT.keySet();
        for (String v : keys) {
            data += v + ":" + configHT.get(v) + "\r\n";
        }
        
        data += "</" +name.toUpperCase() + ">";
        
        return data;
    }
    
    private boolean readConfig() {
        String configFilename = AppCore.getUserConfigDir(user) + File.separator + "config.txt";
        byte[] data;
        String xmlData;

        logger.debug(ltag, "reading " + configFilename);
        File file = new File(configFilename);
        try {
            if (!file.exists()) {
                logger.error(ltag, "No config found for user " + user);
                return false;
            } 
            
            data = Files.readAllBytes(Paths.get(configFilename));
            xmlData = new String(data);
        } catch (IOException e) {
            logger.error(ltag, "Failed to read config file: " + e.getMessage());
            return false;
        }

        return parseConfigData(xmlData);
    }

    private boolean parseConfigData(String xmlData) {
        String tagName = this.name.toUpperCase();
        if (xmlData.indexOf("<" + tagName + ">") == -1)
            return true;
        
        String regex = ".*?<" + tagName + ">(.*)</" + tagName + ">.*";

        xmlData = xmlData.replaceAll("\\n", "***");
        xmlData = xmlData.replaceAll("\\r", "");
        xmlData = xmlData.replaceAll("\\t", "");
        xmlData = xmlData.replaceAll(" ", "");
        
        xmlData = xmlData.replaceAll(regex, "$1");
        
        String[] parts = xmlData.split("\\*\\*\\*");   
        for (String item : parts) {
            if (item.equals(""))
                continue;

            String[] subParts = item.split(":");
            if (subParts.length < 2) {
                logger.error(ltag, "Failed to parse config value " + item);
                continue;
            }

            String k, rest = "";

            k = subParts[0].trim();
            rest = subParts[1].trim();

            for (int i = 2; i < subParts.length; i++)
                rest += ":" + subParts[i].trim();

            logger.debug(ltag, "serv " + name + "-> " + k + ":" + rest);
            configHT.put(k, rest);
        }

        return true;
    }
    
    public void setConfig() {
        logger.debug(ltag, "Set parent config");
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

    public void setLogger(GLogger logger) {
        this.logger = logger;
    }


    private boolean doSetField(Field f, JSONObject o, Object targetObject) throws IllegalAccessException, JSONException {
        String name = f.getName();

        f.setAccessible(true);
        if (f.getType() == int.class) {
            int value = o.optInt(name);
            f.set(targetObject, value);
        } else if (f.getType() == String.class) {
            String value = o.optString(name);
            f.set(targetObject, value);
        } else if (f.getType() == int[].class) {
            int length;
            JSONArray a = o.optJSONArray(name);

            if (a != null)
                length = a.length();
            else
                length = 0;

            int[] ia = new int[length];
            for (int i = 0; i < length; i++) {
                ia[i] = a.getInt(i);
            }

            f.set(targetObject, ia);
        } else if (f.getType() == String[].class) {
            int length;
            JSONArray a = o.optJSONArray(name);

            if (a != null)
                length = a.length();
            else
                length = 0;

            String[] ia = new String[length];
            for (int i = 0; i < length; i++) {
                ia[i] = a.getString(i);
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
        if (1==1)
            return;
        
        String[] urls = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            //urls[i] = "https://s" + i + "." + Config.SENDER_DOMAIN;
            urls[i] = "https://raida" + i + ".cloudcoin.global";
        }

        raida.setExactUrls(urls);
    }


    protected boolean collectedEnough(int[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != valuesPicked[i]) {
                return false;
            }
        }

        return true;
    }

    protected void pickCoin(int idx, int[] values, CloudCoin cc) {
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

    public int[] countCoins(String dir) {
        int[] totals = new int[6];
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
            if (denomination == 1)
                totals[Config.IDX_1]++;
            else if (denomination == 5)
                totals[Config.IDX_5]++;
            else if (denomination == 25)
                totals[Config.IDX_25]++;
            else if (denomination == 100)
                totals[Config.IDX_100]++;
            else if (denomination == 250)
                totals[Config.IDX_250]++;
            else
                continue;

            totals[Config.IDX_TOTAL] += denomination;
        }

        return totals;
    }


    public boolean pickCoinsAmountInDir(String dir, int amount) {
        int[] totals;
        int denomination;
        CloudCoin cc;

        totals = countCoins(dir);
        if (amount > totals[Config.IDX_TOTAL]) {
            logger.error(ltag, "Not enough coins");
            return false;
        }

        if (amount < 0)
            return false;

        int exp_1 = 0;
        int exp_5 = 0;
        int exp_25 = 0;
        int exp_100 = 0;
        int exp_250 = 0;

        for (int i = 0; i < totals.length; i++)
            logger.debug(ltag, "v=" + totals[i]);


        if (amount >= 250 && totals[Config.IDX_250] > 0) {
            exp_250 = ((amount / 250) < (totals[Config.IDX_250])) ? (amount / 250) : (totals[Config.IDX_250]);
            amount -= (exp_250 * 250);
        }

        if (amount >= 100 && totals[Config.IDX_100] > 0) {
            exp_100 = ((amount / 100) < (totals[Config.IDX_100])) ? (amount / 100) : (totals[Config.IDX_100]);
            amount -= (exp_100 * 100);
        }

        if (amount >= 25 && totals[Config.IDX_25] > 0) {
            exp_25 = ((amount / 25) < (totals[Config.IDX_25])) ? (amount / 25) : (totals[Config.IDX_25]);
            amount -= (exp_25 * 25);
        }

        if (amount >= 5 && totals[Config.IDX_5] > 0) {
            logger.debug(ltag, "a=" +amount+ " tot="+totals[Config.IDX_5] + " s="+ (amount/5));
            exp_5 = ((amount / 5) < (totals[Config.IDX_5])) ? (amount / 5) : (totals[Config.IDX_5]);
            amount -= (exp_5 * 5);
        }

        if (amount >= 1 && totals[Config.IDX_1] > 0) {
            exp_1 = (amount < (totals[Config.IDX_1])) ? amount : (totals[Config.IDX_1]);
            amount -= (exp_1);
        }

        if (amount != 0) {
            logger.error(ltag, "Can't collect needed amount of coins. rest: " + amount);
            return false;
        }

        logger.debug(ltag, "Denom: " + exp_1 + "/" + exp_5 + "/" + exp_25 + "/" + exp_100 + "/" + exp_250);
        logger.debug(ltag, "Looking into dir: " + dir + " for " + amount + " coins: " + totals[5]);

        File dirObj = new File(dir);
        for (File file : dirObj.listFiles()) {
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
                if (exp_1-- > 0) {
                    logger.info(ltag, "Adding 1: " + cc.sn);
                    coinsPicked.add(cc);
                } else {
                    exp_1 = 0;
                }
            } else if (denomination == 5) {
                if (exp_5-- > 0) {
                    logger.info(ltag, "Adding 5: " + cc.sn);
                    coinsPicked.add(cc);
                } else {
                    exp_5 = 0;
                }
            } else if (denomination == 25) {
                if (exp_25-- > 0) {
                    logger.info(ltag, "Adding 25: " + cc.sn);
                    coinsPicked.add(cc);
                } else {
                    exp_25 = 0;
                }
            } else if (denomination == 100) {
                if (exp_100-- > 0) {
                    logger.info(ltag, "Adding 100: " + cc.sn);
                    coinsPicked.add(cc);
                } else {
                    exp_100 = 0;
                }
            } else if (denomination == 250) {
                if (exp_250-- > 0) {
                    logger.info(ltag, "Adding 250: " + cc.sn);
                    coinsPicked.add(cc);
                } else {
                    exp_250 = 0;
                }
            }
        }

        return true;
    }

    

}