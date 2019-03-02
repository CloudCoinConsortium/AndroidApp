package global.cloudcoin.ccbank.FrackFixer;

public class FrackFixerResult {
    public int fixed;

    public int failed;

    public int status;

    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public FrackFixerResult() {
        fixed = failed = 0;
        status = STATUS_PROCESSING;
    }
}
