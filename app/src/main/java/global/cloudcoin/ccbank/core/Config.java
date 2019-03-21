package global.cloudcoin.ccbank.core;

public class Config {

    public static String DIR_ROOT = "CloudCoin";
    public static String DIR_BANK = "Bank";
    public static String DIR_COUNTERFEIT = "Counterfeit";
    public static String DIR_DANGEROUS = "Dangerous";
    public static String DIR_DEPOSIT = "Deposit";
    public static String DIR_DETECTED = "Detected";
    public static String DIR_EXPORT = "Export";
    public static String DIR_EMAILOUT = "EmailOut";
    public static String DIR_FRACKED = "Fracked";
    public static String DIR_GALLERY = "Gallery";
    public static String DIR_ID = "ID";
    public static String DIR_IMPORT = "Import";
    public static String DIR_IMPORTED = "Imported";
    public static String DIR_LOGS = "Logs";
    public static String DIR_LOST = "Lost";
    public static String DIR_MIND = "Mind";
    public static String DIR_PARTIAL = "Partial";
    public static String DIR_PAYFORWARD = "PayForward";
    public static String DIR_PREDETECT = "Predetect";
    public static String DIR_RECEIPTS = "Receipts";
    public static String DIR_REQUESTS = "Requests";
    public static String DIR_REQUESTRESPONSE = "RequestsResponse";
    public static String DIR_SUSPECT = "Suspect";
    public static String DIR_TEMPLATES = "Templates";
    public static String DIR_TRASH = "Trash";
    public static String DIR_TRUSTEDTRANSFER = "TrustedTransfer";
    public static String DIR_CONFIG = "Config";
    public static String DIR_SENT = "Sent";



    public static String DIR_ACCOUNTS = "Accounts";
    public static String DIR_DEFAULT_USER = "DefaultUser";
    public static String DIR_MAIN_LOGS = "Logs";

    public static int THREAD_POOL_SIZE = 8;

    public static int CONNECTION_TIMEOUT = 5000; // ms

    public static int MAX_ALLOWED_FAILED_RAIDAS = 3;

    public static String BACKUP0_RAIDA_IP = "95.179.159.178";
    public static int BACKUP0_RAIDA_PORT = 40000;

    public static String BACKUP1_RAIDA_IP = "66.42.61.239";
    public static int BACKUP1_RAIDA_PORT = 40000;

    public static String BACKUP2_RAIDA_IP = "45.32.134.65";
    public static int BACKUP2_RAIDA_PORT = 40000;


    public static String RAIDA_STATUS_READY = "ready";



    public static int MAX_ALLOWED_LATENCY = 20000;


    public static int IDX_1 = 0;
    public static int IDX_5 = 1;
    public static int IDX_25 = 2;
    public static int IDX_100 = 3;
    public static int IDX_250 = 4;

    public static int IDX_FOLDER_BANK = 0;
    public static int IDX_FOLDER_FRACKED = 1;
    public static int IDX_FOLDER_LOST = 2;
    public static int IDX_FOLDER_LAST = 3;

    public static int DEFAULT_NN = 1;

    public static String DEFAULT_TAG = "CC";

    public static int DEFAULT_MAX_COINS_MULTIDETECT = 400;


    public static int PASS_THRESHOLD = 20;

    public static int TYPE_STACK = 1;
    public static int TYPE_JPEG = 2;
    public static int TYPE_CSV = 3;

    public static String JPEG_MARKER = "01C34A46494600010101006000601D05";


    public static String SENDER_DOMAIN = "teleportnow.cc";

    public static int RAIDANUM_TO_QUERY_BY_DEFAULT = 14;

    public static int MAX_COMMON_LOG_LENGTH_MB = 128;
}
