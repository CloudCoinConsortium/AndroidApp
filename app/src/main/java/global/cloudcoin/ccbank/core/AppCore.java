package global.cloudcoin.ccbank.core;

import java.io.File;

import global.cloudcoin.ccbank.core.Config;

public class AppCore {

    static private String ltag = "AppCore";

    static private File rootPath;
    static private GLogger logger;



   static private void createDirectory(String dirName) throws Exception {
        String idPath;

        idPath = rootPath + File.separator + dirName;
        try {
            File idPathFile = new File(idPath);
            idPathFile.mkdirs();
        } catch (Exception e) {
            logger.error(ltag, "Can not create directory " + dirName);
            throw new Exception();
        }

        logger.info(ltag, "CREATED " + idPath);
    }

    static public void initFolders(File path, GLogger logger) throws Exception {
        rootPath = path;
        AppCore.logger = logger;

        createDirectory("Accounts");
        createDirectory("Logs");
        createDirectory("Receipts");
        createDirectory("Commands");

        String[] folders = new String[]{
            Config.DIR_BANK,
            Config.DIR_COUNTERFEIT,
            Config.DIR_CONFIG,
            Config.DIR_DEPOSIT,
            Config.DIR_DETECTED,
            Config.DIR_EXPORT,
            Config.DIR_EMAILOUT,
            Config.DIR_FRACKED,
            Config.DIR_GALLERY,
            Config.DIR_ID,
            Config.DIR_IMPORT,
            Config.DIR_IMPORTED,
            Config.DIR_LOGS,
            Config.DIR_LOST,
            Config.DIR_MIND,
            Config.DIR_PARTIAL,
            Config.DIR_PAYFORWARD,
            Config.DIR_PREDETECT,
            Config.DIR_RECEIPTS,
            Config.DIR_REQUESTS,
            Config.DIR_REQUESTRESPONSE,
            Config.DIR_SUSPECT,
            Config.DIR_TEMPLATES,
            Config.DIR_TRASH,
            Config.DIR_TRUSTEDTRANSFER
        };

        createDirectory("Accounts" + File.separator + "DefaultUser");

        for (String dir : folders) {
            createDirectory("Accounts" + File.separator + "DefaultUser" + File.separator + dir);
        }




    }

    static public String getRootPath() {
       return rootPath.toString();
    }



}
