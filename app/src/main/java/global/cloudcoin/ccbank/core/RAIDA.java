package global.cloudcoin.ccbank.core;

import android.util.Log;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import android.content.Context;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import android.os.Handler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.microedition.khronos.opengles.GL;

import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.DetectionAgent;
import global.cloudcoin.ccbank.MainActivity;

public class RAIDA {


	static String ltag = "RAIDA";

	public static int TOTAL_RAIDA_COUNT = 25;
	static int THREAD_POOL_SIZE = 8;


	public DetectionAgent[] agents;

	ExecutorService service;

	public GLogger logger;

	final static int STATUS_ECHO_OK = 1;
	final static int STATUS_ECHO_FAILED = 2;


	public RAIDA(GLogger logger) {
		agents = new DetectionAgent[TOTAL_RAIDA_COUNT];
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			agents[i] = new DetectionAgent(i, Config.CONNECTION_TIMEOUT, logger);
		}

		this.logger = logger;
	}

	public void setExactUrls(String[] urls) {
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			logger.info(ltag, "Set RAIDA url to " + urls[i]);
			agents[i].setExactFullUrl(urls[i]);
		}
	}

	public void setUrl(String ip, int basePort) {
		logger.info(ltag, "Set RAIDA ip " + ip + ":" + basePort);
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			agents[i].setFullUrl(ip, basePort);
		}
	}

	public String[] getRAIDAURLs() {
		String[] data;

		data = new String[TOTAL_RAIDA_COUNT];
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			data[i] = agents[i].getFullURL();
		}

		return data;
	}

