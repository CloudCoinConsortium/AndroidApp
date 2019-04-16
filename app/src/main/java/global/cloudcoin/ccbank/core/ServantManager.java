/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.ServantManager;

import global.cloudcoin.ccbank.Echoer.Echoer;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.ServantRegistry;
import java.io.File;
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
    
    public ServantManager(GLogger logger, String home) {
        this.logger = logger;
        this.home = home;
        this.sr = new ServantRegistry();
        this.user = Config.DIR_DEFAULT_USER;
    }
    
    public String getActiveWallet() {
        return this.user;
    }
    
    public void setActiveWallet(String user) {
        this.user = user;
        sr.changeUser(user);
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
    
    public boolean initUser(String user, String email, String password) {
        logger.debug(ltag, "Init user " + user);
        
        try {
            AppCore.initUserFolders(user);
        } catch (Exception e) {
            logger.error(ltag, "Error: " + e.getMessage());
            return false;
        }
        
        setActiveWallet(user);
        
        if (!email.equals(""))
            sr.getServant("Authenticator").putConfigValue("email", email);
        
        if (!password.equals(""))
            sr.getServant("Vaulter").putConfigValue("status", "on");
        
        if (!writeConfig(user))
            return false;
        
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
    
    
}
