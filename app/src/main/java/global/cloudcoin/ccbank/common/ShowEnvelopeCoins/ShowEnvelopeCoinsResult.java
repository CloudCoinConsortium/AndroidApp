package global.cloudcoin.ccbank.ShowEnvelopeCoins;

public class ShowEnvelopeCoinsResult {
    public int[] coins;
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public int status;
    public int[][] counters;
}
