package global.cloudcoin.ccbank.Sender;

public class SenderResult {
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public int status;

    public SenderResult() {
        status = STATUS_PROCESSING;
    }
}
