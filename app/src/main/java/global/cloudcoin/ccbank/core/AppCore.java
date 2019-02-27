package global.cloudcoin.ccbank.core;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Executors;

import global.cloudcoin.ccbank.core.Config;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class AppCore {

    static private String ltag = "AppCore";

    static private File rootPath;
    static private GLogger logger;

    static private ExecutorService service;


   static public void createDirectory(String dirName) {
        String idPath;

        idPath = rootPath + File.separator + dirName;
        logger.info(ltag, "Creating " + idPath);
        File idPathFile = new File(idPath);
        if (!idPathFile.mkdirs()) {
            logger.error(ltag, "Can not create directory " + dirName);
            return;
        }

        logger.info(ltag, "CREATED " + idPath);
    }

    static public void initFolders(File path, GLogger logger) throws Exception {

        rootPath = path;
        AppCore.logger = logger;

        createDirectory(Config.DIR_ROOT);
        rootPath = new File(path, Config.DIR_ROOT);

        createDirectory(Config.DIR_ACCOUNTS);
        createDirectory(Config.DIR_MAIN_LOGS);
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

        createDirectory(Config.DIR_ACCOUNTS + File.separator + Config.DIR_DEFAULT_USER);

        for (String dir : folders) {
            createDirectory(Config.DIR_ACCOUNTS + File.separator + Config.DIR_DEFAULT_USER + File.separator + dir);
        }




    }

    static public String getRootPath() {
       return rootPath.toString();
    }

    static public String getConfigDir() {
       File f;

       f = new File(rootPath, "Accounts");
       f = new File(f, "DefaultUser");
       f = new File(f, Config.DIR_CONFIG);

       return f.toString();
   }

   static public void initPool() {
       service = Executors.newFixedThreadPool(Config.THREAD_POOL_SIZE);
   }

   static public void shutDownPool() {
       service.shutdown();
   }

   static public ExecutorService getServiceExecutor() {
       return service;
   }

   static public String getLogDir() {
       File f;

       f = new File(rootPath, Config.DIR_MAIN_LOGS);

       return f.toString();
   }

   static public String getPrivateLogDir() {
       File f;

       f = new File(rootPath, Config.DIR_ACCOUNTS);
       f = new File(f, Config.DIR_DEFAULT_USER);
       f = new File(f, Config.DIR_LOGS);

       return f.toString();
   }

   static public String getUserDir(String folder) {
       File f;

       f = new File(rootPath, Config.DIR_ACCOUNTS);
       f = new File(f, Config.DIR_DEFAULT_USER);
       f = new File(f, folder);

       return f.toString();
   }

    static public int getTotal(int[] counters) {
        return counters[Config.IDX_1] + counters[Config.IDX_5] * 5 +
                counters[Config.IDX_25] * 25 + counters[Config.IDX_100] * 100 +
                counters[Config.IDX_250] * 250;
    }

    static public int[] getDenominations() {
        int[] denominations = {1, 5, 25, 100, 250};

        return denominations;
    }

    static public void moveToFolder(String fileName, String folder) {
        logger.info(ltag, "Moving to " + folder + " -> " + fileName);

        try {
            File fsource = new File(fileName);
            String target = AppCore.getUserDir(folder) + File.separator +
                    System.currentTimeMillis() + "-" + fsource.getName();

            File ftarget = new File(target);
            if (!fsource.renameTo(ftarget))
                logger.error(ltag, "Failed to rename file " + fileName);
        } catch (Exception e) {
            logger.error(ltag, "Failed to move file to trash: " + e.getMessage());
        }
    }

    static public void moveToTrash(String fileName) {
        moveToFolder(fileName, Config.DIR_TRASH);
    }

    static public void moveToImported(String fileName) {
        moveToFolder(fileName, Config.DIR_IMPORTED);
    }


    static public String loadFile(String fileName) {
        String jsonData = "";
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                jsonData += line + "\n";
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException ex) {
                return null;
            }
        }

        return jsonData;
    }

    static public boolean saveFile(String path, String data) {
        File f = new File(path);
        if (f.exists()) {
            logger.error(ltag, "File " + path + " already exists");
            return false;
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));
            writer.write(data);
        } catch (IOException e){
            logger.error(ltag, "Failed to write file: " + e.getMessage());
            return false;
        } finally {
            try{
                if (writer != null)
                    writer.close();
            } catch (IOException e){
                logger.error(ltag, "Failed to close buffered writer");
                return false;
            }
        }

        return true;
    }

    static public int getFilesCount(String dir) {
       String path = getUserDir(dir);

       return new File(path).listFiles().length;
    }

}
