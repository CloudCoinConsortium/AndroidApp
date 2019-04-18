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
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.ServantRegistry;
import global.cloudcoin.ccbank.core.Wallet;
import java.io.File;
import java.util.Hashtable;
import pbank.Pbank;


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
        
        initWallet(wallet, "");     
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
   
        return true;
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
            
        System.out.println("wwwwall=" + wallet + " em="+email + " st="+ encStatus.equals("on")+ " p="+password);
        
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
            ct = sr.getServant(name).getConfigText();
            
            config += ct;
            System.out.println("ct="+ct);
        }

        String configFilename = AppCore.getUserConfigDir(user) + File.separator + "config.txt";
        
        if (!AppCore.saveFile(configFilename, config)) {
            logger.error(ltag, "Failed to save config");
            return false;
        }
        
        return true;
    }
    
    public void startEchoService(CallbackInterface cb) {
	Echoer e = (Echoer) sr.getServant("Echoer");
	e.launch(cb);
    }
    
    public boolean isEchoerFinished() {
        return sr.isRunning("Echoer");
    }
    
    public void startFrackFixerService(CallbackInterface cb) {
        if (sr.isRunning("FrackFixer"))
            return;
        
        FrackFixer ff = (FrackFixer) sr.getServant("FrackFixer");
	ff.launch(cb);
    }
    
    public void startUnpackerService(CallbackInterface cb) {
	Unpacker up = (Unpacker) sr.getServant("Unpacker");
	up.launch(cb);
    }
     
    public void startAuthenticatorService(CallbackInterface cb) {
	Authenticator at = (Authenticator) sr.getServant("Authenticator");
	at.launch(cb);
    }
    
    public void startGraderService(CallbackInterface cb) {
	Grader gd = (Grader) sr.getServant("Grader");
	gd.launch(cb);
    }
    
    public void startShowCoinsService(CallbackInterface cb) {
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
        Exporter ex = (Exporter) sr.getServant("Exporter");
	ex.launch(exportType, amount, tag, cb);
    }
    
}
