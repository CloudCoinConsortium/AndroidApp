package global.cloudcoin.ccbank;

import android.util.Log;

import java.io.File;
import android.content.Context;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;


import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

import android.os.Environment;

import java.util.Random;

import android.os.Handler;
import android.os.Message;

import global.cloudcoin.ccbank.core.Servant;

import javax.microedition.khronos.opengles.GL;

import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.GLoggerInterface;
import global.cloudcoin.ccbank.core.RAIDA;

import global.cloudcoin.ccbank.ALogger;
import global.cloudcoin.ccbank.core.ServantRegistry;

import global.cloudcoin.ccbank.Echoer.Echoer;

public class Bank {

	static String RAIDA_AUTH_URL = "https://www.cloudcoin.co/servers.html";
	static String DIR_BASE = "CloudCoins";
	static String IMPORT_DIR_NAME = "Import";
	static String EXPORT_DIR_NAME = "Export";
	static String IMPORTED_DIR_NAME = "Imported";
	static String TRASH_DIR_NAME = "Trash";
	static String SENT_DIR_NAME = "Sent";
	static String BANK_DIR_NAME = "Bank";
	static String TAG = "CLOUDCOIN";
	static int CONNECTION_TIMEOUT = 5000; // ms

	static int STAT_FAILED = 0;
	static int STAT_AUTHENTIC = 1;
	static int STAT_COUNTERFEIT = 2;
	static int STAT_FRACTURED = 3;
	
	static int STAT_VALUE_MOVED_TO_BANK = 4;

	private String importDirPath;
	private String exportDirPath;
	private String importedDirPath;
	private String trashDirPath;
	private String bankDirPath;
	private String sentDirPath;

	private Context ctx;

	private ArrayList<IncomeFile> loadedIncome; 
	private CloudCoin[] frackedCoins;

	static int[] denominations = {1, 5, 25, 100, 250};

	RAIDA raida;
	boolean isCancelled;

	private int[] importStats;

	ArrayList<String[]> report;

	private ArrayList<String> exportedFilenames; 

	public Bank(Context ctx) {

		this.importDirPath = null;
		this.loadedIncome = new ArrayList<IncomeFile>();


		ALogger alogger = new ALogger();



		this.raida = new RAIDA(null);

		this.raida.logger = alogger;





		this.resetImportStats();
		this.createDirectories();
		this.ctx = ctx;
		this.isCancelled = false;

		this.exportedFilenames = new ArrayList<String>();
	}

	public void resetImportStats() {
		importStats = new int[6];

		for (int i = 0; i < importStats.length; i++)
			importStats[i] = 0;
	}

	public void setImportDirPath(String importDirPath) {
		this.importDirPath = importDirPath;
		this.importedDirPath = importDirPath + "/" + IMPORTED_DIR_NAME;

		try {  
			File idPathFile = new File(this.importedDirPath);
                        idPathFile.mkdirs();
                } catch (Exception e) {
                        Log.e(TAG, "Can not create Import/Imported directory");
                }

	}

	public String getImportDirPath() {
		return this.importDirPath;
	}

	public ArrayList<String[]> getReport() {
		return this.report;
	}

	public String getRelativeExportDirPath() {
		return DIR_BASE + "/" + EXPORT_DIR_NAME;

	}
	public String getDefaultRelativeImportDirPath() {
		return DIR_BASE + "/" + IMPORT_DIR_NAME;
	}

	public String getBankDirPath() {
		return this.bankDirPath;
	}

	public ArrayList<String> getExportedFilenames() {
		return this.exportedFilenames;
	}



	private void createDirectories() {



	}


	public String getFileExtension(String f) {
		String ext = "";
		int i = f.lastIndexOf('.');

		if (i > 0 &&  i < f.length() - 1) {
			ext = f.substring(i + 1);
		}

		return ext;
	}


