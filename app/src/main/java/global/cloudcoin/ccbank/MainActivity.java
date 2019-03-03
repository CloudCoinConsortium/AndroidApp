package global.cloudcoin.ccbank;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;

import android.content.res.Resources;

import android.app.Activity;
import android.os.Bundle;

import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.TextView;
import android.content.Context;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.content.Intent;
import android.view.Window;
import android.graphics.Color;

import android.util.Log;
import android.view.WindowManager;
import android.view.Display;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread;
import java.lang.Runnable;
import android.widget.Toast;
import android.graphics.Paint;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.app.Dialog;


import android.content.pm.PackageManager.NameNotFoundException;
import android.view.ViewGroup.LayoutParams;


import android.widget.LinearLayout;
import android.graphics.drawable.ColorDrawable;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.EditText;


import java.lang.reflect.Field;
import java.util.ArrayList;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.graphics.Point;
import android.os.Build;

import android.content.pm.ActivityInfo;
import android.view.Surface;
import android.widget.ProgressBar;


import java.util.Date;
import java.util.Calendar;


import global.cloudcoin.ccbank.Authenticator.Authenticator;
import global.cloudcoin.ccbank.Authenticator.AuthenticatorResult;
import global.cloudcoin.ccbank.Echoer.Echoer;
import global.cloudcoin.ccbank.Exporter.Exporter;
import global.cloudcoin.ccbank.Exporter.ExporterResult;
import global.cloudcoin.ccbank.FrackFixer.FrackFixer;
import global.cloudcoin.ccbank.FrackFixer.FrackFixerResult;
import global.cloudcoin.ccbank.ShowCoins.ShowCoins;
import global.cloudcoin.ccbank.ShowCoins.ShowCoinsResult;
import global.cloudcoin.ccbank.Unpacker.Unpacker;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.ServantRegistry;
import global.cloudcoin.ccbank.Grader.Grader;
import global.cloudcoin.ccbank.Grader.GraderResult;



public class MainActivity extends Activity implements NumberPicker.OnValueChangeListener, OnClickListener {
	String ltag = "PocketBank";

	final static int ECHO_RESULT_INITIAL = 0;
	final static int ECHO_RESULT_OK = 1;
	final static int ECHO_RESULT_FAILED = 2;
	final static int ECHO_RESULT_DOING = 3;

	int echoResult;

	ServantRegistry sr;

	TextView tv;
	Button bt;
	LinearLayout ll1, ll2, ll3;

	ArrayList<String> exportedFilenames;

	SharedPreferences mSettings;
	static public String version;

	TextView subTv;

    TextView[][] ids;
	int lastProgress;

	NumberPicker[] nps;
	TextView[] tvs;

	Button button, emailButton;

	EditText et;
	TextView tvTotal, exportTv;

	Dialog dialog;

	int importState;

	static int IMPORT_STATE_INIT = 1;
	static int IMPORT_STATE_UNPACKING = 2;
	static int IMPORT_STATE_IMPORT = 3;
	static int IMPORT_STATE_DONE = 4;

	ProgressBar pb;

	public static final String APP_PREFERENCES_IMPORTDIR = "pref_importdir";
	final static int MY_STORAGE_WRITE_CONSTANT = 1;


	private int statToBankValue, statToBank, statFailed;

	static int DIALOG_NONE = 0;
	static int DIALOG_IMPORT = 1;
	static int DIALOG_BANK = 2;
	static int DIALOG_EXPORT = 3;

	int requestedDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		echoResult = ECHO_RESULT_INITIAL;
		setImportState(IMPORT_STATE_INIT);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		initSystem();

