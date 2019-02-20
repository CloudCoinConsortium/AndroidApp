package global.cloudcoin.ccbank;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;

import android.content.res.Resources;

import android.app.Activity;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;
import android.content.Context;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.content.Intent;
import android.view.Window;
import android.os.Handler;
import android.graphics.Color;

import android.util.Log;
import android.view.WindowManager;
import android.view.Display;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread;
import java.lang.Runnable;

import android.widget.Toast;

import java.io.FileOutputStream;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import android.app.Dialog;

import android.net.Uri;

import android.content.pm.PackageManager.NameNotFoundException;

import android.view.ViewGroup.LayoutParams;


import android.widget.LinearLayout;

import android.graphics.drawable.ColorDrawable;

import android.widget.NumberPicker;
import android.widget.RadioGroup;

import android.widget.EditText;
import java.lang.reflect.Field;
import android.graphics.Paint;

import java.util.ArrayList;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.os.AsyncTask;
import android.graphics.Point;
import android.os.Build;

import android.content.pm.ActivityInfo;
import android.view.Surface;

import android.widget.ProgressBar;
import android.os.Looper;
import android.os.Message;


import java.util.Date;
import java.util.Calendar;

import global.cloudcoin.ccbank.Echoer.Echoer;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.Servant;
import global.cloudcoin.ccbank.core.ServantRegistry;

public class MainActivity extends Activity implements NumberPicker.OnValueChangeListener, OnClickListener {

	TextView tv;
	Button bt;

	String ltag = "PocketBank";

	boolean asyncFinished;

	LinearLayout ll1, ll2, ll3;

	SharedPreferences mSettings;
	static public String version;

	Bank bank;
	ArrayList<String> files;

        static int IDX_BANK = 0;
        static int IDX_COUNTERFEIT = 1;
        static int IDX_FRACTURED = 2;

	final static int REQUEST_CODE_IMPORT_DIR = 1;
	final static int COINS_CNT = 1;

	boolean isImportDialog;

	TextView subTv;

        TextView[][] ids;
        int[][] stats;
        int size;
	int lastProgress;

	NumberPicker[] nps;
	TextView[] tvs;

	Button button, emailButton;

	EditText et;
	TextView tvTotal, exportTv;

	Dialog dialog;
	ImportTask iTask;
	FixFrackedTask ffTask;

	int importState;

	static int IMPORT_STATE_INIT = 1;
	static int IMPORT_STATE_IMPORT = 2;
	static int IMPORT_STATE_DONE = 3;

	ProgressBar pb;
	int raidaStatus = 0;
	int coinActive = 0;
	int coinTotal = 0;

	Handler mHandler;
	boolean isFixing = false;

	boolean isImportSuspect = false;

	public static final String APP_PREFERENCES_IMPORTDIR = "pref_importdir";


	AppCore appCore;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		asyncFinished = false;
		files = null;

		init();

		setImportState(IMPORT_STATE_INIT);
		bank = new Bank(this);

		mHandler = new Handler(Looper.getMainLooper()) {
                        public void handleMessage(Message inputMessage) {
                                int what = inputMessage.what;

                                if (what == 0) {
                                        raidaStatus++;
                                        setDots();
                                } else if (what == COINS_CNT) {
                                        raidaStatus = 0;
                                        coinActive = inputMessage.arg1 + 1;
                                        coinTotal = inputMessage.arg2;
                                        setDots();
                                }

                        }
                };

