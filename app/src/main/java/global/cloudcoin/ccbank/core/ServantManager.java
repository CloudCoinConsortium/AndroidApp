/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.ServantManager;

import global.cloudcoin.ccbank.Authenticator.Authenticator;
import global.cloudcoin.ccbank.Echoer.Echoer;
import global.cloudcoin.ccbank.Exporter.Exporter;
import global.cloudcoin.ccbank.FrackFixer.FrackFixer;
import global.cloudcoin.ccbank.Grader.Grader;
import global.cloudcoin.ccbank.ShowCoins.ShowCoins;
import global.cloudcoin.ccbank.Unpacker.Unpacker;
import global.cloudcoin.ccbank.Vaulter.Vaulter;
import global.cloudcoin.ccbank.Vaulter.VaulterResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.ServantRegistry;
import global.cloudcoin.ccbank.core.Wallet;
import java.io.File;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import org.json.JSONException;

/**
 *
 * @author Alexander
 */
public class ServantManager {
    String ltag = "ServantManager";
    ServantRegistry sr;
    GLogger logger;
    String home;
    String user;
    private Hashtable<String, Wallet> wallets;
    
    public ServantManager(GLogger logger, String home) {
        this.logger = logger;
        this.home = home;
        this.sr = new ServantRegistry();
        this.user = Config.DIR_DEFAULT_USER;
        this.wallets = new Hashtable<String, Wallet>();
    }
    
    public Wallet getActiveWallet() {
        if (!wallets.containsKey(user)) 
            return null;
        
        return wallets.get(user);
    }
    
    public void setActiveWallet(String wallet) {        
        this.user = wallet;
        sr.changeUser(wallet);   
    }
    
    public boolean init() {
        AppCore.initPool();
        
        try {
            AppCore.initFolders(new File(home), logger);
        } catch (Exception e) {
            logger.error(ltag, "Failed to init root dir " + home);
            return false;
        }   
        
        sr.registerServants(new String[]{
                "Echoer",
                "Authenticator",
                "ShowCoins",
                "Unpacker",
                "Authenticator",
                "Grader",
                "FrackFixer",
                "Exporter",
                "Sender",
                "Receiver",
                "Backupper",
                "LossFixer",
                "ChangeMaker",
                "Vaulter",
                "ShowEnvelopeCoins"
        }, AppCore.getRootPath() + File.separator + user, logger);
   
        
        String[] wallets = AppCore.getDirs();
        for (int i = 0; i < wallets.length; i++) {
            setActiveWallet(wallets[i]);
            initWallet(wallets[i], "");
            
            System.out.println("Checking " + wallets[i]);
            checkIDCoins(wallets[i]);
            
        }
        
        setActiveWallet(user);
        
        return true;
    }
    
    public void checkIDCoins(String root) {
        String[] idCoins = AppCore.getFilesInDir(Config.DIR_ID, root);
        
        for (int i = 0; i < idCoins.length; i++) {
            CloudCoin cc;
            try {
                cc = new CloudCoin(AppCore.getUserDir(Config.DIR_ID, root) + File.separator + idCoins[i]);
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse ID coin: " + idCoins[i] + " error: " + e.getMessage());
                continue;
            }
            
            initCloudWallet(root, cc);
            System.out.println("x="+idCoins[i]+ " c="+cc.sn);
        }
        
    }
    
    public void initCloudWallet(String wallet, CloudCoin cc) {
        Wallet parent = wallets.get(wallet);
        
        String name = wallet + ":" + cc.sn;
        
        Wallet wobj = new Wallet(name, parent.getEmail(), parent.isEncrypted(), parent.getPassword(), logger);
        wobj.setIDCoin(cc);
        
        wallets.put(name, wobj);   
    }
    
    public void initWallet(String wallet, String password) {
        if (wallets.containsKey(wallet)) 
            return;
        
        logger.debug(ltag, "Initializing wallet " + wallet);
        Authenticator au = (Authenticator) sr.getServant("Authenticator");
        String email = au.getConfigValue("email");
        if (email == null)
            email = "";
            
        Vaulter v = (Vaulter) sr.getServant("Vaulter");
        String encStatus = v.getConfigValue("status");
        if (encStatus == null)
            encStatus = "off";
            
        System.out.println("wallet " + wallet + " em="+email + " st="+ encStatus.equals("on")+ " p="+password);
        
        Wallet wobj = new Wallet(wallet, email, encStatus.equals("on"), password, logger);
        wallets.put(wallet, wobj);    
        
    }
    