		Log.v("xxx", "ONCREATE FINISHED");
	}


	private void initSystem() {
		AppCore.initPool();

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(this,
					new String[]{
							Manifest.permission.WRITE_EXTERNAL_STORAGE,
							Manifest.permission.READ_EXTERNAL_STORAGE
					}, MY_STORAGE_WRITE_CONSTANT);
		} else {
			Log.v(ltag, "Granted");

			doInitSystem();
		}

		requestedDialog = DIALOG_NONE;
		initUI();
	}

	public void doInitSystem() {
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			Log.e(ltag, "Primary storage is not mounted");
			return;
		}

		File path = Environment.getExternalStorageDirectory();
		if (path == null) {
			Log.e(ltag, "Failed to get External directory");
			return;
		}

		ALogger alogger = new ALogger();
		try {
			AppCore.initFolders(path, alogger);

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

			startEchoService();
		} catch (Exception e) {
			Log.e(ltag, "Failed to init folders");
		}


		int d;
		String templateDir, fileName;

		templateDir = AppCore.getUserDir(Config.DIR_TEMPLATES);
		fileName = templateDir + File.separator + "jpeg1.jpg";
		File f = new File(fileName);
		if (!f.exists()) {
			Log.v(ltag, "Copying templates");
			for (int i = 0; i < AppCore.getDenominations().length; i++) {
				d = AppCore.getDenominations()[i];
				try {
					fileName = "jpeg" + d + ".jpg";
					copyAssetFile("jpegs" + File.separator + fileName,
							AppCore.getUserDir(Config.DIR_TEMPLATES) +
									File.separator + fileName);
				} catch (IOException e) {
					Log.e(ltag, "Failed to copy file: " + e.getMessage());
				}
			}
		}
	}

	public void startEchoService() {
		echoResult = ECHO_RESULT_DOING;

		Echoer e = (Echoer) sr.getServant("Echoer");
		e.launch(new EchoCb());
	}

	public void startShowCoinsService() {
		ShowCoins sc = (ShowCoins) sr.getServant("ShowCoins");
		sc.launch(new ShowCoinsCb());
	}

	public void startUnpackerService() {
		Unpacker up = (Unpacker) sr.getServant("Unpacker");
		up.launch(new UnpackerCb());
	}

	public void startAuthenticatorService() {
		Authenticator at = (Authenticator) sr.getServant("Authenticator");
		at.launch(new AuthenticatorCb());
	}

	public void startGraderService() {
		Grader gd = (Grader) sr.getServant("Grader");
		gd.launch(new GraderCb());
	}

	public void startFrackFixerService() {
		if (sr.isRunning("FrackFixer")) {
			Log.v(ltag, "Fracker is already running. Nothing to do");
			return;
		}

		FrackFixer ff = (FrackFixer) sr.getServant("FrackFixer");
		ff.launch(new FrackFixererCb());
	}

	public void startExporterService() {
		Exporter e = (Exporter) sr.getServant("Exporter");
		e.launch(new ExporterCb());
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_STORAGE_WRITE_CONSTANT: {
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					doInitSystem();
				} else {
					showError("Permission was denied");
				}

				return;
			}
		}
	}

	private void setRAIDAProgress(int raidaProcessed, int totalFilesProcessed, int totalFiles) {
		tv.setText(getStatusString(totalFilesProcessed, totalFiles));
		pb.setProgress(raidaProcessed);
	}

	public void initUI() {
		try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			version = "";
		}

		ll1 = (LinearLayout) findViewById(R.id.limport);
		ll1.setOnClickListener(this);

		ll2 = (LinearLayout) findViewById(R.id.lbank);
		ll2.setOnClickListener(this);

		ll3 = (LinearLayout) findViewById(R.id.lexport);
		ll3.setOnClickListener(this);

		((TextView) findViewById(R.id.tversion)).setText(version);
	}


	public void onBackPressed() {
		final Context mContext = this;

		super.onBackPressed();
	}

	public void onStart() {
		super.onStart();
	}

	public void onStop() {
		super.onStop();
	}


	public void onPause() {
		super.onPause();

	//	if (iTask != null)
	//		iTask.cancel(true);
	}

	public void onResume() {
		super.onResume();
	}

	public void onDestroy() {
		super.onDestroy();
	}

	private void allocId(int idx, int size, String prefix) {
		int resId, i;
		String idTxt;

		ids[idx] = new TextView[size];
		for (i = 0; i < size; i++) {
			idTxt = prefix + AppCore.getDenominations()[i];

			resId = getResources().getIdentifier(idTxt, "id", getPackageName());
			ids[idx][i] = (TextView) dialog.findViewById(resId);
		}
	}


	private void initDialog(int layout) {
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(layout);
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		LinearLayout closeButton = (LinearLayout) dialog.findViewById(R.id.closebutton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setImportState(IMPORT_STATE_INIT);
				dialog.dismiss();
			}
		});

	}

	private void showError(String msg) {
		Toast toast = Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG);
		toast.show();
	}

	private void showMessage(String msg) {
		showError(msg);
	}

	private void showShortMessage(String msg) {
		Toast toast = Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	private int getExportTotal() {
		int total = 0;

		for (int i = 0; i < AppCore.getDenominations().length; i++) {
			int denomination =  AppCore.getDenominations()[i];;
			total += denomination * nps[i].getValue();
		}

		return total;
	}

	public void updateExportTotal() {
		String totalStr;

		if (exportTv == null)
			return;

		Resources res = getResources();

		int total = getExportTotal();

		StringBuilder sb = new StringBuilder();
		sb.append(res.getString(R.string.export));
		sb.append(" " + total);

		exportTv.setText(sb.toString());
	}

	public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		updateExportTotal();
	}


	public void doSendEmail() {
		StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
		StrictMode.setVmPolicy(builder.build());

		EmailSender email = new EmailSender(this, "", "Send CloudCoins");
		email.openDialogWithAttachments(exportedFilenames);
	}


	public void doExport() {
		String exportTag;
		int[] values;

		Resources res = getResources();
		if (getExportTotal() == 0) {
			showError(res.getString(R.string.nocoins));
			return;
		}

		et = (EditText) dialog.findViewById(R.id.exporttag);
		exportTag = et.getText().toString();

		RadioGroup rg = (RadioGroup) dialog.findViewById(R.id.radioGroup);
		int selectedId = rg.getCheckedRadioButtonId();
		int size = AppCore.getDenominations().length;

		values = new int[size];
		for (int i = 0; i < size; i++)
			values[i] = nps[i].getValue();

		if (sr.isRunning("FrackFixer")) {
			showError(res.getString(R.string.fixing));
			return;
		}

		int type;
		if (selectedId == R.id.rjpg) {
			type = Config.TYPE_JPEG;
		} else if (selectedId == R.id.rjson) {
			type = Config.TYPE_STACK;
		} else
			return;

		Exporter ex = (Exporter) sr.getServant("Exporter");
		ex.launch(Config.DIR_DEFAULT_USER, type, values, exportTag, new ExporterCb());
	}

	public void showExportResult() {
		String msg = "Exported successfully";

		dialog.setContentView(R.layout.exportdialog2);

		TextView infoText = (TextView) dialog.findViewById(R.id.infotext);
		infoText.setText(msg);

		LinearLayout emailButton = (LinearLayout) dialog.findViewById(R.id.emailbutton);
		emailButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doSendEmail();
			}
		});

		LinearLayout closeButton = (LinearLayout) dialog.findViewById(R.id.closebutton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

	}

	public void doEmailReceipt() {
		StringBuilder sb = new StringBuilder();
		Resources res = getResources();

		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		int day = cal.get(Calendar.DAY_OF_MONTH);
		int month = cal.get(Calendar.MONTH) + 1;
		int year = cal.get(Calendar.YEAR);

		String dayStr = Integer.toString(day);
		String monthStr = Integer.toString(month);

		if (day < 10)
			dayStr = "0" + dayStr;
		if (day < 10)
			monthStr = "0" + monthStr;

		String dateStr = monthStr + "/" + dayStr + "/" + year;

		sb.append(res.getString(R.string.paymentreceived));
		sb.append(" " + dateStr + "\n");
		sb.append(res.getString(R.string.totalreceived));
	//sb.append(": " + bank.getImportStats(Bank.STAT_VALUE_MOVED_TO_BANK) + "\n");
		sb.append("\n");

		sb.append(res.getString(R.string.serialnumber));
		sb.append("   |   ");
		sb.append(res.getString(R.string.importresult));
		sb.append("\n");
		sb.append("------------------------------------------------------\n");

		/*
		ArrayList<String[]> report = bank.getReport();
		for (String[] item : report) {
			sb.append(String.format("%1$-15s", item[0]));
			sb.append(" ");
			sb.append(String.format("%1$-15s", item[1]));
			sb.append(" ");
			sb.append(item[2]);
			sb.append("CC\n");
		}

		*/


	//	EmailSender email = new EmailSender(this, "", "Import Receipt");
	//	email.setBody(sb.toString());
	//	email.openDialog();
	}

	public void showImportScreen() {
		String result;

		dialog = new Dialog(this);
		if (!isOnline()) {
			initDialog(R.layout.importdialog2);
			dialog.show();
			return;
		}

		if (importState == IMPORT_STATE_UNPACKING) {
			initDialog(R.layout.importdialog);
			tv = (TextView) dialog.findViewById(R.id.infotext);
			tv.setText(getString(R.string.unpacking));

			dialog.show();
			return;
		}

		if (importState == IMPORT_STATE_IMPORT) {
			initDialog(R.layout.importdialog4);
			tv = (TextView) dialog.findViewById(R.id.infotext);
			subTv = (TextView) dialog.findViewById(R.id.infotextsub);

			pb = (ProgressBar) dialog.findViewById(R.id.firstBar);
			pb.setMax(RAIDA.TOTAL_RAIDA_COUNT);

			setRAIDAProgress(0, 0, AppCore.getFilesCount(Config.DIR_SUSPECT));
			dialog.show();
			return;
		}

		if (importState == IMPORT_STATE_DONE) {
			setImportState(IMPORT_STATE_INIT);
			initDialog(R.layout.importdialog5);
			LinearLayout emailButton = (LinearLayout) dialog.findViewById(R.id.emailbutton);
			emailButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.dismiss();
					doEmailReceipt();
				}
			});

			TextView ttv;

			ttv = (TextView) dialog.findViewById(R.id.closebuttontext);
			if (statFailed > 0 || statToBank == 0)
				ttv.setText(R.string.back);
			else
				ttv.setText(R.string.awesome);


			ttv = (TextView) dialog.findViewById(R.id.imptotal);
			ttv.setText("" + statToBankValue);

			ttv = (TextView) dialog.findViewById(R.id.auth);
			ttv.setText("" + statToBank);

			ttv = (TextView) dialog.findViewById(R.id.failed);
			ttv.setText("" + statFailed);

			try {
				dialog.show();
			} catch (Exception e) {
				Log.v(ltag, "Activity is gone. No result will be shown");
			}
			return;
		}

		initDialog(R.layout.importdialog);

		tv = (TextView) dialog.findViewById(R.id.infotext);

		String importDir = AppCore.getUserDir(Config.DIR_IMPORT);
		int totalFiles = AppCore.getFilesCount(Config.DIR_IMPORT);

		if (totalFiles == 0) {
			result = String.format(getResources().getString(R.string.erremptyimport), importDir);
			tv.setText(result);
			dialog.show();
			return;
		}

		result = String.format(getResources().getString(R.string.importwarn), importDir, totalFiles);

		dialog.setContentView(R.layout.importdialog3);
		LinearLayout closeButton = (LinearLayout) dialog.findViewById(R.id.closebutton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setImportState(IMPORT_STATE_INIT);
				dialog.dismiss();
			}
		});

		LinearLayout importButton = (LinearLayout) dialog.findViewById(R.id.importbutton);
		importButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setImportState(IMPORT_STATE_UNPACKING);
				dialog.dismiss();
				showImportScreen();
				startUnpackerService();
			}
		});

		tv = (TextView) dialog.findViewById(R.id.infotext);
		tv.setText(result);

		dialog.show();
	}

	public void showExportScreen(int[][] counters) {
		int size, i, resId, lTotal;
		String idTxt;

        if (dialog != null)
            dialog.dismiss();

		dialog = new Dialog(this);

		if (counters.length == 0)
			return;

		size = AppCore.getDenominations().length;

		initDialog(R.layout.exportdialog);

		nps = new NumberPicker[size];
		tvs = new TextView[size];
		for (i = 0; i < size; i++) {
			idTxt = "np" + AppCore.getDenominations()[i];
			resId = getResources().getIdentifier(idTxt, "id", getPackageName());
			nps[i] = (NumberPicker) dialog.findViewById(resId);

			setNumberPickerTextColor(nps[i], Color.parseColor("#348EFB"));

			idTxt = "bs" + AppCore.getDenominations()[i];
			resId = getResources().getIdentifier(idTxt, "id", getPackageName());
			tvs[i] = (TextView) dialog.findViewById(resId);
		}

		tvTotal = (TextView) dialog.findViewById(R.id.exptotal);
		exportTv = (TextView) dialog.findViewById(R.id.exporttv);

		int overall = 0;
		for (i = 0; i < size; i++) {
			lTotal = counters[Config.IDX_FOLDER_BANK][i] +
					counters[Config.IDX_FOLDER_FRACKED][i];

			nps[i].setMinValue(0);
			nps[i].setMaxValue(lTotal);
			nps[i].setValue(0);
			nps[i].setOnValueChangedListener(this);
			nps[i].setTag(AppCore.getDenominations()[i]);
			nps[i].setWrapSelectorWheel(false);

			tvs[i].setText("" + lTotal);

			overall += AppCore.getDenominations()[i] * lTotal;
		}

		updateExportTotal();
		tvTotal.setText("" + overall);

		LinearLayout exportButton = (LinearLayout) dialog.findViewById(R.id.exportbutton);
		exportButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doExport();
			}
		});

		String msg = String.format(getResources().getString(R.string.exportnotice),
				AppCore.getUserDir(Config.DIR_EXPORT));

		TextView eNotice = (TextView) dialog.findViewById(R.id.en);
		eNotice.setText(msg);

		dialog.show();
	}

	public void showBankScreen(int[][] counters) {
		int size;

        if (dialog != null)
            dialog.dismiss();

		dialog = new Dialog(this);

		if (counters.length == 0)
			return;

		initDialog(R.layout.bankdialog);

		size = counters[0].length;
		ids = new TextView[3][];

		allocId(Config.IDX_FOLDER_BANK, size,"bs");
		allocId(Config.IDX_FOLDER_FRACKED, size, "fs");

		int totalCnt = 0;
		for (int i = 0; i < size; i++) {
			int authCount = counters[Config.IDX_FOLDER_BANK][i] +
					counters[Config.IDX_FOLDER_FRACKED][i];

			ids[Config.IDX_FOLDER_BANK][i].setText("" + authCount);
		}

		totalCnt = AppCore.getTotal(counters[Config.IDX_FOLDER_BANK]) +
				AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]);

		TextView tcv = (TextView) dialog.findViewById(R.id.totalcoinstxt);
		String msg = getResources().getString(R.string.acc);
		msg += " " + Integer.toString(totalCnt);
		tcv.setText(msg);

		dialog.show();
	}

	public void setNumberPickerTextColor(NumberPicker numberPicker, int color) {
		final int count = numberPicker.getChildCount();

		for (int i = 0; i < count; i++) {
			View child = numberPicker.getChildAt(i);
			if (child instanceof EditText) {
				try {
					Field selectorWheelPaintField = numberPicker.getClass()
							.getDeclaredField("mSelectorWheelPaint");
					selectorWheelPaintField.setAccessible(true);

					Field selectorDivider = numberPicker.getClass()
							.getDeclaredField("mSelectionDivider");
					selectorDivider.setAccessible(true);

					ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#ECECEC"));
					selectorDivider.set(numberPicker, colorDrawable);

					((Paint) selectorWheelPaintField.get(numberPicker)).setColor(color);
					((EditText) child).setTextColor(color);
					numberPicker.invalidate();

					return;
				}
				catch (NoSuchFieldException e) {}
				catch (IllegalAccessException e) {}
				catch(IllegalArgumentException e) {}
			}
		}

	}

	public void onClick(View v) {
		final int id;
		Intent intent;
		int state;

		id = v.getId();

		switch (id) {
			case R.id.limport:

				if (echoResult == ECHO_RESULT_INITIAL)
					return;

				else if (echoResult == ECHO_RESULT_DOING) {
					showError("RAIDA is being checked. Please wait");
					return;
				} if (echoResult == ECHO_RESULT_FAILED) {
					showError("RAIDA failed. Starting a new check. Please wait");

					startEchoService();
					return;
				}

				requestedDialog= DIALOG_IMPORT;
				showImportScreen();
				break;
			case R.id.lexport:
                requestedDialog= DIALOG_EXPORT;
				//showExportScreen();
				showShortMessage("Loading");
                startShowCoinsService();
				break;
			case R.id.lbank:
                requestedDialog= DIALOG_BANK;
                //showBankScreen();
				showShortMessage("Loading");
				startShowCoinsService();
				break;
			default:
				break;

		}
	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();

		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

	private void lockOrientation() {
		Display display = getWindowManager().getDefaultDisplay();
		int rotation = display.getRotation();
		int height, width;

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
			height = display.getHeight();
			width = display.getWidth();
		} else {
			Point size = new Point();
			display.getSize(size);
			height = size.y;
			width = size.x;
		}

		switch (rotation) {
			case Surface.ROTATION_90:
				if (width > height)
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				else
					setRequestedOrientation(9/* reversePortait */);
				break;
			case Surface.ROTATION_180:
				if (height > width)
					setRequestedOrientation(9/* reversePortait */);
				else
					setRequestedOrientation(8/* reverseLandscape */);
				break;
			case Surface.ROTATION_270:
				if (width > height)
					setRequestedOrientation(8/* reverseLandscape */);
				else
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
			default:
				if (height > width)
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				else
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

    }

	public void setImportState(int newState) {
		//SharedPreferences.Editor ed = mSettings.edit();
		//ed.putInt("state", newState);
		//ed.commit();

		importState = newState;
	}

	private String getStatusString(int progressCoins, int totalFiles) {
		String statusString;

		statusString = String.format(getResources().getString(R.string.authstring), progressCoins, totalFiles);

		return statusString;
	}

	public void copyAssetFile(String assetFilePath, String destinationFilePath) throws IOException {
		InputStream in = getApplicationContext().getAssets().open(assetFilePath);
		OutputStream out = new FileOutputStream(destinationFilePath);

		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
			out.write(buf, 0, len);

		in.close();
		out.close();
	}

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

    class ShowCoinsCb implements CallbackInterface {
		public void callback(final Object result) {
			final Object fresult = result;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ShowCoinsResult scresult = (ShowCoinsResult) fresult;

					if (requestedDialog == DIALOG_BANK)
					    showBankScreen(scresult.counters);
					else if (requestedDialog == DIALOG_EXPORT)
					    showExportScreen(scresult.counters);
				}
			});
		}
	}

	class UnpackerCb implements CallbackInterface {
		public void callback(Object result) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {

			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					Log.v("xxx", "unpacker done");
					setImportState(IMPORT_STATE_IMPORT);
					dialog.dismiss();
					lastProgress = 0;

					startAuthenticatorService();

					showImportScreen();
				}
			});
		}
	}

	class AuthenticatorCb implements CallbackInterface {
		public void callback(Object result) {
			final Object fresult = result;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AuthenticatorResult ar = (AuthenticatorResult) fresult;

					if (ar.status == AuthenticatorResult.STATUS_ERROR) {
						setImportState(IMPORT_STATE_INIT);
						dialog.dismiss();
						showError("Internal error or RAIDA is unavailable");

						//startGraderService();


						return;
					}

					if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
						//setImportState(IMPORT_STATE_DONE);
						//dialog.dismiss();
						//showImportScreen();
						startGraderService();
						return;
					}

					setRAIDAProgress(ar.totalRAIDAProcessed, ar.totalFilesProcessed, ar.totalFiles);

					Log.v("xxx", "authenticator done: " + ar.totalRAIDAProcessed + "/25 coin: " +
							" f=" + ar.totalFilesProcessed);
				}
			});
		}
	}

	class GraderCb implements CallbackInterface {
		public void callback(Object result) {
			final Object fresult = result;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					GraderResult gr = (GraderResult) fresult;

					statToBankValue = gr.totalAuthenticValue + gr.totalFrackedValue;
					statToBank = gr.totalAuthentic + gr.totalFracked;
					statFailed = gr.totalLost + gr.totalCounterfeit + gr.totalUnchecked;

					setImportState(IMPORT_STATE_DONE);
					dialog.dismiss();
					showImportScreen();
				}
			});
		}
	}

	class FrackFixererCb implements CallbackInterface {
		public void callback(Object result) {
			final Object fresult = result;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FrackFixerResult fr = (FrackFixerResult) fresult;

					if (fr.status == FrackFixerResult.STATUS_ERROR) {
						showError("Failed to fix coins");
						return;
					}

					if (fr.status == FrackFixerResult.STATUS_FINISHED) {
						if (fr.fixed + fr.failed > 0) {
							showMessage("Fracker fixed: " + fr.fixed + ", failed: " + fr.failed);
							return;
						}
					}
				}
			});
		}
	}

	class ExporterCb implements CallbackInterface {
		public void callback(Object result) {
			final Object fresult = result;

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ExporterResult er = (ExporterResult) fresult;

					Log.v("XXX", "EXPORTER RETURNED");

					if (er.status == ExporterResult.STATUS_ERROR) {
						dialog.dismiss();
						showError("Failed to fix coins");
						return;
					}

					if (er.status == ExporterResult.STATUS_FINISHED) {
						exportedFilenames = er.exportedFileNames;
						showExportResult();
						return;
					}
				}
			});
		}
	}
}
