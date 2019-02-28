package global.cloudcoin.ccbank.Authenticator;

public class AuthenticatorResult {
    public int totalFilesProcessed;

    public int totalCoinsProcessedInFile;
    public int totalCoinsInFile;

    public int totalRAIDAProcessed;

    public AuthenticatorResult() {
        totalFilesProcessed = totalRAIDAProcessed = 0;
        totalCoinsProcessedInFile = totalCoinsInFile = 0;
    }
}
