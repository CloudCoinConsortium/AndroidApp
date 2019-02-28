package global.cloudcoin.ccbank.core;

import android.util.Log;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import global.cloudcoin.ccbank.MainActivity;
import global.cloudcoin.ccbank.core.CloudCoin;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;



class DetectionAgent {

	GLogger logger;

	String ltag = "DetectionAgent";

	int connectionTimeout;
	int readTimeout;
	private long dms;

	public int lastDetectStatus = CloudCoin.PAST_STATUS_ERROR;
	public String lastRequest = "empty";
	public String lastResponse = "empty";

	public String lastTicket = "empty";
	public String lastTicketStatus = "empty";
	public String lastFixStatus = "empty";

	private String fullURL;


	static String TAG = "RAIDADetectionAgent";

	int RAIDANumber;

	public DetectionAgent(int RAIDANumber, int timeout, GLogger logger) {

		this.RAIDANumber = RAIDANumber;


		// TODO: remove +2 seconds. Now it is a workaround for slow RAIDAs
		this.readTimeout = timeout + 2000;
		this.connectionTimeout = timeout;

		this.logger = logger;

		setDefaultFullUrl();
	}

	public void setExactFullUrl(String url) {
		this.fullURL = url;
	}

	public void setDefaultFullUrl() {
		this.fullURL = "https://RAIDA" + this.RAIDANumber + ".cloudcoin.global";
	}

	public void setFullUrl(String ip, int basePortArg) {
		int basePort = basePortArg + this.RAIDANumber;

		this.fullURL = "https://" + ip + ":" + basePort;
	}

	public String getFullURL() {
		return this.fullURL;
	}

/*

	public String get_ticket(int nn, int sn, String an, int d ) { 
		long tsBefore, tsAfter;

		lastRequest = fullURL + "get_ticket?nn="+nn+"&sn="+sn+"&an="+an+"&pan="+an+"&denomination="+d;
		tsBefore = System.currentTimeMillis();

		lastResponse = doRequest(lastRequest);
		if (lastResponse == null)
			return "error";

                tsAfter = System.currentTimeMillis();
		dms = tsAfter - tsBefore;

		if (lastResponse.contains("ticket")) {
			String[] KeyPairs = this.lastResponse.split(",");
			String message = KeyPairs[3];
			int startTicket = ordinalIndexOf( message, "\"", 3);
			int endTicket = ordinalIndexOf( message, "\"", 4);
			lastTicket = message.substring(startTicket + 1, endTicket);
			lastTicketStatus = "ticket";

			return lastTicket;
		}

		return "error";

	}

	public int detect(int nn, int sn, String an, String pan, int d) {
		long tsBefore, tsAfter;

		lastRequest = fullURL + "detect?nn="+nn+"&sn="+sn+"&an="+an+"&pan="+pan+"&denomination="+d;
		tsBefore = System.currentTimeMillis();

		lastResponse = doRequest(lastRequest);

		tsAfter = System.currentTimeMillis();
		dms = tsAfter - tsBefore;

		if (lastResponse == null) {
			lastDetectStatus = CloudCoin.PAST_STATUS_ERROR;
		} else if (lastResponse.contains("pass")) {
			lastDetectStatus = CloudCoin.PAST_STATUS_PASS;
		} else if (lastResponse.contains("fail") && lastResponse.length() < 200) {
			lastDetectStatus = CloudCoin.PAST_STATUS_FAIL;
		} else {
			lastDetectStatus = CloudCoin.PAST_STATUS_ERROR;
		}
	
		return lastDetectStatus;
	}*/

	public long getLastLatency() {
		return dms;
	}

	public String doRequest(String url, String post) {
		long tsBefore, tsAfter;
		int c;
		String data;
		StringBuilder sb = new StringBuilder();

		if (fullURL == null) {
			logger.error(ltag, "Skipping raida: " + RAIDANumber);
			return null;
		}

		String urlIn = fullURL + "/service/" + url;

		String method = (post == null) ? "GET" : "POST";

		logger.debug(ltag, method + " url " + urlIn);

		tsBefore = System.currentTimeMillis();

		URL cloudCoinGlobal;
		HttpURLConnection urlConnection = null;
		try {
			cloudCoinGlobal = new URL(urlIn);
			urlConnection = (HttpURLConnection) cloudCoinGlobal.openConnection();
			urlConnection.setConnectTimeout(connectionTimeout);
			urlConnection.setReadTimeout(readTimeout);
			urlConnection.setRequestProperty("User-Agent", "Android CloudCoin App");

			if (post != null) {
				logger.debug(ltag, post);

				byte[] postDataBytes = post.getBytes("UTF-8");

				urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				urlConnection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
				urlConnection.setDoOutput(true);
				urlConnection.getOutputStream().write(postDataBytes);
			}

			if (urlConnection.getResponseCode() != 200) {
				logger.error(ltag, "Invalid response from server " + urlIn + ":" + urlConnection.getResponseCode());
				return null;
			}

			InputStream input = urlConnection.getInputStream();
			while (((c = input.read()) != -1)) { 
				sb.append((char) c);
			}

			input.close();
			logger.debug(ltag, "Response: "+ sb.toString() + " url=" + urlIn);

			tsAfter = System.currentTimeMillis();
			dms = tsAfter - tsBefore;

			return sb.toString();
		} catch (MalformedURLException e) {
			logger.error(ltag, "Failed to fetch. Malformed URL " + urlIn);
			return null;
		} catch (IOException e) {
			logger.error(ltag, "Failed to fetch URL: " + e.getMessage());
			return null;
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
	}
/*
	public String fix(int[] triad, String m1, String m2, String m3, String pan) {
		long tsBefore, tsAfter;

		lastFixStatus = "error";
		int f1 = triad[0];
		int f2 = triad[1];
		int f3 = triad[2];
		lastRequest = fullURL + "fix?fromserver1="+f1+"&message1="+m1+"&fromserver2="+f2+"&message2="+m2+"&fromserver3="+f3+"&message3="+m3+"&pan="+pan;
		tsBefore = System.currentTimeMillis();

		lastResponse = doRequest(lastRequest);
		if (lastResponse == null)
			return "error";

		tsAfter = System.currentTimeMillis();
		dms = tsAfter - tsBefore;

		if (lastResponse.contains("success")) {
			lastFixStatus = "success";
			return "success";
		}

		return "error";
	}
*/


	private int ordinalIndexOf(String str, String substr, int n) {
		int pos = str.indexOf(substr);
		while (--n > 0 && pos != -1)
			pos = str.indexOf(substr, pos + 1);

		return pos;
	}

}