	public ArrayList<IncomeFile> selectAllFileNamesFolder(String path, String extension) {
		int fileType;
		ArrayList<IncomeFile> fileArray = new ArrayList<IncomeFile>();

		try {
			File f = new File(path);
			File[] files = f.listFiles();
			for (File inFile : files) {
				if (inFile.isFile()) {
					String currentExtension = getFileExtension(inFile.getName()).toLowerCase();
					
					if (currentExtension.equals(extension)) {
						if (extension.equals("jpeg") || extension.equals("jpg")) {
							fileType = IncomeFile.TYPE_JPEG;
						} else {
							fileType = IncomeFile.TYPE_STACK;
						}

						fileArray.add(new IncomeFile(inFile.getAbsolutePath(), fileType));
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to read directory: " + path);
			e.printStackTrace();
			return null;
		}

		return fileArray;
	}
	
	public void loadIncomeFromFiles(ArrayList<String> files) {
		String extension;
		int fileType;

		loadedIncome = new ArrayList<IncomeFile>();

		for (String file : files) {
			File inFile = new File(file);
			try {  
				if (inFile.isFile()) {
					extension = getFileExtension(inFile.getName()).toLowerCase();

					if (extension.equals("jpeg") || extension.equals("jpg")) {
                                                fileType = IncomeFile.TYPE_JPEG;
                                        } else {
                                                fileType = IncomeFile.TYPE_STACK;
                                        }

                                        loadedIncome.add(new IncomeFile(inFile.getAbsolutePath(), fileType));
				}
			} catch (Exception e) {
	                        Log.e(TAG, "Failed to read file " + file);
			}
		}
	}

	public boolean examineImportDir() {
		String extension;
		int fileType;

		if (this.importDirPath == null)
			return false;

		loadedIncome = new ArrayList<IncomeFile>();

		try {
			File f = new File(this.importDirPath);
			File[] files = f.listFiles();
			for (File inFile : files) {
				if (inFile.isFile()) {
					extension = getFileExtension(inFile.getName()).toLowerCase();
					
					if (extension.equals("jpeg") || extension.equals("jpg")) {
						fileType = IncomeFile.TYPE_JPEG;
					} else {
						fileType = IncomeFile.TYPE_STACK;
					}

					loadedIncome.add(new IncomeFile(inFile.getAbsolutePath(), fileType));
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to read Import directory");
			return false;
		}

		return true;
	}

	private void importError(IncomeFile iFile, String error) {
		String fileName = "?";

		importStats[STAT_FAILED]++;

		if (iFile != null)
			fileName = iFile.fileName;

		Log.e(TAG, "Error importing file: " + fileName + ": " + error);

		if (iFile != null)
			moveFileToTrash(fileName, error);

		addCoinToReport(null, "failed");
	}

	public void addCoinToReport(CloudCoin cc, String status) {
		String serial;
		String denom;

		serial = (cc == null) ? "?" : Integer.toString(cc.sn);
		denom = (cc == null) ? "?" : Integer.toString(cc.getDenomination());

		String[] s = new String[3];

		s[0] = serial;
		s[1] = status;
		s[2] = denom;

		this.report.add(s);
	} 

	public void initReport() {
		this.report = new ArrayList<String[]>();
	}

	public void importLoadedItem(int idx) {
		IncomeFile incomeFile;
		CloudCoin cc;
		String incomeJson;

		if (idx >= loadedIncome.size()) {
			importError(null, "Internal error");
			return;
		}	

		incomeFile = loadedIncome.get(idx);
		incomeFile.fileTag = "";
		try {
			if (incomeFile.fileType == IncomeFile.TYPE_JPEG) {
			//	cc = new CloudCoin(incomeFile);
			//	cc.saveCoin(bankDirPath, "suspect");
			} else if (incomeFile.fileType == IncomeFile.TYPE_STACK) {
				incomeJson = CloudCoin.loadJSON(incomeFile.fileName);
				if (incomeJson == null) {
					importError(incomeFile, "Failed to load JSON file");
					return;
				}
			
				try {
					JSONObject o = new JSONObject(incomeJson);
					JSONArray incomeJsonArray = o.getJSONArray("cloudcoin");

					for (int i = 0; i < incomeJsonArray.length(); i++) {
						JSONObject childJSONObject = incomeJsonArray.getJSONObject(i);
						int nn     = childJSONObject.getInt("nn");
						int sn     = childJSONObject.getInt("sn");
						JSONArray an = childJSONObject.getJSONArray("an");
						String ed     = childJSONObject.getString("ed");
						String aoid = childJSONObject.getString("aoid");

						aoid = aoid.replace("[", "");
						aoid = aoid.replace("]", "");
					
						cc = new CloudCoin(nn, sn, CloudCoin.toStringArray(an), ed, aoid, "");
						cc.saveCoin(bankDirPath, "suspect");
					}
				} catch (JSONException e) {
					importError(incomeFile, "Failed to parse JSON file. It is corrupted: " + e.getMessage());
					e.printStackTrace();
					return;
				}
			}

			detectAuthenticity(incomeFile.fileName);
		} catch (Exception e) {
			importError(incomeFile, "Coin is not imported. " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	public void cancel() {
		this.isCancelled = true;
	}

	public void loadFracked() {
		this.frackedCoins = loadCoinArray("fracked");
	}

/*
	public void fixFracked(int idx) {
		CloudCoin cc;

		if (idx >= getFrackedCoinsLength()) {
			Log.e(TAG, "Internal error while fixing fracked: " + idx + " length: " + getFrackedCoinsLength());
			return;
		}

		cc = frackedCoins[idx];
	
		// File was not loaded as it was corrupted
		if (cc == null) 
			return;

		Log.v(TAG, "Fixing Fracked coin: " +  cc.fullFileName);
		raida.fixCoin(cc);

		try {
			if (cc.extension.equals("bank")) {
				cc.saveCoin(bankDirPath, cc.extension);
				deleteCoin(cc.fullFileName);
			} else if (cc.extension.equals("fracked")) {
				deleteCoin(cc.fullFileName);
				cc.saveCoin(bankDirPath, cc.extension);
			} else if (cc.extension.equals("counterfeit")) {
				moveFileToTrash(cc.fullFileName, "The coin is counterfeit (after fracked). Passed: " + cc.gradeStatus[0] + "; Failed: " + cc.gradeStatus[1] + "; Other: " + cc.gradeStatus[2]);
			} else {
			//	moveFileToTrash(cc.fullFileName, "The coin is failed (after fracked). Passed: " + cc.gradeStatus[0] + "; Failed: " + cc.gradeStatus[1] + "; Other: " + cc.gradeStatus[2]);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to save coin: " + cc.fullFileName);
		}
	}
	*/

	public void moveFileToTrash(String fileName, String error) {
		File fsource, ftarget;
		String target, etarget;
		BufferedWriter writer = null;

		try {
			fsource = new File(fileName);
			target = trashDirPath + "/" + System.currentTimeMillis() + "-" + fsource.getName();
			etarget = target + ".txt";

			ftarget = new File(target);
			fsource.renameTo(ftarget);

                        writer = new BufferedWriter(new FileWriter(etarget));
                        writer.write(error + "\n");
 			writer.close();

		} catch (IOException e) {
			Log.e(TAG, "Failed to move to Trash " + fileName + ": " + e.getMessage());
			e.printStackTrace();
			return;
		} catch (Exception e) {
			// Non critical, dont throw and anything and interrupt the process.
			Log.e(TAG, "Failed to move to Trash " + fileName);
			e.printStackTrace();
			return;
		}

		return;
	}

	public void moveExportedToSent() {
		File fsource, ftarget;
		String target;

		ArrayList<String> filenames = getExportedFilenames();

		for (String fileName : filenames) {
			try {
				fsource = new File(fileName);
			
				target = sentDirPath + "/" + fsource.getName();
				ftarget = new File(target);
				fsource.renameTo(ftarget);
			} catch (Exception e) {
				// Non critical, dont throw and anything and interrupt the process.
				Log.e(TAG, "Failed to move to Sent " + fileName);
				e.printStackTrace();
			}
		}
	}

	public void moveFileToImported(String fileName) {
		File fsource, ftarget;
		String target;
	
		try {
			fsource = new File(fileName);
			target = importedDirPath + "/" + System.currentTimeMillis() + "-" + fsource.getName() + ".imported";

			ftarget = new File(target);
			fsource.renameTo(ftarget);
		} catch (Exception e) {
			// Non critical, dont throw and anything and interrupt the process.
			Log.e(TAG, "Failed to move to Imported " + fileName);
			e.printStackTrace();
			return;
		}

	}

	public void deleteCoin(String path) throws Exception {
		boolean deleted = false;

		File f  = new File(path);
		try {
			f.delete();
		} catch (Exception e) {
			Log.e(TAG, "Failed to delete coin " + path);
			e.printStackTrace();
			throw new Exception("Failed to delete coin " + path + " " + e.getMessage());
		}
	}

	public int[] exportJson(int[] values, String tag) {
		int[] failed;
		CloudCoin cc;
		ArrayList<IncomeFile> bankFiles = selectAllFileNamesFolder(bankDirPath, "bank");
		ArrayList<IncomeFile> frackedFiles = selectAllFileNamesFolder(bankDirPath, "fracked");

		bankFiles.addAll(frackedFiles);

		failed = new int[values.length];

		int denomination;
		int totalSaved = 0;
		int coinCount = 0;
		for (int i = 0; i < values.length; i++) {
			failed[i] = 0;
			totalSaved += denominations[i] * values[i];
			coinCount += values[i];
		}

		String tJ, json = "{ \"cloudcoin\": [";
		ArrayList<String> coinsToDelete = new ArrayList<String>();
		int c = 0;

		for (int i =0; i < bankFiles.size(); i++ ) {
			IncomeFile fileToExport = bankFiles.get(i);
			denomination = getDenomination(fileToExport);

			for (int j = 0; j < values.length; j++) {
				if (denomination == denominations[j] && values[j] > 0) {
					if (c != 0)
						json += ",\n";

					try {
						fileToExport.fileTag = tag;
						tJ = CloudCoin.loadJSON(fileToExport.fileName);
						if (tJ == null) {
							Log.e(TAG, "Failed to export coin: " + bankFiles.get(i).fileName);
							failed[j]++;
						} else {
							JSONObject o = new JSONObject(tJ);
							JSONArray jArray = o.getJSONArray("cloudcoin");
							json += jArray.getJSONObject(0).toString();
						}
						
						coinsToDelete.add(fileToExport.fileName);
						c++;
					} catch (JSONException e) {
						Log.e(TAG, "Invalid json " + bankFiles.get(i).fileName);
						failed[j]++;
					} catch (Exception e) {
						Log.e(TAG, "Failed to export coin: " + bankFiles.get(i).fileName);
						failed[j]++;
					}

					values[j]--;
					break;
				}
			}
		}

		json += "]}";

		try {  
			JSONObject o = new JSONObject(json);
		} catch (JSONException e) {
			Log.e(TAG, "Invalid JSON was created");
			failed[0] = -1;
			return failed;
		}

		if (tag.isEmpty()) {
			Random rnd = new Random();
			tag = "" + rnd.nextInt(999);
		}
	
		String fileName = exportDirPath + "/" + totalSaved + ".CloudCoins." + tag + ".stack";
		BufferedWriter writer = null;
		try {   
			if (ifFileExists(fileName)) { 
				// no overwriting
				Log.v(TAG, "Filename " + fileName + " already exists");
				failed[0] = -1;
				return failed;
			}

                        writer = new BufferedWriter(new FileWriter(fileName));
                        writer.write(json);
                } catch (IOException e){
                        Log.e(TAG, "Failed to save file " + fileName);
			failed[0] = -1;
			return failed;
                } finally {
                        try{
                                if (writer != null)
                                        writer.close();
                        } catch (IOException e){
                                Log.e(TAG, "Failed to close BufferedWriter");
				failed[0] = -1;
				return failed;
                        }
                }
		
		exportedFilenames.add(fileName);

		for (String ctd : coinsToDelete) {
			try {
				deleteCoin(ctd);
			} catch (Exception e) {
				Log.e(TAG, "Failed to delete coin: " + ctd + " " + e.getMessage());
			}
		}

		return failed;
	}

	public boolean ifFileExists(String filePathString ) {
		File f = new File(filePathString);
		if (f.exists() && !f.isDirectory()) 
			return true;

		return false;
	}

	public int[] exportJpeg(int[] values, String tag) {
		int[] failed;
		CloudCoin cc;
		ArrayList<IncomeFile> bankFiles = selectAllFileNamesFolder(bankDirPath, "bank");
		ArrayList<IncomeFile> frackedFiles = selectAllFileNamesFolder(bankDirPath, "fracked");

		bankFiles.addAll(frackedFiles);

		failed = new int[values.length];

		int denomination;
		int totalSaved = 0;
		int coinCount = 0;
		for (int i = 0; i < values.length; i++) {
			failed[i] = 0;
			totalSaved += denominations[i] * values[i];
			coinCount += values[i];
		}

		for (int i =0; i < bankFiles.size(); i++ ) {
			IncomeFile fileToExport = bankFiles.get(i);
			denomination = getDenomination(fileToExport);

			for (int j = 0; j < values.length; j++) {
				if (denomination == denominations[j] && values[j] > 0) {
					try {
				//		fileToExport.fileTag = tag;
				//		cc = new CloudCoin(fileToExport);
				//		cc.setJpeg(bankDirPath, ctx);

				//		String fileName = cc.writeJpeg(exportDirPath);
				//		exportedFilenames.add(fileName);

				//		deleteCoin(fileToExport.fileName);
						// delete
					} catch (Exception e) {
						Log.e(TAG, "Failed to export coin: " + bankFiles.get(i).fileName);
						failed[j]++;
					}

					values[j]--;
					break;
				}
			}
		}

		return failed;
	}

	public int getSuspectSize() {
		ArrayList<IncomeFile> incomeFiles = selectAllFileNamesFolder(bankDirPath, "suspect");

		return incomeFiles.size();
	}

	public void detectAuthenticity(String importedfileName) throws Exception {
		CloudCoin cc;
		ArrayList<IncomeFile> incomeFiles = selectAllFileNamesFolder(bankDirPath, "suspect");

		if (incomeFiles == null) {
			throw new Exception("Failed to read directory " + bankDirPath);
		}

		Handler h = ((MainActivity) this.ctx).getHandler();

		int iFSize = incomeFiles.size();
		for (int i = 0; i < iFSize; i++) {
			if (this.isCancelled)
				return;

			try {
				Message msg = h.obtainMessage(MainActivity.COINS_CNT, i, iFSize);
				h.sendMessage(msg);

				//CHANGED WILL NOT WORK (remove filename)
				cc = new CloudCoin(incomeFiles.get(i).fileName);
			//	raida.detectCoin(cc);

				Log.v(TAG, "Coin #" + cc.sn + " got extension " + cc.extension);

				if (cc.extension.equals("bank")) {
					cc.saveCoin(bankDirPath, cc.extension);

					importStats[STAT_AUTHENTIC]++;
					importStats[STAT_VALUE_MOVED_TO_BANK] += cc.getDenomination();
					if (importedfileName != null)
						moveFileToImported(importedfileName);
					addCoinToReport(cc, "authentic");
					deleteCoin(incomeFiles.get(i).fileName);
				} else if (cc.extension.equals("fracked")) {
					cc.saveCoin(bankDirPath, cc.extension);

					//importStats[STAT_FRACTURED]++;
					importStats[STAT_AUTHENTIC]++;
					importStats[STAT_VALUE_MOVED_TO_BANK] += cc.getDenomination();
					if (importedfileName != null)
						moveFileToImported(importedfileName);
					addCoinToReport(cc, "fracked");
					deleteCoin(incomeFiles.get(i).fileName);
				} else if (cc.extension.equals("counterfeit")) {
					//importStats[STAT_COUNTERFEIT]++;
					importStats[STAT_FAILED]++;
					if (importedfileName != null)
						moveFileToTrash(importedfileName, "The coin is counterfeit. Passed: " + cc.gradeStatus[0] + "; Failed: " + cc.gradeStatus[1] + "; Other: " + cc.gradeStatus[2]);
					addCoinToReport(cc, "counterfeit");
					deleteCoin(incomeFiles.get(i).fileName);
				} else {
					importStats[STAT_FAILED]++;
					addCoinToReport(cc, "failed");
				}

			} catch (Exception e) {
				Log.e(TAG, "Failed to detect coin: " + e.getMessage());
				e.printStackTrace();
				throw new Exception("Failed to Detect Coin: " + e.getMessage());
			}
		}

	}


	public boolean renameFileExtension(String source, String newExtension){
		String target;
		String currentExtension = getFileExtension(source);

		if (currentExtension.equals("")){
			target = source + "." + newExtension;
		} else {
			target = source.replaceFirst(Pattern.quote("." + currentExtension) + "$", Matcher.quoteReplacement("." + newExtension));
		}

		return new File(source).renameTo(new File(target));
	}

	public int getImportStats(int type) {
		if (type >= importStats.length)
			return 0;

		return importStats[type];
	}
	
	public int getFrackedCoinsLength() {
		if (this.frackedCoins == null)
			return 0;

		return this.frackedCoins.length;
	}

	public int getLoadedIncomeLength() {
		return this.loadedIncome.size();
	}

	private int getDenomination(IncomeFile incomeFile) {
		int denomination;
		File f = new File(incomeFile.fileName);

		String[] nameParts = f.getName().split("\\.");
		try {
			denomination = Integer.parseInt(nameParts[0]);
		} catch (Exception e) {
			denomination = -1;
		}

		return denomination;			
	}

	public int[] countCoins(String extension) {
		int denomination;
		int totalCount =  0;
		int[] returnCounts = new int[6]; //0. Total, 1.1s, 2,5s, 3.25s 4.100s, 5.250s
		
		ArrayList<IncomeFile> incomeFiles = selectAllFileNamesFolder(bankDirPath, extension);
		if (incomeFiles == null) {
			return returnCounts;
		}

		for (int i = 0; i < incomeFiles.size(); i++) {
			denomination = getDenomination(incomeFiles.get(i));
			switch (denomination) {
				case 1: returnCounts[0] += 1; returnCounts[1]++; break;
				case 5: returnCounts[0] += 5; returnCounts[2]++; break;
				case 25: returnCounts[0] += 25; returnCounts[3]++; break;
				case 100: returnCounts[0] += 100; returnCounts[4]++; break;
				case 250: returnCounts[0] += 250; returnCounts[5]++; break;
			}
		}

		return returnCounts;
	}



	public CloudCoin[] loadCoinArray(String extension) {
		CloudCoin[] loadedCoins = null;
		
		try {
			ArrayList<IncomeFile> incomeFiles = selectAllFileNamesFolder(bankDirPath, extension);

			loadedCoins = new CloudCoin[incomeFiles.size()];
			for (int i = 0; i < incomeFiles.size(); i++) {
				try {
					// WILL NOT WORK
					loadedCoins[i] = new CloudCoin(incomeFiles.get(i).fileName);
				} catch (Exception e) {
					Log.e(TAG, "Can not parse coin " + incomeFiles.get(i));
					continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to load coins for " + extension);
			return null;
		}

		return loadedCoins;
	}
}
