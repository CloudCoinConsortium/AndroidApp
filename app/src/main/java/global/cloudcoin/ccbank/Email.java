package global.cloudcoin.ccbank;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.util.Log;

import java.io.File;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;


class EmailSender {
	static String TAG = "CLOUDCOIN";

	String subject, to;
	Context ctx;
	String body;

	public EmailSender(Context ctx, String to, String subject) {
		this.to = to;
		this.subject = subject;
		this.ctx = ctx;
		this.body = "";
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean openDialog() {
		String[] tos;

		tos = new String[1];
		tos[0] = this.to;

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL, tos);
		i.putExtra(Intent.EXTRA_SUBJECT, this.subject);
		i.putExtra(Intent.EXTRA_TEXT, this.body);
		try {
			ctx.startActivity(Intent.createChooser(i, "Send mail"));
		} catch (android.content.ActivityNotFoundException e) {
			Log.e(TAG, "Can not open Email Client");
			return false;
		}

		return true;
	}

	public boolean openDialogWithAttachments(ArrayList<String> filenames) {
		String[] tos;

		tos = new String[1];
		tos[0] = this.to;

		Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_EMAIL, tos);
		i.putExtra(Intent.EXTRA_SUBJECT, this.subject);
		i.putExtra(Intent.EXTRA_TEXT, this.body);

		ArrayList<Uri> uris = new ArrayList<Uri>();
		for (String file : filenames) {
			File fileIn = new File(file);
			Uri u = Uri.fromFile(fileIn);
			uris.add(u);
		}

		i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		try {
			((MainActivity) ctx).startActivityForResult(Intent.createChooser(i, "Send mail"), 66);
		} catch (android.content.ActivityNotFoundException e) {
			Log.e(TAG, "Can not open Email Client");
			return false;
		}

		return true;
	}
}