/*

	public String[] getTickets(int[] triad, String[] ans, int nn, int sn, int denomination) {
		final String[] returnTickets = new String[3];
		final int[] triadFinal = triad;
		final String[] ansFinal = ans;
		final int nnFinal = nn;
		final int snFinal = sn;
		final int denominationFinal = denomination;

		service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		List<Future<Runnable>> futures = new ArrayList<Future<Runnable>>();
		returnTickets[0] = returnTickets[1] = returnTickets[2] = "error";
		for (int i = 0; i < 3; i++) {
			final int iFinal = i;
			Future f = service.submit(new Runnable() {
				public void run() {
					Log.v(TAG, "run agent=" + triadFinal[iFinal] + " i="+iFinal+ " a="+ansFinal[iFinal]);
					returnTickets[iFinal] = agents[triadFinal[iFinal]].get_ticket(nnFinal, snFinal, ansFinal[iFinal], denominationFinal);
				}
			});
			futures.add(f);
		}

		for (Future<Runnable> f : futures) {
			try {
				f.get(CONNECTION_TIMEOUT * 2, TimeUnit.MILLISECONDS);
			} catch (ExecutionException e) {
				Log.v(TAG, "Error executing the task");
				e.printStackTrace();
			} catch (TimeoutException e) {
				Log.v(TAG, "Timeout connection to the server");
			} catch (InterruptedException e) {
				Log.v(TAG, "Task interrupted");
			}
		}

		service.shutdownNow();

		return returnTickets;
	}

	public void fixCoin(CloudCoin brokeCoin) {
		boolean[] raidaIsBroke = new boolean[TOTAL_RAIDA_COUNT];

		for (int guid_id = 0; guid_id < TOTAL_RAIDA_COUNT; guid_id++)
			raidaIsBroke[guid_id] = false;

		brokeCoin.setAnsToPans();

		for (int guid_id = 0; guid_id < TOTAL_RAIDA_COUNT; guid_id++) { 
			if (brokeCoin.pastStatus[guid_id] == CloudCoin.PAST_STATUS_FAIL && !raidaIsBroke[guid_id]) { 
				FixitHelper fixer = new FixitHelper(guid_id);
				int corner = 1;

				Log.v(TAG, "Iteration " + guid_id + " fc=" + fixer.currentTriad[0] + " fc1= " + fixer.currentTriad[1] + " fc2= " + fixer.currentTriad[2]);

				while (!fixer.finnished) {
				String[] trustedServerAns = new String[] {
					brokeCoin.ans[fixer.currentTriad[0]],
					brokeCoin.ans[fixer.currentTriad[1]],
					brokeCoin.ans[fixer.currentTriad[2]]
				};

					Log.v(TAG, "ans0="+trustedServerAns[0] + " ans1="+trustedServerAns[1] + " ans2="+trustedServerAns[2]);

					String fix_result = "";
					String[] tickets = getTickets(fixer.currentTriad, trustedServerAns, brokeCoin.nn, brokeCoin.sn, brokeCoin.getDenomination());

					if (tickets[0].equals("error") || tickets[2].equals("error") ||  tickets[2].equals("error")) {
	
						corner++;
						fixer.setCornerToCheck(corner);

						if (corner == 5)
							raidaIsBroke[guid_id] = true; 
					} else {
						fix_result = agents[guid_id].fix(fixer.currentTriad, tickets[0], tickets[1], tickets[2], brokeCoin.ans[guid_id]);
						if (fix_result.equalsIgnoreCase("success")) { 
							brokeCoin.pastStatus[guid_id] = CloudCoin.PAST_STATUS_PASS;
							fixer.finnished = true;
						} else {
							corner++;
							fixer.setCornerToCheck(corner);

							if (corner == 5)
								raidaIsBroke[guid_id] = true; 
						}
		                        }

				}

				//Handler h = ((MainActivity) ctx).getHandler();
				//h.sendEmptyMessage(0);
			}

		}

		brokeCoin.setAnsToPansIfPassed();
		brokeCoin.calculateHP();
        	brokeCoin.calcExpirationDate();
	        brokeCoin.gradeStatus();
	}

	public void detectCoin(CloudCoin ccIn) {
		service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		List<Future<Runnable>> futures = new ArrayList<Future<Runnable>>();

		final CloudCoin cc = ccIn;
		final int[] pastStatuses = new int[TOTAL_RAIDA_COUNT];
		int serversDone = 0;

		final Context ctx = this.ctx;

		final int serversDoneFinal = serversDone;
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			final int iFinal = i;
			Future f = service.submit(new Runnable() {
				public void run() {
					pastStatuses[iFinal] = agents[iFinal].detect(cc.nn, cc.sn, cc.ans[iFinal], cc.pans[iFinal], cc.getDenomination());

				//	Handler h = ((global.cloudcoin.ccbank.MainActivity) ctx).getHandler();
				//	h.sendEmptyMessage(0);
				}
			});
			futures.add(f);
		}

		for (Future<Runnable> f : futures) {
			try {
				f.get(CONNECTION_TIMEOUT * 2, TimeUnit.MILLISECONDS);
			} catch (ExecutionException e) {
				Log.v(TAG, "Error executing the task");
				e.printStackTrace();
			} catch (TimeoutException e) {
				Log.v(TAG, "Timeout during connection to the server");
			} catch (InterruptedException e) {
				Log.v(TAG, "Task interrupted");
			}
		}

		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			ccIn.pastStatus[i] = pastStatuses[i];
		}
		

		service.shutdownNow();

		ccIn.setAnsToPansIfPassed();
		ccIn.calculateHP();
		ccIn.calcExpirationDate();
		ccIn.gradeStatus();

	}
	*/

	public long[] getLastLatencies() {
		long[] responses;

		responses = new long[TOTAL_RAIDA_COUNT];
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++)
			responses[i] = agents[i].getLastLatency();

		return responses;
	}

	public String[] query(String[] requests) {
		return query(requests, null);
	}

	public String[] query(String[] requests, String[] posts) {
		service = AppCore.getServiceExecutor();
		List<Future<Runnable>> futures = new ArrayList<Future<Runnable>>();

		final String[] results = new String[TOTAL_RAIDA_COUNT];

		if (requests.length != TOTAL_RAIDA_COUNT) {
			logger.error(ltag, "Internal error. Wrong parameters");
			return null;
		}

		if (posts != null) {
			if (posts.length != TOTAL_RAIDA_COUNT) {
				logger.error(ltag, "Internal error. Wrong post parameters");
				return null;
			}
		} else {
			posts = new String[TOTAL_RAIDA_COUNT];
			for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
				posts[i] = null;
			}
		}

		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			final int iFinal = i;
			final String request = requests[i];
			final String post = posts[i];

			Future f = service.submit(new Runnable() {
				public void run() {
					results[iFinal] = agents[iFinal].doRequest(request, post);
				}
			});
			futures.add(f);
		}

		for (Future<Runnable> f : futures) {
			try {
				f.get(Config.CONNECTION_TIMEOUT * 2, TimeUnit.MILLISECONDS);
			} catch (ExecutionException e) {
				logger.error(ltag, "Error executing the task");
			} catch (TimeoutException e) {
				logger.error(ltag, "Timeout during connection to the server");
			} catch (InterruptedException e) {
				logger.error(ltag, "Task interrupted");
			}
		}

		return results;
	}

}