    public boolean initUser(String wallet, String email, String password) {
        logger.debug(ltag, "Init user " + wallet);
        
        try {
            AppCore.initUserFolders(wallet);
        } catch (Exception e) {
            logger.error(ltag, "Error: " + e.getMessage());
            return false;
        }
        
        this.user = wallet;
        sr.changeUser(wallet);
        
        if (!email.equals(""))
            sr.getServant("Authenticator").putConfigValue("email", email);
        
        if (!password.equals(""))
            sr.getServant("Vaulter").putConfigValue("status", "on");
        
        if (!writeConfig(wallet)) {
            System.exit(1);
            return false;
        }
              
        initWallet(wallet, password);
              
        return true;
    }
    
    public boolean writeConfig(String user) {
        String config = "", ct;
        
        for (String name : sr.getServantKeySet()) {
            System.out.println("na="+name);
            ct = sr.getServant(name).getConfigText();
            
            System.out.println("ct="+ct);
            config += ct;
        }

        String configFilename = AppCore.getUserConfigDir(user) + File.separator + "config.txt";
        
        if (!AppCore.saveFile(configFilename, config)) {
            logger.error(ltag, "Failed to save config");
            return false;
        }
        
        return true;
    }
    
    public void startEchoService(CallbackInterface cb) {
        if (sr.isRunning("Echoer"))
            return;
        
	Echoer e = (Echoer) sr.getServant("Echoer");
	e.launch(cb);
    }
    
    public boolean isEchoerFinished() {
        return !sr.isRunning("Echoer");
    }
    
    public void startFrackFixerService(CallbackInterface cb) {
        if (sr.isRunning("FrackFixer"))
            return;
        
        FrackFixer ff = (FrackFixer) sr.getServant("FrackFixer");
	ff.launch(cb);
    }
    
    public void startUnpackerService(CallbackInterface cb) {
        if (sr.isRunning("Unpacker"))
            return;
        
	Unpacker up = (Unpacker) sr.getServant("Unpacker");
	up.launch(cb);
    }
     
    public void startAuthenticatorService(CallbackInterface cb) {
        if (sr.isRunning("Authenticator"))
            return;
        
	Authenticator at = (Authenticator) sr.getServant("Authenticator");
	at.launch(cb);
    }
    
    public void startGraderService(CallbackInterface cb) {
        if (sr.isRunning("Grader"))
            return;
        
	Grader gd = (Grader) sr.getServant("Grader");
	gd.launch(cb);
    }
    
    public void startShowCoinsService(CallbackInterface cb) {
        if (sr.isRunning("ShowCoins"))
            return;
                
	ShowCoins sc = (ShowCoins) sr.getServant("ShowCoins");
	sc.launch(cb);
    }
    
    public void startVaulterService(CallbackInterface cb) {
        String password = getActiveWallet().getPassword();
        
        logger.debug(ltag, "Vaulter password " + password);
	Vaulter v = (Vaulter) sr.getServant("Vaulter");
	v.vault(password, 0, null, cb);
    }
    
    public void startExporterService(int exportType, int amount, String tag, CallbackInterface cb) {
        if (sr.isRunning("Exporter"))
            return;
                
        Exporter ex = (Exporter) sr.getServant("Exporter");
	ex.launch(exportType, amount, tag, cb);
    }
    
    public void startSecureExporterService(int exportType, int amount, String tag, CallbackInterface cb) {
        String password = getActiveWallet().getPassword();
        
        logger.debug(ltag, "Vaulter password " + password);
	Vaulter v = (Vaulter) sr.getServant("Vaulter");
	v.unvault(password, amount, null, new eVaulterCb(exportType, amount, tag, cb));
    }
    
    class eVaulterCb implements CallbackInterface {
        CallbackInterface cb;
        int exportType;
        int amount;
        String tag;
        
        public eVaulterCb(int exportType, int amount, String tag, CallbackInterface cb) {
            this.cb = cb;
            this.amount = amount;
            this.tag = tag;
            this.exportType = exportType;
        }
        
	public void callback(final Object result) {
            final Object fresult = result;
            VaulterResult vresult = (VaulterResult) fresult;

            Exporter ex = (Exporter) sr.getServant("Exporter");
            ex.launch(exportType, amount, tag, cb);
	}
    }
    
    public Wallet[] getWallets() {
        int size = wallets.size();
        Collection c = wallets.values();
        Wallet[] ws = new Wallet[size];
        
        int i = 0;
        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            ws[i++] = (Wallet) itr.next();
        }
        
        return ws;
    }
}
