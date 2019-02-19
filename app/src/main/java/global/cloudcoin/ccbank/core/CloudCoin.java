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
 
	public String ed; 
	public String edHex;
	public int hp; 
	public String aoid; 
	public String fileName;
	public String json;
	public byte[] jpeg;
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

	public static String JPEGAOID = "204f42455920474f4420262044454645415420545952414e54532000";
	
	
	int raidaCnt;

	public void initCommon() {
		raidaCnt = RAIDA.TOTAL_RAIDA_COUNT;

		pans = new String[raidaCnt];
		pastStatus = new int[raidaCnt];

		for (int i = 0; i < raidaCnt; i++){
			this.pans[i] = generatePan();
			this.pastStatus[i] = PAST_STATUS_UNDETECTED;
		}

		json = "";
		jpeg = null;
	}

	public CloudCoin(int nn, int sn, String[] ans, String ed, String aoid, String tag) {
		initCommon();

		this.nn = nn;
		this.sn = sn;
		this.ans = ans;
		this.ed = ed;
		this.hp = raidaCnt;
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
	public CloudCoin(String dummy) {

	}

/*
	public CloudCoin(IncomeFile file) throws Exception {
		raidaCnt = RAIDA.TOTAL_RAIDA_COUNT;
		int cnt;
		FileInputStream fis;


		initCommon();

		ans = new String[raidaCnt];
		tag = file.fileTag;
		fullFileName = file.fileName;

		if (file.fileType == IncomeFile.TYPE_JPEG) {
			String wholeString ="";
			byte[] jpegHeader = new byte[455];
			int startAn, endAn;
			
			try {
				fis = new FileInputStream(file.fileName);
				cnt = fis.read(jpegHeader);
				wholeString = toHexadecimal(jpegHeader);
				fis.close();
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found");
				throw new Exception();
			} catch (IOException e) {
				Log.e(TAG, "Error while reading file");
				throw new Exception();
			}
				
			startAn = 40; 
			endAn = 72;

			for (int i = 0; i < raidaCnt; i++) {
				ans[i] = wholeString.substring(startAn + (i * 32), endAn + (i * 32));
			}

			aoid = wholeString.substring(840, 895);
			hp = raidaCnt;
			ed = wholeString.substring(898, 902);
			nn = Integer.parseInt(wholeString.substring(902, 904), 16);
			sn = Integer.parseInt(wholeString.substring(904, 910), 16);


		} else if (file.fileType == IncomeFile.TYPE_STACK) {
			String jsonData;

			jsonData = loadJSON(file.fileName);
			if (jsonData == null) {
				Log.e(TAG, "Failed to parse json: " + file.fileName);
				throw new Exception("Failed to parse json");
			}

			try {
				JSONObject o = new JSONObject(jsonData);
				JSONArray incomeJsonArray = o.getJSONArray("cloudcoin");

				JSONObject childJSONObject = incomeJsonArray.getJSONObject(0);
				int nn     = childJSONObject.getInt("nn");
				int sn     = childJSONObject.getInt("sn");
				JSONArray an = childJSONObject.getJSONArray("an");
				String ed     = childJSONObject.getString("ed");
				String aoid = childJSONObject.getString("aoid");

				aoid = aoid.replace("[", "");
				aoid = aoid.replace("]", "");

				this.nn = nn;	
				this.sn = sn;
				this.ans = toStringArray(an);
				this.ed = ed;
				this.aoid = aoid;
			} catch (JSONException e) {
				Log.e(TAG, "Stack file " + file.fileName + " is corrupted: " + e.getMessage());
				throw new Exception("Stack file " + file.fileName + " is corrupted: " + e.getMessage());
			}

			Log.v(TAG, "AOID " + this.aoid);

			int indexStartOfStatus = this.aoid.indexOf(">");
			int indexEndOfStatus = this.aoid.indexOf("<");

			if (indexStartOfStatus != -1 && indexEndOfStatus != -1) {
				String rawStatus = aoid.substring(indexStartOfStatus + 1, indexEndOfStatus);
				Log.v(TAG, "Raw status: " + rawStatus);
				for (int i = 0; i < raidaCnt; i++) {
					if (rawStatus.charAt(i) == 'p') {
						this.pastStatus[i] = PAST_STATUS_PASS;
					} else if (rawStatus.charAt(i) == 'f'){
						this.pastStatus[i] = PAST_STATUS_FAIL;
					} else if (rawStatus.charAt(i) == 'e') {
						this.pastStatus[i] = PAST_STATUS_ERROR;
					} else {
						this.pastStatus[i] = PAST_STATUS_UNDETECTED;
					}
				}
			}
		} else {
			Log.e(TAG, "Invalid file type");
			throw new Exception();
		}

		fileName = getFileName();
		
	}
*/
	public static String[] toStringArray(JSONArray array) {
		if (array == null)
			return null;

		String[] arr = new String[array.length()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = array.optString(i);
		}
		return arr;
        }


	public static String loadJSON(String fileName) {
		String jsonData = "";
		BufferedReader br = null;
		try {  
			String line;
			br = new BufferedReader(new FileReader(fileName));
			while ((line = br.readLine()) != null) {
				jsonData += line + "\n";
			}
		} catch (IOException e) {
			Log.e(TAG, "Failed to open file " + fileName);
			return null;
		} finally {
			try {  
				if (br != null)
					br.close();
			} catch (IOException ex) {
				Log.e(TAG, "Failed to close bufferedReader");
				return null;
			}
		}

		return jsonData;
	}

	public void setJSON() {
		String sep = System.getProperty("line.separator");

		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		year = year + YEARSTILEXPIRE;

		String expDate = month + "-" + year;

		json = "{" + sep;
		json +=   "\t\"cloudcoin\": [{" + sep;
		json += "\t\t\"nn\":\"1\"," + sep;
		json +="\t\t\"sn\":\""+ sn + "\"," + sep;
		json += "\t\t\"an\": [\"";

		for (int i = 0; i < raidaCnt; i++) {
			json += ans[i];
			if (i != raidaCnt - 1) {
				json += "\",\"";
			}
		}
		
		if (!aoid.startsWith("\""))
			aoid = "\"" + aoid + "\"";

		json += "\"]," + sep;
		json += "\t\t\"ed\":\"" + expDate + "\"," + sep;
		json += "\t\t\"aoid\": [" + aoid + "]" + sep;
		json += "\t}] "+ sep;
		json += "}";

	}

	private byte[] readAsset(Context context, String fileName) throws Exception {
		byte[] bytes;
		try {  
			AssetFileDescriptor fd = context.getAssets().openFd(fileName);
			long size = fd.getLength();
			bytes = new byte[(int) size];

			InputStream fis = context.getAssets().open(fileName);

			BufferedInputStream buf = new BufferedInputStream(fis);
			buf.read(bytes, 0, bytes.length);
			buf.close();

			fis.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found: " + fileName);
			throw new Exception();
		} catch (IOException e) {
			Log.e(TAG, "Error while reading file: " + fileName);
			throw new Exception();
		} 

		return bytes;
	}

	public void setJpeg(String rootFolder, Context context) throws Exception {
		byte[] returnBytes =  null;
		String cloudCoinStr = "";

		for (int i = 0; i < raidaCnt; i++) {
			cloudCoinStr += this.ans[i];
		}

		cloudCoinStr += JPEGAOID; 
		cloudCoinStr += "00";//LHC = 100%
		cloudCoinStr += "97E2";//0x97E2;//Expiration date Sep. 2018
		cloudCoinStr += "01";// cc.nn;//network number
		String hexSN = Integer.toHexString(this.sn);
		String fullHexSN = "";

		switch (hexSN.length()) {  
			case 1: fullHexSN = "00000" +hexSN; break;
			case 2: fullHexSN = "0000" +hexSN; break;
			case 3: fullHexSN = "000" +hexSN; break;
			case 4: fullHexSN = "00" +hexSN; break;
			case 5: fullHexSN = "0" +hexSN; break;
			case 6: fullHexSN = hexSN; break;
		}

		cloudCoinStr += fullHexSN;
		String Path = "";

		String d = "" + getDenomination();

		returnBytes = readAsset(context, "jpegs/jpeg" + d + ".jpg");
		byte[] ccArray = hexStringToByteArray(cloudCoinStr);

		int offset = 20;  
		for (int j =0; j < ccArray.length; j++) {
			returnBytes[offset + j] = ccArray[j];
		}

		this.jpeg = returnBytes;
	}
		
	public String writeJpeg(String path) throws Exception {
		Log.v(TAG, "writejpeg " + path);

		String fileName = path + "/" + this.fileName + "jpg";

		try {
			File file = new File(fileName);
			if (file.exists()) {
				Log.e(TAG, "File " + fileName + " already exists");
				throw new Exception();
			}

			FileOutputStream fos = new FileOutputStream(fileName);
			fos.write(this.jpeg);
			fos.close();
		} catch (IOException e) {
			Log.e(TAG, "Error while writing file: " + fileName);
			e.printStackTrace();
			throw new Exception();
		}

		return fileName;
	}

	public void saveCoin(String path, String extension) throws Exception {

		String newFileName = path + "/" + this.fileName + extension;

		setJSON();

		File f = new File(newFileName);
		if (f.exists()) {
			Log.e(TAG, "File " + newFileName + " already exists");
			throw new Exception("File " + newFileName + " already exists");
		}

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(newFileName));
			writer.write(this.json);
		} catch (IOException e){ 
			Log.e(TAG, "Failed to save file " + newFileName);
			throw new Exception("Failed to save file: " + e.getMessage());
		} finally {    
			try{
				if (writer != null)
					writer.close();
			} catch (IOException e){
				Log.e(TAG, "Failed to close BufferedWriter");
				throw new Exception("Failed to close BufferedWriter");
			}
		}
	}

	private String toHexadecimal(byte[] digest) {
		String hash = "";
		for(byte aux : digest) {
			int b = aux & 0xff;
			if (Integer.toHexString(b).length() == 1) hash += "0";
				hash += Integer.toHexString(b);
		}

		return hash;
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


	public void setAnsToPans(){
		for (int i =0; i < raidaCnt; i++) {
			pans[i] = ans[i];
		}
	}


	public void setAnsToPansIfPassed() {
		for (int i = 0; i < raidaCnt; i++) {
			if (pastStatus[i] == PAST_STATUS_PASS) {
				ans[i] = pans[i];
			}
		}
	}

	public void calculateHP(){
		hp = raidaCnt;

		for (int i = 0; i < raidaCnt; i++) {
			if( this.pastStatus[i] == PAST_STATUS_FAIL) 
				hp--;
		}
	}

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

	public byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}


}
