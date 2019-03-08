# AndroidApp

CloudCoin Pocket Bank v. 2.0



## The app comprises several Servants that are independent modules and can be compiled as a standalone applcation, if necessary

The app meets the standards: https://cloudcoinconsortium.org/software.html


All servants can be invoked through a direct function call or a file command can be issued to the Command Folder (to be implemented)

If called by a direct call, each Servant has to be registered in the ServiceRegistry


```java

sr = new ServantRegistry();
sr.registerServants(new String[]{
	"Echoer",
	"Authenticator",
	"ShowCoins",
	"Unpacker",
	"Authenticator",
	"Grader",
	"FrackFixer",		
	"Exporter"
}, AppCore.getRootPath(), alogger);


```

A callback can be passed to the Servant. If so, the callback will be triggered in the Servant.

Examples:

```java

Echoer e = (Echoer) sr.getServant("Echoer");
e.launch(new EchoCb());

ShowCoins sc = (ShowCoins) sr.getServant("ShowCoins");
sc.launch(new ShowCoinsCb());

Unpacker up = (Unpacker) sr.getServant("Unpacker");
up.launch(new UnpackerCb());

Authenticator at = (Authenticator) sr.getServant("Authenticator");
at.launch(new AuthenticatorCb());

Grader gd = (Grader) sr.getServant("Grader");
gd.launch(new GraderCb());


FrackFixer ff = (FrackFixer) sr.getServant("FrackFixer");
ff.launch(new FrackFixererCb());
if (sr.isRunning("FrackFixer")) {
	Log.v(TAG, "FrackFixer is running");
}


Exporter ex = (Exporter) sr.getServant("Exporter");
ex.launch(Config.DIR_DEFAULT_USER, type, values, exportTag, new ExporterCb());


```


Callback example:


```java

class EchoCb implements CallbackInterface {
	public void callback(Object result) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				echoResult = ECHO_RESULT_OK;
				startFrackFixerService();
			}
		});
	}
}

```



