package global.cloudcoin.ccbank;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.*;
import android.widget.*;

public class DirPickerActivity extends Activity {
	// Intent parameters names constants
	public static final String returnParameter = "directoryPathRet";

	// Stores names of traversed directories
	ArrayList<String> pathDirsList = new ArrayList<String>();

	ArrayList<String> chosenFiles = new ArrayList<String>();

	static String TAG = "CLOUDCOIN";

	private ArrayList<Item> fileList = new ArrayList<Item>();
	private File path = null;
	private String chosenFile;

	ArrayAdapter<Item> adapter;

	private boolean showHiddenFilesAndDirs = true;

	private boolean directoryShownIsEmpty = false;

	private String filterFileExtension = null;

	private static final int MAX_FILES = 100;

	// Action constants
	private static final int SELECT_DIRECTORY = 1;
	private static final int SELECT_FILE = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// In case of
		// ua.com.vassiliev.androidfilebrowser.SELECT_DIRECTORY_ACTION
		// Expects com.mburman.fileexplore.directoryPath parameter to
		// point to the start folder.
		// If empty or null, will start from SDcard root.
		setContentView(R.layout.dirpicker);

		// Set action for this activity
		Intent thisInt = this.getIntent();
		
		setInitialDirectory();
		parseDirectoryPath();
		loadFileList();
		this.createFileListAdapter();
		this.initializeButtons();
		this.initializeFileListView();
		updateCurrentDirectoryTextView();
	}

	private void setInitialDirectory() {
		if (Environment.getExternalStorageDirectory().isDirectory()
				&& Environment.getExternalStorageDirectory().canRead())
			path = Environment.getExternalStorageDirectory();
		else
			path = new File("/");
	}

	private void parseDirectoryPath() {
		pathDirsList.clear();
		String pathString = path.getAbsolutePath();
		String[] parts = pathString.split("/");
		int i = 0;
		while (i < parts.length) {
			pathDirsList.add(parts[i]);
			i++;
		}
	}

	private void initializeButtons() {
		Button upDirButton = (Button) this.findViewById(R.id.upDirectoryButton);
		upDirButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				loadDirectoryUp();
				loadFileList();
				adapter.notifyDataSetChanged();
				updateCurrentDirectoryTextView();
			}
		});

		Button selectFolderButton = (Button) this.findViewById(R.id.selectCurrentDirectoryButton);
		selectFolderButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				returnDirectoryFinishActivity();
			}
		});
	}

	private void loadDirectoryUp() {
		// present directory removed from list
		String s = pathDirsList.remove(pathDirsList.size() - 1);
		// path modified to exclude present directory
		path = new File(path.toString().substring(0,
				path.toString().lastIndexOf(s)));
		fileList.clear();
	}

	private void updateCurrentDirectoryTextView() {
		int i = 0;
		String curDirString = "";
		while (i < pathDirsList.size()) {
			curDirString += pathDirsList.get(i) + "/";
			i++;
		}
		if (pathDirsList.size() == 0) {
			((Button) this.findViewById(R.id.upDirectoryButton)).setEnabled(false);
			curDirString = "/";
		} else
			((Button) this.findViewById(R.id.upDirectoryButton)).setEnabled(true);

		String cd = getResources().getString(R.string.currentdirectory);
		String fp = getResources().getString(R.string.filespicked);

		((TextView) this.findViewById(R.id.currentdir)).setText(cd + ": " + curDirString);

		((TextView) this.findViewById(R.id.filespicked)).setText(fp + ": " + chosenFiles.size());

	}

	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	private void initializeFileListView() {
		ListView lView = (ListView) this.findViewById(R.id.fileListView);
		lView.setBackgroundColor(Color.LTGRAY);
		LinearLayout.LayoutParams lParam = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		lParam.setMargins(15, 5, 15, 5);
		lView.setAdapter(this.adapter);
		lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				chosenFile = fileList.get(position).file;
				File sel = new File(path + "/" + chosenFile);
				if (sel.isDirectory()) {
					if (sel.canRead()) {
						pathDirsList.add(chosenFile);
						path = new File(sel + "");
						loadFileList();
						adapter.notifyDataSetChanged();
						updateCurrentDirectoryTextView();
					} else {
						showToast(getResources().getString(R.string.cantread));
					}
				} else {
					if (sel.canRead()) {
						int rv = pickPath(path + "/" + chosenFile);

						if (rv == -1) {
							showToast(getResources().getString(R.string.toomanyfiles));
							return;
						}

						if (rv == 0) {
							view.setBackgroundColor(Color.LTGRAY);
						} else {
							view.setBackgroundColor(Color.CYAN);
						}

						updateCurrentDirectoryTextView();
					} else {
						showToast(getResources().getString(R.string.cantread));
					}
				}
			}
		});
	}

	private int pickPath(String path) {
		boolean toDelete = false;
		for (String file : chosenFiles) {
			if (file.equals(path)) {
				toDelete = true;
				break;
			}
		}

		if (toDelete) {
			chosenFiles.remove(path);
			return 0;
		}

		if (chosenFiles.size() >= MAX_FILES) 
			return -1;
			

		chosenFiles.add(path);
		
		return 1;
	}

	private void returnDirectoryFinishActivity() {
		Intent retIntent = new Intent();
		retIntent.putStringArrayListExtra(returnParameter, chosenFiles);
		this.setResult(RESULT_OK, retIntent);
		this.finish();
	}

	private void loadFileList() {
		try {
			path.mkdirs();
		} catch (SecurityException e) {
			Log.e(TAG, "Unable to write to SD CARD");
		}
		fileList.clear();

		if (path.exists() && path.canRead()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					boolean showReadableFile = showHiddenFilesAndDirs || sel.canRead();

					return ((sel.isDirectory() || sel.isFile()) && showReadableFile);
				}
			};

			String[] fList = path.list(filter);
			this.directoryShownIsEmpty = false;
			for (int i = 0; i < fList.length; i++) {
				File sel = new File(path, fList[i]);
				int drawableID;
				boolean canRead = sel.canRead();

				//if (!sel.isDirectory()) 	
				//	continue;

				if (sel.isDirectory()) {
					if (canRead) {
						drawableID = R.drawable.folder_icon;
					} else {
						drawableID = R.drawable.folder_icon_light;
					}
				} else {
					drawableID = R.drawable.file_icon;	
				}
					
				fileList.add(i, new Item(fList[i], drawableID, canRead));
			}

			if (fileList.size() == 0) {
				this.directoryShownIsEmpty = true;
				fileList.add(0, new Item(getResources().getString(R.string.emptydir), -1, true));
			} else {
				Collections.sort(fileList, new ItemFileNameComparator());
			}
		} else {
			Log.e(TAG, "path does not exist or cannot be read");
		}
	}

	private void createFileListAdapter() {
		adapter = new ArrayAdapter<Item>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				fileList) {

			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view.findViewById(android.R.id.text1);

				int drawableID = 0;
				if (fileList.get(position).icon != -1) {
					drawableID = fileList.get(position).icon;
				}
				textView.setCompoundDrawablesWithIntrinsicBounds(drawableID, 0, 0, 0);

				textView.setEllipsize(null);

				int dp3 = (int) (4 * getResources().getDisplayMetrics().density + 0.5f);
				textView.setCompoundDrawablePadding(dp3);
				textView.setBackgroundColor(Color.LTGRAY);

				return view;
			}
		};
	}

	private class Item {
		public String file;
		public int icon;
		public boolean canRead;

		public Item(String file, Integer icon, boolean canRead) {
			this.file = file;
			this.icon = icon;
		}

		public String toString() {
			return file;
		}
	}

	private class ItemFileNameComparator implements Comparator<Item> {
		public int compare(Item lhs, Item rhs) {
			return lhs.file.toLowerCase().compareTo(rhs.file.toLowerCase());
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
		}
	}

}
