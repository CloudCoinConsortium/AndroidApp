package global.cloudcoin.ccbank.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.json.JSONException;

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

    static public void createDirectoryPath(String path) {
        File idPathFile = new File(path);
        if (idPathFile.exists())
            return;
        
        if (!idPathFile.mkdirs()) {
            logger.error(ltag, "Can not create directory " + path);
            return;
        }
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
    }
   
    static public void initUserFolders(String user) throws Exception {
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
            Config.DIR_SENT,
            Config.DIR_SUSPECT,
            Config.DIR_TEMPLATES,
            Config.DIR_TRASH,
            Config.DIR_TRUSTEDTRANSFER,
            Config.DIR_VAULT
        };

        createDirectory(Config.DIR_ACCOUNTS + File.separator + user);

        for (String dir : folders) {
            createDirectory(Config.DIR_ACCOUNTS + File.separator + user + File.separator + dir);
        }
    }

    static public String getRootPath() {
       return rootPath.toString();
    }
  
    static public String getUserConfigDir(String user) {
       File f;

       f = new File(rootPath, "Accounts");
       f = new File(f, user);
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

   static public String getPrivateLogDir(String user) {
       File f;

       f = new File(rootPath, Config.DIR_ACCOUNTS);
       f = new File(f, user);
       f = new File(f, Config.DIR_LOGS);

       return f.toString();
   }

   static public String getUserDir(String folder, String user) {
        File f;

        f = new File(rootPath, Config.DIR_ACCOUNTS);
        f = new File(f, user);
        f = new File(f, folder);

        return f.toString();
   }

   static public String formatNumber(int number) {
       NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
       DecimalFormat formatter = (DecimalFormat) nf;
       formatter.applyPattern("#,###,###");

       return formatter.format(number);
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
    
    static public boolean moveToFolderNewName(String fileName, String folder, String user, String newFileName) {
        logger.info(ltag, "Moving to " + folder + " -> " + fileName + " new " + newFileName);
        
        try {
            File fsource = new File(fileName);
            String target = AppCore.getUserDir(folder, user) + File.separator + newFileName;
            File ftarget = new File(target);
            if (!fsource.renameTo(ftarget)) {
                logger.error(ltag, "Failed to rename file " + fileName + " to " + ftarget.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            logger.error(ltag, "Failed to move file: " + e.getMessage());
            return false;
        }
        
        return true;
        
    }

    static public boolean moveToFolder(String fileName, String folder, String user) {
        logger.info(ltag, "Moving to " + folder + " -> " + fileName);

        try {
            File fsource = new File(fileName);
            String target = AppCore.getUserDir(folder, user) + File.separator +
                    System.currentTimeMillis() + "-" + fsource.getName();

            File ftarget = new File(target);
            if (!fsource.renameTo(ftarget)) {
                logger.error(ltag, "Failed to rename file " + fileName);
                return false;
            }
        } catch (Exception e) {
            logger.error(ltag, "Failed to move file: " + e.getMessage());
            return false;
        }
        
        return true;
    }

    static public void moveToTrash(String fileName, String user) {
        moveToFolder(fileName, Config.DIR_TRASH, user);
    }

    static public void moveToLost(String fileName, String user) {
        moveToFolder(fileName, Config.DIR_LOST, user);
    }

    static public void moveToBank(String fileName, String user) { 
        moveToFolder(fileName, Config.DIR_BANK, user); 
    }

    static public void moveToImported(String fileName, String user) {
        moveToFolder(fileName, Config.DIR_IMPORTED, user);
    }

    static public boolean copyFile(InputStream is, String fdst) {
        File dest = new File(fdst);
        OutputStream os = null;
        
        try {
            os = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            logger.error(ltag, "Failed to copy file: " + e.getMessage());
            return false;
        } finally {
            try {
                if (is != null)
                    is.close();

                if (os != null)
                    os.close();

            } catch (IOException e) {
                logger.error(ltag, "Failed to copy file: " + e.getMessage());
            }
        }
        
        return true;
    }
    
    static public boolean copyFile(String fsrc, String fdst) {
        File source = new File(fsrc);
        File dest = new File(fdst);
        InputStream is = null;
        OutputStream os = null;
        
        logger.debug(ltag, "Copy " + fsrc + " to " + fdst);
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            logger.error(ltag, "Failed to copy file: " + e.getMessage());
            return false;
        } finally {
            try {
                if (is != null)
                    is.close();

                if (os != null)
                    os.close();

            } catch (IOException e) {
                logger.error(ltag, "Failed to finally copy file: " + e.getMessage());
            }
        }

        return true;
    }

    static public String loadFile(String fileName) {
        String jsonData = "";
        BufferedReader br = null;
        try {
            /*
            String line;
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new FileReader(fileName));
            System.out.println("loading2");
            while ((line = br.readLine()) != null) {
                System.out.println("loading3 " + line);
                //jsonData += line + "\n";
                sb.append(line);
            }
            System.out.println("loading4");
            */
            
            byte[] encoded = Files.readAllBytes(Paths.get(fileName));
            jsonData = new String(encoded);
            
        } catch (IOException e) {
            logger.error(ltag, "Failed to load file: " + e.getMessage());
            return null;
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                logger.error(ltag, "Failed to finally load file: " + e.getMessage());
                return null;
            }
        }

        return jsonData.toString();
    }

    static public byte[] loadFileToBytes(String path) {
        byte[] getBytes = {};
        try {
            File file = new File(path);
            getBytes = new byte[(int) file.length()];
            InputStream is = new FileInputStream(file);
            is.read(getBytes);
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getBytes;
    }

    static public boolean saveFile(String path, String data) {
        return saveFileAppend(path, data, false);
    }
    
    static public boolean saveFileAppend(String path, String data, boolean isAppend) {
        File f = new File(path);
        if (f.exists() && !isAppend) {
            logger.error(ltag, "File " + path + " already exists");
            return false;
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path, isAppend));
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

    static public boolean saveFileFromBytes(String path, byte[] bytes) {
       try {
           File file = new File(path);
           if (file.exists()) {
               logger.error(ltag, "File exists: " + path);
               return false;
           }

           FileOutputStream fos = new FileOutputStream(file);
           fos.write(bytes);
           fos.close();
       } catch (IOException e) {
           logger.error(ltag, "Failed to write file: " + e.getMessage());
           return false;
       }

       return true;
    }

    static public void deleteFile(String path) {
        File f = new File(path);

        logger.debug(ltag, "Deleting " + path);
        f.delete();
    }

    static public int getFilesCount(String dir, String user) {
       String path = getUserDir(dir, user);
       File rFile;
       int rv;

       try {
           rFile = new File(path);
           rv = rFile.listFiles().length;
       } catch (Exception e) {
           logger.error(ltag, "Failed to read directory: " + e.getMessage());
           return 0;
       }

       return rv;
    }

    static public String getMD5(String data) {
        byte[] bytesOfMessage = data.getBytes();
        byte[] digest;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            digest = md.digest(bytesOfMessage);
        } catch (NoSuchAlgorithmException e) {
            logger.error(ltag, "No such algorithm MD5: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error(ltag, "MD5 error: " + e.getMessage());
            return null;
        }

        return toHexString(digest);
    }

    static public String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    static public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
                    Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }


    static public String generateHex() {
        String AB = "0123456789ABCDEF";

        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));

        return sb.toString();
    }


    public static String padString(String string, int length, char padding) {
        return String.format("%" + length + "s", string).replace(' ', padding);
    }

    public static int charCount(String pown, char character) {
        return pown.length() - pown.replace(Character.toString(character), "").length();
    }
    
    public static String[] getFilesInDir(String dir, String user) {
        String path = AppCore.getUserDir(Config.DIR_ID, user);
        
        String[] rv;
        int c = 0;

        File dirObj = new File(path);
        if (!dirObj.exists()) {
            return new String[0];
        }
        
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;
            
            c++;
        }
        
        rv = new String[c];
        c = 0;
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;
            
            rv[c++] = file.getName();
        }
        
        return rv;       
    }
    
    public static String[] getDirs() {
        String[] rv;
        int c = 0;

        File dirObj = new File(rootPath + File.separator + Config.DIR_ACCOUNTS);
        for (File file: dirObj.listFiles()) {
            if (!file.isDirectory())
                continue;
            
            c++;
        }
        
        rv = new String[c];
        c = 0;
        for (File file: dirObj.listFiles()) {
            if (!file.isDirectory())
                continue;
            
            rv[c++] = file.getName();
        }
        
        return rv;       
    }
    
    
    public static void copyTemplatesFromJar(String user) {
        int d;
        String templateDir, fileName;

	templateDir = AppCore.getUserDir(Config.DIR_TEMPLATES, user);
	fileName = templateDir + File.separator + "jpeg1.jpg";
	File f = new File(fileName);
	if (!f.exists()) {
            for (int i = 0; i < AppCore.getDenominations().length; i++) {
                d = AppCore.getDenominations()[i];
                fileName = "jpeg" + d + ".jpg";
                   
                URL u = AppCore.class.getClassLoader().getResource("resources/" + fileName);
                if (u == null)
                    continue;
                
                String url;
                try {
                    url = URLDecoder.decode(u.toString(), "UTF-8");
                }  catch (UnsupportedEncodingException e) {
                    logger.error(ltag, "Failed to decode url");
                    return;
                }

                int bang = url.indexOf("!");
                String JAR_URI_PREFIX = "jar:file:";
                JarFile jf;
                
                try {
                    if (url.startsWith(JAR_URI_PREFIX) && bang != -1) {
                        jf = new JarFile(url.substring(JAR_URI_PREFIX.length(), bang)) ;
                    } else {
                        logger.error(ltag, "Invalid jar");
                        return;
                    }
                
                    for (Enumeration<JarEntry> entries = jf.entries(); entries.hasMoreElements();) {
                        JarEntry entry = entries.nextElement();

                        if (entry.getName().equals("resources/" + fileName)) {
                            InputStream in = jf.getInputStream(entry);
                            String dst = AppCore.getUserDir(Config.DIR_TEMPLATES, user);
                            dst += File.separator + fileName;

                            AppCore.copyFile(in, dst);
                        }
                    }
                } catch (IOException e) {
                    logger.error(ltag, "Failed to copy templates: " + e.getMessage());
                    return ;
                }          
            }
        }
    }
    
    
    public static String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
	Date date = new Date();
        
        return dateFormat.format(date);
    }
    
    
    public static boolean isCoinOk(String path) {
        try {
            CloudCoin cc = new CloudCoin(path);
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse coin: " + path + " error: " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    
    
}
