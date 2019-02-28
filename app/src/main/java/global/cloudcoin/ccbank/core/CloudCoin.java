package global.cloudcoin.ccbank.core;

import java.security.SecureRandom;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.InputStream;
import java.io.BufferedInputStream;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.Calendar;

import android.content.res.AssetFileDescriptor;
import android.content.Context;

//import global.cloudcoin.ccbank.IncomeFile;
import global.cloudcoin.ccbank.core.RAIDA;

public class CloudCoin {

	public int nn; 
	public int sn; 
	public String[] ans; 
	public String[] pans;
	public int[] pastStatus; 
 
	private String ed;
	private String edHex;
	private int hp;
	public String aoid; 
	public String fileName;
	public static final int YEARSTILEXPIRE = 2;
	public String extension; //"suspect", "bank", "fracked", "counterfeit"
	public String[] gradeStatus = new String[3]; //What passed, what failed, what was undetected
	public String tag;
	public String fullFileName;

	public static String TAG = "CLOUDCOIN";
	
	static int PAST_STATUS_PASS = 1;
	static int PAST_STATUS_FAIL = 2;
	static int PAST_STATUS_ERROR = 3;
	static int PAST_STATUS_UNDETECTED = 4;

	public void initCommon() {

		pans = new String[RAIDA.TOTAL_RAIDA_COUNT];
		pastStatus = new int[RAIDA.TOTAL_RAIDA_COUNT];

		for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++){
			this.pans[i] = generatePan();
			this.pastStatus[i] = PAST_STATUS_UNDETECTED;
		}

	}

	public CloudCoin(String fileName) throws JSONException {
		String data = AppCore.loadFile(fileName);

		if (data == null)
			throw(new JSONException("Failed to open file"));

		JSONObject o = new JSONObject(data);
		JSONArray incomeJsonArray = o.getJSONArray("cloudcoin");

		JSONObject childJSONObject = incomeJsonArray.getJSONObject(0);

		nn = childJSONObject.getInt("nn");
		sn = childJSONObject.getInt("sn");

		if (sn < 0 || sn > 16777217)
            throw(new JSONException("Invalid SN number: " + sn));

		if (nn < 0 || nn > 65535)
            throw(new JSONException("Invalid NN number: " + nn));

		JSONArray an = childJSONObject.getJSONArray("an");

		ed = childJSONObject.optString("ed");
		aoid = childJSONObject.optString("aoid");

		ans = toStringArray(an);
		if (ans.length != RAIDA.TOTAL_RAIDA_COUNT)
		    throw(new JSONException("Wrong an count"));

		pans = new String[RAIDA.TOTAL_RAIDA_COUNT];

		setPansToAns();
	}

	public CloudCoin(int nn, int sn, String[] ans, String ed, String aoid, String tag) {
		initCommon();

		this.nn = nn;
		this.sn = sn;
		this.ans = ans;
		this.ed = ed;
		this.hp = RAIDA.TOTAL_RAIDA_COUNT;
		this.aoid = aoid;

		this.tag = tag;
		this.fileName = getFileName();
	}

	public String getFileName() {
		String result;

		result = getDenomination() + ".CloudCoin." + this.nn + "." + this.sn + ".";
		if (this.tag != null && !this.tag.isEmpty()) {
			result += this.tag + ".";
		}

		return result;
	}

	public static String[] toStringArray(JSONArray array) {
		if (array == null)
			return null;

		String[] arr = new String[array.length()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = array.optString(i);
		}

		return arr;
	}

	public String getJson() {
		String json;

		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		year = year + YEARSTILEXPIRE;

		String expDate = month + "-" + year;

		json = "{'cloudcoin':[{'nn':" + nn + ",'sn':" + sn + ",'an':['";
		for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
			json += ans[i];
			if (i != RAIDA.TOTAL_RAIDA_COUNT - 1) {
				json += "','";
			}
		}
		
		if (!aoid.startsWith("\"") && !aoid.startsWith("'"))
			aoid = "'" + aoid + "'";

		json += "'], 'ed': '" + expDate + "', 'aoid': [" + aoid + "]}]}";

		return json;
	}

	private String generatePan() {  
		String AB = "0123456789ABCDEF";

		SecureRandom rnd = new SecureRandom();
		StringBuilder sb = new StringBuilder(RAIDA.TOTAL_RAIDA_COUNT);
		for(int i = 0; i < 32; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));

		return sb.toString();
	}

	public int getDenomination() {  
		if (this.sn < 1 )
			return 0;
		else if (this.sn < 2097153) 
			return 1;
		else if (this.sn < 4194305) 
			return 5;
	        else if (this.sn < 6291457) 
			return 25;
		else if (this.sn < 14680065) 
			return 100;
		else if (this.sn < 16777217) 
			return 250;

	        return 0;
	}

	static int ordinalIndexOf(String str, String substr, int n) {
		int pos = str.indexOf(substr);
		while (--n > 0 && pos != -1)
			pos = str.indexOf(substr, pos + 1);
		return pos;
	}


	public void setPansToAns(){
		for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
			pans[i] = ans[i];
		}
	}

/*
	public void setAnsToPansIfPassed() {
		for (int i = 0; i < raidaCnt; i++) {
			if (pastStatus[i] == PAST_STATUS_PASS) {
				ans[i] = pans[i];
			}
		}
	}*/
/*
	public void calculateHP(){
		hp = raidaCnt;

		for (int i = 0; i < raidaCnt; i++) {
			if( this.pastStatus[i] == PAST_STATUS_FAIL) 
				hp--;
		}
	}*/

	public void calcExpirationDate() {
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		year = year + YEARSTILEXPIRE;

		ed = month + "-" + year;
		edHex = Integer.toHexString(month);
		edHex += Integer.toHexString(year);
	}

/*
	private String rateToString(int count) {
		double pct = (double) count / (double) raidaCnt;

		Log.v(TAG, "count " + count + " r " + raidaCnt + " pct " +pct);

		if (pct == 1)
			return "100%";

		if (pct == 0)
			return "None";		

		if (pct > 0.68)
			return "Super Majority";

		if (pct > 0.52)
			return "Majority";

		if (pct < 5)
			return "Super Minority";

		return "Minority";
	}
*/
/*
	public void gradeStatus(){
		int passed = 0;
		int failed = 0;
		int other = 0;

		String passedDesc = "";
		String failedDesc = "";
		String otherDesc = "";

		String internalAoid = ">";

		for (int i = 0; i < raidaCnt; i++) {
			if (pastStatus[i] == PAST_STATUS_PASS) {
				passed++;
				internalAoid += "p";
			} else if (pastStatus[i] == PAST_STATUS_FAIL) {
				internalAoid += "f";
				Log.v(TAG, "Failed " + i);
				failed++;
			} else {
				internalAoid += "u";
				other++;
			}
		}

		internalAoid += "<";
		this.aoid = internalAoid;

		gradeStatus[0] = rateToString(passed);
		gradeStatus[1] = rateToString(failed);
		gradeStatus[2] = rateToString(other);

		if (other > (raidaCnt / 2) - 1) {
			extension = "suspect";
		} else if (failed > passed || failed > 5) {
			extension = "counterfeit";
		} else if (failed > 0) {
			extension = "fracked";
		} else {
			extension = "bank";
		}

		Log.v(TAG, "Got extension " + extension + "; passed = " +passed + " failed = " +failed + " other = " +other);
	}
*/
	public byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}


}
