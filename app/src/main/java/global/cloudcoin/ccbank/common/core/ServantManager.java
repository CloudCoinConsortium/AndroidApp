/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.ServantManager;

import global.cloudcoin.ccbank.Authenticator.Authenticator;
import global.cloudcoin.ccbank.Backupper.Backupper;
import global.cloudcoin.ccbank.Echoer.Echoer;
import global.cloudcoin.ccbank.Eraser.Eraser;
import global.cloudcoin.ccbank.Exporter.Exporter;
import global.cloudcoin.ccbank.FrackFixer.FrackFixer;
import global.cloudcoin.ccbank.Grader.Grader;
import global.cloudcoin.ccbank.LossFixer.LossFixer;
import global.cloudcoin.ccbank.Receiver.Receiver;
import global.cloudcoin.ccbank.Sender.Sender;
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
import java.util.Collections;
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
    
    public ServantRegistry getSR() {
        return this.sr;
    }
    
    public Wallet getWallet(String wallet) {
        return wallets.get(wallet);
    }
    
    public void setActiveWallet(String wallet) {        
        this.user = wallet;
        sr.changeUser(wallet);   
    }
    
    public void changeServantUser(String servant, String wallet) {
        sr.changeServantUser(servant, wallet);
    }
    
    public boolean init() {
        AppCore.initPool();
        
        try {
            AppCore.initFolders(new File(home), logger);
        } catch (Exception e) {
            logger.error(ltag, "Failed to init root dir " + home);
            return false;
        }   
        
        initServants();
        
        return true;
    }
    
    public boolean initServants() {
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
                "ShowEnvelopeCoins",
                "Eraser",
                "Backupper"
        }, AppCore.getRootPath() + File.separator + user, logger);
   

        String[] wallets = AppCore.getDirs();
        for (int i = 0; i < wallets.length; i++) {
            setActiveWallet(wallets[i]);
            initWallet(wallets[i], "");
            
            checkIDCoins(wallets[i]);
        }
        
        //setActiveWallet(user);  
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
            logger.error(ltag, "Failed to write conifg");
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
    
    public void startLossFixerService(CallbackInterface cb) {
        LossFixer l = (LossFixer) sr.getServant("LossFixer");
	l.launch(cb);
    }
    
    public void startEraserService(CallbackInterface cb) {
	Eraser e = (Eraser) sr.getServant("Eraser");
	e.launch(cb);
    }
    
    public void startBackupperService(String dstDir, CallbackInterface cb) {
	Backupper b = (Backupper) sr.getServant("Backupper");
	b.launch(dstDir, cb);
    }
    
    public void startSenderService(int sn, String dstFolder, int amount, String memo, CallbackInterface cb) {
	Sender s = (Sender) sr.getServant("Sender");
	s.launch(sn, dstFolder, null, amount, memo, cb);
    }
     
    /*
    public void startReceiverService(int sn, String dstFolder, int amount, String memo, CallbackInterface cb) {
	Receiver r = (Receiver) sr.getServant("Receiver");
	r.launch(, new int[]{1,1}, new int[] {7050330, 7050331}, memo, cb);
        r.launch(sn, dstFolder, amount, memo, cb);
    }
    */
    
    public int getRemoteSn(String dstWallet) {
        int sn;
        
        try {
            sn = Integer.parseInt(dstWallet);
        } catch (NumberFormatException e) {
            return 0;
        }
        
        if (sn <= 0)
            return 0;
        
        return sn;
    }
    
    public boolean transferCoins(String srcWallet, String dstWallet, CloudCoin cc, int amount, 
            String memo, CallbackInterface scb, CallbackInterface rcb) {
        
        logger.debug(ltag, "Transferring " + amount + " from " + srcWallet + " to " + dstWallet);
        int sn = 0;
        
        Wallet srcWalletObj, dstWalletObj; 
        
        srcWalletObj = wallets.get(srcWallet);
        dstWalletObj = wallets.get(dstWallet);
        if (srcWalletObj == null) {
            logger.error(ltag, "Wallet not found");
            return false;
        }
        
        if (dstWalletObj == null) {
            sn = getRemoteSn(dstWallet);
            logger.debug(ltag, "Remote SkyWallet got SN " + sn);
            if (sn == 0) {
                logger.error(ltag, "Invalid dst wallet");
                return false;
            }
            
            if (srcWalletObj.isSkyWallet()) {
                logger.error(ltag, "We can't transfer from SKY to SKY");
                return false;
            }
            
            dstWallet = null;
        } else {
            if (srcWalletObj.isSkyWallet()) {
                dstWallet = null;
                sn = cc.sn;
            }
        }
                
        if (srcWalletObj.isSkyWallet()) {
            logger.debug(ltag, "Receiving from SkyWallet");
            setActiveWallet(dstWallet);
            //startReceiverService(cc.sn, dstWalletObj.getName(), amount, memo, rcb);
        } else {
            if (srcWalletObj.isEncrypted()) {
                logger.debug(ltag, "Src wallet is encrypted");
                Vaulter v = (Vaulter) sr.getServant("Vaulter");
                v.unvault(srcWalletObj.getPassword(), amount, null, 
                        new rVaulterCb(sn, dstWallet, amount, memo, scb));
                
            }
            
            logger.debug(ltag, "send to sn " + sn + " dstWallet " + dstWallet);
            startSenderService(sn, dstWallet, amount, memo, scb);
        }
        
        return true;
        
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
    
    class rVaulterCb implements CallbackInterface {
        CallbackInterface cb;
        int amount;
        String memo;
        String dstFolder;
        int sn;
    
        public rVaulterCb(int sn, String dstFolder, int amount, 
                String memo, CallbackInterface cb) {
            this.cb = cb;
            this.amount = amount;
            this.memo = memo;
            this.sn = sn;
            this.dstFolder = dstFolder;
        }
        
	public void callback(final Object result) {
            final Object fresult = result;
            VaulterResult vresult = (VaulterResult) fresult;
            
            if (vresult.status == VaulterResult.STATUS_ERROR) {
                logger.error(ltag, "Error on Vaulter");
                return;
            }
            
            logger.debug(ltag, "send sn " + sn + " dstWallet " + dstFolder);
            startSenderService(sn, dstFolder, amount, memo, cb);
	}
    }
    
    public Wallet[] getWallets() {
        int size = wallets.size();
        Collection c = wallets.values();
        Wallet[] ws = new Wallet[size];
        
        int i = 0;
        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            Wallet tw = (Wallet) itr.next();
            if (tw.getName().equals(Config.DIR_DEFAULT_USER)) {
                ws[i++] = tw;
                break;
            }
        }
        
        itr = c.iterator();
        while (itr.hasNext()) {
            Wallet tw = (Wallet) itr.next();
            if (tw.getName().equals(Config.DIR_DEFAULT_USER))
                continue;
            
            ws[i++] = tw;
        }
        
        
        return ws;
    }
}