		Thread myThread = new Thread(new Runnable() {
			public void run() {
				RAIDA.updateRAIDAList(MainActivity.this);

				MainActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						asyncFinished = true;		
					}
				});
			}
		});

		myThread.start();
		parseViewIntent();

		isImportDialog = false;
	}


	private void initSystem() {


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
			//appCore.initFolders();

			ServantRegistry sr = new ServantRegistry();

			sr.registerServants(new String[]{
					"Echoer"
			}, AppCore.getRootPath(), alogger);

			//Echoer e = (Echoer) sr.getServant("Echoer");
			//e.echo();


		} catch (Exception e) {
			Log.e(ltag, "Failed to init folders");
		}
	}

	Handler getHandler() {
		return mHandler;
	}


	private void setDots() {
		String s;

		if (coinTotal == 0) {
			s = "\n";
		} else {
			s = getResources().getString(R.string.coin) + " " + coinActive + "/" + coinTotal + "\n";
		}

		pb.setProgress(raidaStatus);

		subTv.setText(s);
        }

	public void init() {
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
	
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

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

		updateImportString();
		doFixFracked();
	}

	public void updateImportString() {
		String importDir;
		String savedImportDir = mSettings.getString(APP_PREFERENCES_IMPORTDIR, "");

		if (savedImportDir == "") {
			importDir = bank.getDefaultRelativeImportDirPath();
			if (importDir == null) 
				return;
		} else {
			importDir = savedImportDir;
			bank.setImportDirPath(importDir);
		}

		if (!bank.examineImportDir()) 
			return;

		TextView ltv = (TextView) findViewById(R.id.icoins);

		int totalIncomeLength = bank.getLoadedIncomeLength();
                if (totalIncomeLength == 0) 
			ltv.setVisibility(View.GONE);
		else 
			ltv.setVisibility(View.VISIBLE);
	}

	public void onDestroy() {
		super.onDestroy();
	}


	private void allocId(int idx, String prefix) {
		int resId, i;
                String idTxt;

		stats[idx] = new int[size];
		ids[idx] = new TextView[size];
		for (i = 0; i < size; i++) {
			if (i == size - 1)
				idTxt = prefix + "all";
			else
				idTxt = prefix + Bank.denominations[i];

			resId = getResources().getIdentifier(idTxt, "id", getPackageName());
			ids[idx][i] = (TextView) dialog.findViewById(resId);
                }
        }


	private void initDialog(int layout) {
		if (isImportDialog)
			return;
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(layout);
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		LinearLayout closeButton = (LinearLayout) dialog.findViewById(R.id.closebutton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

	}

	private void showError(String msg) {
                Toast toast = Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG);
                toast.show();
        }

	private int getTotal() {
		int total = 0;

		for (int i = 0; i < size; i++) {
			int denomination =  Bank.denominations[i];
			total += denomination * nps[i].getValue();
		}

		return total;
	}

	public void updateTotal() {
                String totalStr;

		if (exportTv == null)
			return;

		Resources res = getResources();

                int total = getTotal();

		StringBuilder sb = new StringBuilder();
		sb.append(res.getString(R.string.export));
		sb.append(" " + total);

		exportTv.setText(sb.toString());
        }

        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		updateTotal();
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

	private void InputStreamToFile(InputStream in, String file) {
		try {
			OutputStream out = new FileOutputStream(new File(file));

			int size = 0;
			byte[] buffer = new byte[1024];

			while ((size = in.read(buffer)) != -1) {
				out.write(buffer, 0, size);
			}

			out.close();
		}
		catch (Exception e) {
			Log.e("MainActivity", "InputStreamToFile exception: " + e.getMessage());
		}
	}
	private String getContentName(ContentResolver resolver, Uri uri){
		Cursor cursor = resolver.query(uri, null, null, null, null);
		cursor.moveToFirst();
		int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
		if (nameIndex >= 0) {
			return cursor.getString(nameIndex);
		} else {
			return null;
		}
	}
	public void parseViewIntent() {
		Intent intent = getIntent();
		String action = intent.getAction();

		String savedImportDir = mSettings.getString(APP_PREFERENCES_IMPORTDIR, "");
		String importDir = "" ;
		if (savedImportDir == "") {
			importDir = bank.getDefaultRelativeImportDirPath();
			if (importDir == null) {
				Toast.makeText(this,R.string.errmnt,Toast.LENGTH_SHORT).show();

				return;
			}
		} else {
			importDir = savedImportDir;
			bank.setImportDirPath(importDir);
		}

		if (!bank.examineImportDir()) {
			Toast.makeText(this,R.string.errimport,Toast.LENGTH_SHORT).show();
			return;
		}

		if (action.compareTo(Intent.ACTION_VIEW) == 0) {
			String scheme = intent.getScheme();
			ContentResolver resolver = getContentResolver();
			if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
				Uri uri = intent.getData();

				String name = getContentName(resolver, uri);

				Log.v("tag" , "Content intent detected: " + action + " : " + intent.getDataString() + " : " + intent.getType() + " : " + name);
				try {
					InputStream input = resolver.openInputStream(uri);
					String importfilepath =  bank.getImportDirPath()+ "/"+ name;
					InputStreamToFile(input, importfilepath);
				}
				catch (Exception e) {
					Toast.makeText(this, "Error occured while opening the file", Toast.LENGTH_SHORT).show();
				}
				showImportScreen();
				isImportDialog = false;

			}
			else if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
				Uri uri = intent.getData();
				String name = uri.getLastPathSegment();

				Log.v("tag" , "File intent detected: " + action + " : " + intent.getDataString() + " : " + intent.getType() + " : " + name);

				try {
					InputStream input = resolver.openInputStream(uri);
					String importfilepath =  bank.getImportDirPath()+ "/"+ name;
					InputStreamToFile(input, importfilepath);
				}
				catch (Exception e) {
					Toast.makeText(this, "Error occured while opening the file", Toast.LENGTH_SHORT).show();
				}
				showImportScreen();
				isImportDialog = false;

			}
			else if (scheme.compareTo("http") == 0) {
				// TODO Import from HTTP!
			}
			else if (scheme.compareTo("ftp") == 0) {
				// TODO Import from FTP!
			}
		}
	}

	public void doSendEmail() {
                ArrayList<String> filenames;

                filenames = bank.getExportedFilenames();

                //EmailSender email = new EmailSender(this, "", "Send CloudCoins");
		//email.openDialogWithAttachments(filenames);
        }

	public void doExport() {
		String exportTag;
		int[] values;
                int[] failed;
                int totalFailed = 0;

		Resources res = getResources();

		if (getTotal() == 0) {
			showError(res.getString(R.string.nocoins));
			return;
		}

                et = (EditText) dialog.findViewById(R.id.exporttag);
		exportTag = et.getText().toString();

		RadioGroup rg = (RadioGroup) dialog.findViewById(R.id.radioGroup);
		int selectedId = rg.getCheckedRadioButtonId();

		values = new int[size];
		for (int i = 0; i < size; i++)
			values[i] = nps[i].getValue();

		if (isFixing) {
			showError(res.getString(R.string.fixing));
			return;
		}

		if (selectedId == R.id.rjpg) {
                        failed = bank.exportJpeg(values, exportTag);
                } else if (selectedId == R.id.rjson) {
                        failed = bank.exportJson(values, exportTag);
		} else {
			Log.v("CC", "We will never be here");
			return;
		}

                String msg;

		if (failed[0] == -1) {
                        msg = res.getString(R.string.globalexporterror);
                } else {
			for (int i = 0; i < size; i++) {
				totalFailed += failed[i];
			}
			if (totalFailed == 0) {
				msg = String.format(res.getString(R.string.exportok), bank.getRelativeExportDirPath());
                        } else {
				msg = String.format(res.getString(R.string.exportfailed), totalFailed);
                        }
                }

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

	public void selectFile() {
                Intent i = new Intent((Context) this, DirPickerActivity.class);
                startActivityForResult(i, REQUEST_CODE_IMPORT_DIR);
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
		sb.append(": " + bank.getImportStats(Bank.STAT_VALUE_MOVED_TO_BANK) + "\n");
		sb.append("\n");

		sb.append(res.getString(R.string.serialnumber));
		sb.append("   |   ");
		sb.append(res.getString(R.string.importresult));
		sb.append("\n");
		sb.append("------------------------------------------------------\n");

		ArrayList<String[]> report = bank.getReport();
		for (String[] item : report) {
			sb.append(String.format("%1$-15s", item[0]));
			sb.append(" ");
			sb.append(String.format("%1$-15s", item[1]));
			sb.append(" ");
			sb.append(item[2]);
			sb.append("CC\n");
		}

	//	EmailSender email = new EmailSender(this, "", "Import Receipt");
	//	email.setBody(sb.toString());
	//	email.openDialog();
	}


	public void showImportScreen() {
		int totalIncomeLength;
		String result;
                String importDir = "";

		if (!isImportDialog)
			dialog = new Dialog(this);


		if (!isOnline()) {
			initDialog(R.layout.importdialog2);
			dialog.show();
			return;
		}

		if (importState == IMPORT_STATE_IMPORT) {
			initDialog(R.layout.importdialog4);
			tv = (TextView) dialog.findViewById(R.id.infotext);
			tv.setText(getStatusString(lastProgress));

			subTv = (TextView) dialog.findViewById(R.id.infotextsub);

			pb = (ProgressBar) dialog.findViewById(R.id.firstBar);
			pb.setMax(RAIDA.TOTAL_RAIDA_COUNT);
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

			int toBankValue, toBank, failed;
	
			toBankValue = bank.getImportStats(Bank.STAT_VALUE_MOVED_TO_BANK);
			toBank = bank.getImportStats(Bank.STAT_AUTHENTIC);
			failed = bank.getImportStats(Bank.STAT_FAILED);

			TextView ttv;


			ttv = (TextView) dialog.findViewById(R.id.closebuttontext);
			if (failed > 0 || toBank == 0)
				ttv.setText(R.string.back);
			else
				ttv.setText(R.string.awesome);
			

			ttv = (TextView) dialog.findViewById(R.id.imptotal);
			ttv.setText("" + toBankValue);

			ttv = (TextView) dialog.findViewById(R.id.auth);
			ttv.setText("" + toBank);

			ttv = (TextView) dialog.findViewById(R.id.failed);
			ttv.setText("" + failed);

			try {
				dialog.show();
			} catch (Exception e) {
				Log.v("CLOUDCOIN", "Activity is gone. No result will be shown");
			}
			return;
		}

		if (bank.getSuspectSize() > 0) {
			initDialog(R.layout.importdialog6);
			LinearLayout goButton = (LinearLayout) dialog.findViewById(R.id.gobutton);
			goButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					doImportSuspect();
				}
			});
			dialog.show();
			return;
		}


		initDialog(R.layout.importdialog);
		LinearLayout fileButton = (LinearLayout) dialog.findViewById(R.id.filebutton);
		fileButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				selectFile();
			}
		});

		tv = (TextView) dialog.findViewById(R.id.infotext);



		if (files != null && files.size() > 0) {
			bank.loadIncomeFromFiles(files);
		} else {
			String savedImportDir = mSettings.getString(APP_PREFERENCES_IMPORTDIR, "");

			if (savedImportDir == "") {
				importDir = bank.getDefaultRelativeImportDirPath();
				if (importDir == null) {
					tv.setText(R.string.errmnt);
					dialog.show();
					return;
				}
			} else {
				importDir = savedImportDir;
				bank.setImportDirPath(importDir);
			}

			if (!bank.examineImportDir()) {
				tv.setText(R.string.errimport);
				dialog.show();
				return;
			}
		}

		totalIncomeLength = bank.getLoadedIncomeLength();
                if (totalIncomeLength == 0) {
			result = String.format(getResources().getString(R.string.erremptyimport), importDir);
			tv.setText(result);		
			dialog.show();
                        return;
                }

		if (files != null && files.size() > 0) {
                        result = String.format(getResources().getString(R.string.importfiles), totalIncomeLength);
                } else {
                        result = String.format(getResources().getString(R.string.importwarn), importDir, totalIncomeLength);            
		}

		dialog.setContentView(R.layout.importdialog3);
		LinearLayout closeButton = (LinearLayout) dialog.findViewById(R.id.closebutton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		LinearLayout importButton = (LinearLayout) dialog.findViewById(R.id.importbutton);
		importButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doImport();
			}
		});

		tv = (TextView) dialog.findViewById(R.id.infotext);
                tv.setText(result);
		

		
		dialog.show();
	}

	public void showExportScreen() {
		isImportDialog = false;
		dialog = new Dialog(this);

		int i, resId;
                String idTxt;
		int bankCoins[], frackedCoins[];
		int lTotal;

		initDialog(R.layout.exportdialog);

		size = Bank.denominations.length;
		nps = new NumberPicker[size];
                tvs = new TextView[size];
                for (i = 0; i < size; i++) {
                        idTxt = "np" + Bank.denominations[i];
                        resId = getResources().getIdentifier(idTxt, "id", getPackageName());
                        nps[i] = (NumberPicker) dialog.findViewById(resId);
			setNumberPickerTextColor(nps[i], Color.parseColor("#348EFB"));

                        idTxt = "bs" + Bank.denominations[i];
                        resId = getResources().getIdentifier(idTxt, "id", getPackageName());
                        tvs[i] = (TextView) dialog.findViewById(resId);
                }


		tvTotal = (TextView) dialog.findViewById(R.id.exptotal);
		exportTv = (TextView) dialog.findViewById(R.id.exporttv);

		bankCoins = bank.countCoins("bank");
		frackedCoins = bank.countCoins("fracked");

		int overall = 0;
		for (i = 0; i < size; i++) {
			lTotal = bankCoins[i + 1] + frackedCoins[i + 1];

			nps[i].setMinValue(0);
			nps[i].setMaxValue(lTotal);
			nps[i].setValue(0);
			nps[i].setOnValueChangedListener(this);
			nps[i].setTag(Bank.denominations[i]);
			nps[i].setWrapSelectorWheel(false);
		
			tvs[i].setText("" + lTotal);

			overall += Bank.denominations[i] * lTotal;
		}

		updateTotal();
		tvTotal.setText("" + overall);

		LinearLayout exportButton = (LinearLayout) dialog.findViewById(R.id.exportbutton);
		exportButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doExport();
			}
		});

		String msg = String.format(getResources().getString(R.string.exportnotice), bank.getRelativeExportDirPath());

		TextView eNotice = (TextView) dialog.findViewById(R.id.en);
		eNotice.setText(msg);

		dialog.show();
	}

	public void showBankScreen() {
		isImportDialog = false;
		dialog = new Dialog(this);

		initDialog(R.layout.bankdialog);

		size = Bank.denominations.length + 1;
		ids = new TextView[3][];
		stats = new int[3][];

		allocId(IDX_BANK, "bs");
		allocId(IDX_COUNTERFEIT, "cs");
		allocId(IDX_FRACTURED, "fs");

                stats[IDX_BANK] = bank.countCoins("bank");
                stats[IDX_FRACTURED] = bank.countCoins("fracked");

                int j;
		int totalCnt = 0;
		int tval;

                for (int i = 0; i < size; i++) {
                        if (i == 0) {
                                j = size - 1;
				tval = 0;
			} else {
                                j = i - 1;
				tval = Bank.denominations[i - 1];
			}

			int authCount = stats[IDX_BANK][i] + stats[IDX_FRACTURED][i];

			totalCnt += tval * authCount;

			ids[IDX_BANK][j].setText("" + authCount);
                }

		TextView tcv = (TextView) dialog.findViewById(R.id.totalcoinstxt);
		String msg = getResources().getString(R.string.acc);
		msg += " " + Integer.toString(totalCnt);
		tcv.setText(msg);

		dialog.show();
	}

	public void onClick(View v) {
		final int id;
		Intent intent;
		int state;

		id = v.getId();


		switch (id) {
			case R.id.limport:
				if (!asyncFinished) 
					return;

				showImportScreen();
				break;
			case R.id.lexport:
				showExportScreen();
				break;
			case R.id.lbank:
				showBankScreen();
				break;
			default:
				break;
				
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_IMPORT_DIR) {
			if(resultCode == RESULT_OK) {
        	                this.files = data.getStringArrayListExtra(DirPickerActivity.returnParameter);
	                } else {
	//			showError("Internal error");
	                }

			dialog.dismiss();
			isImportDialog = false;
			showImportScreen();
			return;
		}

		bank.moveExportedToSent();
		dialog.dismiss();
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


	class FixFrackedTask extends AsyncTask<String, Integer, String> {
		int before, after;

                protected String doInBackground(String... params) {
                        for (int i = 0; i < bank.getFrackedCoinsLength(); i++) {
				publishProgress(i);
				bank.fixFracked(i);
			}

			return "OK";
		}

		protected void onPreExecute() {
			isFixing = true;
                        bank.loadFracked();

			before = bank.getFrackedCoinsLength();

			if (before == 0)
				return;

			String msg = String.format(getResources().getString(R.string.fixstart), before);
			showError(msg);
		}

		protected void onPostExecute(String result) {
			int fixedCnt;

			isFixing = false;

			if (before == 0)
				return;

			after = bank.getFrackedCoinsLength();
			fixedCnt = before - after;

			// It is possible that some coins will be added during Import process
			if (fixedCnt < 0)
				fixedCnt = 0;

			String msg = String.format(getResources().getString(R.string.fixed), fixedCnt, before);
			showError(msg);
		}

		protected void onProgressUpdate(Integer... values) {
		//	showError("Fixed " + values[0] + " of " + bank.getFrackedCoinsLength());
		}
	}

	class ImportTask extends AsyncTask<String, Integer, String> {
                protected String doInBackground(String... params) {
			bank.initReport();
			bank.resetImportStats();

			if (params[0] == "import") {
				for (int i = 0; i < bank.getLoadedIncomeLength(); i++) {
					if (isCancelled())
						return "CANCELLED";
	
					publishProgress(i);
					bank.importLoadedItem(i);
				}
			} else if (params[0] == "suspect") {
				try {
					bank.detectAuthenticity(null);
				} catch (Exception e) {
					Log.v("CLOUDCOIN", "Failed to detect suspect coins");
				}

			}

                        return "OK";
                }

		protected void onPostExecute(String result) {
			setImportState(IMPORT_STATE_DONE);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			dialog.dismiss();
			isImportDialog = false;
			showImportScreen();
		}

		protected void onPreExecute() {
			lockOrientation();
                        setImportState(IMPORT_STATE_IMPORT);
			dialog.dismiss();
			isImportDialog = false;
			lastProgress = 0;
			showImportScreen();

		}

		protected void onProgressUpdate(Integer... values) {
			tv.setText(getStatusString(values[0]));

			lastProgress = values[0];

			raidaStatus = 0;
			coinActive = 0;
			coinTotal = 0;
			setDots();
                }

	}

	private void doFixFracked() {
		if (isFixing)
			return;

		ffTask = new FixFrackedTask();
		ffTask.execute();
	}

	private void doImportSuspect() {
		isImportSuspect = true;
		iTask = new ImportTask();
		iTask.execute("suspect");
	}

	private void doImport() {
		isImportSuspect = false;
		iTask = new ImportTask();
		iTask.execute("import");
	}

	public void setImportState(int newState) {
		//SharedPreferences.Editor ed = mSettings.edit();
		//ed.putInt("state", newState);
		//ed.commit();

                importState = newState;
        }

	private String getStatusString(int progressCoins) {
                String statusString;

		if (isImportSuspect)
			return "";

		int totalIncomeLength = bank.getLoadedIncomeLength();
		int importedIncomeLength = progressCoins + 1;

		statusString = String.format(getResources().getString(R.string.authstring), importedIncomeLength, totalIncomeLength);

		return statusString;
        }

}
