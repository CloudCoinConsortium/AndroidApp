/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.core;

import global.cloudcoin.ccbank.FrackFixer.GetTicketResponse;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.json.JSONException;

/**
 *
 * @author 
 */
public class DNSSn {
    GLogger logger;
    String name;
    String ltag = "DNSSn";
    String path;
    
    public DNSSn(String name, GLogger logger) {
        this.logger = logger;
        this.name = name;
    }
    
        
    
    public boolean recordExists() {
        String domain = name + "." + Config.DDNS_DOMAIN;
        
        logger.debug(ltag, "Query " + domain);
        InetAddress address;
        try {
            address = InetAddress.getByName(domain);
        } catch (UnknownHostException e) {
            logger.debug(ltag, "Host not found");
            return false;
        }
        
        return true;
    }
    
    public boolean setRecord(String path, ServantRegistry sr) {
        CloudCoin cc;

        logger.debug(ltag, "Setting record " + path);
        
        File f = new File(path);
        if (!f.exists()) {
            logger.error(ltag, "File " + path + " does not exist");
            return false;
        }    
        
        try {
            cc = new CloudCoin(f.toString());
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse coin: " + f.toString() +
                " error: " + e.getMessage());
            return false;
        }

        int raidaNum = Config.RAIDANUM_TO_QUERY_BY_DEFAULT;
        
        StringBuilder sb = new StringBuilder();
        sb.append("nns[]=");
        sb.append(cc.nn);
        sb.append("&sns[]=");
        sb.append(cc.sn);
        sb.append("&ans[]=");
        sb.append(cc.ans[raidaNum]);
        sb.append("&pans[]=");
        sb.append(cc.ans[raidaNum]);
        sb.append("&denomination[]=");
        sb.append(cc.getDenomination());


        DetectionAgent da = new DetectionAgent(raidaNum, Config.CONNECTION_TIMEOUT, logger);        
        String result = da.doRequest("/service/multi_get_ticket", sb.toString());
        if (result == null) {
            logger.error(ltag, "Failed to get tickets. Setting triad to failed");
            return false;
        }
        
        Object[] o = sr.getServant("FrackFixer").parseArrayResponse(result, GetTicketResponse.class);
        if (o == null) {
            logger.error(ltag, "Failed to parse result " + result);
            return false;
        }
        
        if (o.length != 1) {
            logger.error(ltag, "Failed to parse result (length is wrong) " + result);
            return false;
        }
        
        GetTicketResponse g = (GetTicketResponse) o[0];
        String message = g.message;
        
        logger.debug(ltag, " message " + g.message);
        if (!g.status.equals("ticket")) {
            logger.error(ltag, "Failed to get ticket for coin id " + cc.sn);
            return false;
        }
        
        if (message == null || message.length() != 44) {
            logger.error(ltag, "Invalid ticket from RAIDA");
            return false;
        }
        
        String rq = "/ddns.php?sn=" + cc.sn + "&username=" + name + "&ticket=" + message + "&raidanumber=" + raidaNum;

        DetectionAgent daFake = new DetectionAgent(RAIDA.TOTAL_RAIDA_CNT * 10000, Config.CONNECTION_TIMEOUT, logger);
        daFake.setExactFullUrl(Config.DDNSSN_SERVER);
        result = daFake.doRequest(rq, null);
        if (result == null) {
            logger.error(ltag, "Failed to receive response from DDNSSN Server");
            return false;
        }
        
        CommonResponse cr = (CommonResponse) sr.getServant("FrackFixer").parseResponse(result, CommonResponse.class);
        if (!cr.status.equals("success")) {
            logger.error(ltag, "Invalid response from DDNSSN Server");
            return false;
        }
        
        return true;
    }
    
    
}
