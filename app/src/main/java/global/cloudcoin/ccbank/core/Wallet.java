/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.core;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Alexander
 */
public class Wallet {
    String ltag = "Wallet";
    GLogger logger;
    String name;
    String lsep;
    String password;
    boolean isEncrypted;
    String email;
    
    public Wallet(String name, String email, boolean isEncrypted, String password, GLogger logger) {
        this.name = name;
        this.email = email;
        this.isEncrypted = isEncrypted;
        this.password = password;
        this.ltag += name;
        this.logger = logger;
        
        logger.debug(ltag, "wallet " + name + " e=" + email + " is="+isEncrypted+ " p="+password);
        lsep = System.getProperty("line.separator");
    }
    
    public boolean isEncrypted() {
        return this.isEncrypted;
    }
    
    public String getPassword() {
        return this.password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String[][] getTransactions() {
        
        
        String fileName = AppCore.getUserDir(Config.TRANSACTION_FILENAME, name);
        String data = AppCore.loadFile(fileName);
        if (data == null)
            return null;
        
        String[] parts = data.split("\\r?\\n");
        String[][] rv = new String[parts.length][];
        
        for (int i = 0; i < parts.length; i++) {
            rv[i] = parts[i].split(",");
            if (rv[i].length != 5) {
                logger.error(ltag, "Transaction parse error: " + parts[i]);
                return null;
            }
            
            rv[i][3] = rv[i][3].replace("-", "");
        }
        
        return rv;
   
       
       
    }
    
    public void appendTransaction(String memo, int amount) {
        
        
        String fileName = AppCore.getUserDir(Config.TRANSACTION_FILENAME, name);
        
        String date = AppCore.getCurrentDate(); 
        String rMemo = memo.replaceAll("\r\n", " ").replaceAll("\n", " ").replaceAll(",", " ");
        //String sAmount = Integer.toString(amount);
        
        int rest = 0;
        String[][] tr = getTransactions();
        if (tr != null) {
        
            String[] last = tr[tr.length - 1];
            String rRest = last[last.length - 1];
            
            rest = Integer.parseInt(rRest);
            if (rest <= 0)
                rest = 0;
            
            System.out.println("rest="+rest);
        }
        
        
        
        
        
        
        String result = rMemo + "," + date + ",";
        if (amount > 0) {
            result += amount + ",,";
        } else {
            result += "," + amount + ",";
        }
        
        rest += amount;
        
        result += rest + lsep;
        
        logger.debug(ltag, "Saving " + result);
        AppCore.saveFileAppend(fileName, result, true);
        
        System.out.println("r=" + result);
        
        
    }
    
    
}
