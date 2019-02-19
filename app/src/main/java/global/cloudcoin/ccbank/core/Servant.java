package global.cloudcoin.ccbank.core;

import global.cloudcoin.ccbank.core.GLoggerInterface;

public class Servant {

    final static int STATUS_RUNNING = 1;
    final static int STATUS_WAITING = 2;

    private String rootDir;
    private String name;
    private int status;

    private GLogger logger;

    public Servant(String name, String rootDir, GLogger logger) {
        this.name = name;
        this.rootDir = rootDir;
        this.status = STATUS_WAITING;
        this.logger = logger;

        logger.info("xxx", "zzz123123412412");
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setLogger(GLogger logger) {
        this.logger = logger;
    }


}
