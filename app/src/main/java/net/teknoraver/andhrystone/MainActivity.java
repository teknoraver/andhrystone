/*
 * Copyright (C) 2015 Matteo Croce <matteo@openwrt.org>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.teknoraver.andhrystone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;

public class MainActivity extends Activity {
	private String arch32;
	private String arch64;
	private TextView dhrystones_st32;
	private TextView dhrystones_mt32;
	private TextView dhrystones_st64;
	private TextView dhrystones_mt64;
	private Button startButton;

	private void detectCpu() {
		// Build.SUPPORTED_{32,64}_BIT_ABIS reflects what the device can actually execute.
		// Build.CPU_ABI doesn't: some recent 64-bit-only devices (e.g. Pixel 9) report an
		// arm64 CPU_ABI but have dropped 32-bit execution entirely at the kernel level, so
		// inferring 32-bit support from the CPU_ABI prefix is wrong on those devices.
		for(final String abi : Build.SUPPORTED_64_BIT_ABIS) {
			if(abi.startsWith("arm64")) {
				arch64 = "arm64";
				break;
			} else if(abi.startsWith("x86_64")) {
				arch64 = "x86_64";
				break;
			}
		}
		for(final String abi : Build.SUPPORTED_32_BIT_ABIS) {
			if(abi.startsWith("armeabi")) {
				arch32 = "arm";
				break;
			} else if(abi.startsWith("x86")) {
				arch32 = "x86";
				break;
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final TextView manufacturer = (TextView)findViewById(R.id.manufacturer);
		final TextView brand = (TextView)findViewById(R.id.brand);
		final TextView model = (TextView)findViewById(R.id.model);
		final TextView abi = (TextView)findViewById(R.id.abi);
		final TextView architecture = (TextView)findViewById(R.id.arch);
		dhrystones_st32 = (TextView)findViewById(R.id.dhrystones_st32);
		dhrystones_mt32 = (TextView)findViewById(R.id.dhrystones_mt32);
		dhrystones_st64 = (TextView)findViewById(R.id.dhrystones_st64);
		dhrystones_mt64 = (TextView)findViewById(R.id.dhrystones_mt64);
		startButton = (Button)findViewById(R.id.startDry);

		manufacturer.setText(Build.MANUFACTURER);
		brand.setText(Build.BRAND);
		model.setText(Build.MODEL);
		abi.setText(Build.CPU_ABI);

		detectCpu();

		if(arch64 != null)
			architecture.setText(arch64);
		else if(arch32 != null)
			architecture.setText(arch32);
		else {
			dhrystones_st32.setText(R.string.unknown);
			dhrystones_mt32.setText(R.string.unknown);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(arch32 != null) {
			dhrystones_st32.setText("");
			dhrystones_mt32.setText("");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.share) {
			String sharetxt = getString(R.string.sharetxt,
				Build.BRAND,
				Build.MANUFACTURER,
				Build.MODEL,
				Build.CPU_ABI,
				dhrystones_st32.getText(),
				dhrystones_mt32.getText()
			);
			startActivity(new Intent(Intent.ACTION_SEND)
				.setType("text/plain")
				.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sharesub, Build.MODEL))
				.putExtra(Intent.EXTRA_TEXT, sharetxt));
			return true;
		} else if (id == R.id.about) {
			new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setMessage(R.string.desc)
				.setIcon(android.R.drawable.ic_dialog_info)
				.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void dryStart(View v)
	{
		startButton.setEnabled(false);
		new Dhrystone().execute(0);
	}

	private class Dhrystone extends AsyncTask<Integer, Integer, Void>
	{
		private static final int MT = 1;
		private static final int B64 = 2;
		private static final int tries = 5;

		private int max = 0;
		private int mode;
		private TextView text;
		private boolean err;

		@Override
		protected Void doInBackground(Integer... params) {
			mode = params[0];
			if((mode & B64) == 0) {
				if((mode & MT) == 0)
					text = dhrystones_st32;
				else
					text = dhrystones_mt32;
			} else {
				if((mode & MT) == 0)
					text = dhrystones_st64;
				else
					text = dhrystones_mt64;
			}
			if(arch64 == null && (mode & B64) != 0)
				return null;
			if(arch32 == null && (mode & B64) == 0)
				return null;
			// Shipped as a JNI lib (not extracted from assets at runtime): on Android 10+,
			// SELinux W^X blocks executing binaries extracted into the app's own data dir,
			// but the native lib dir the OS extracts lib*.so into at install time is
			// execute-permitted.
			final String binary = "libdry-" + (((mode & B64) != 0) ? arch64 : arch32) + ".so";
			final File file = new File(getApplicationInfo().nativeLibraryDir, binary);
			try {
				for(int i = 1; i <= tries; i++) {
					publishProgress(i);
					final BufferedReader stdout = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(file.getAbsolutePath() + ((mode & MT) != 0 ? " -bt" : " -b")).getInputStream()));
					int threads = Integer.parseInt(stdout.readLine());
					int sum = 0;
					while(threads-- > 0)
						sum += Integer.parseInt(stdout.readLine());
					max = Math.max(sum, max);
				}
			} catch (final IOException | RuntimeException ex) {
				// RuntimeException covers e.g. NumberFormatException from a process that
				// exits without producing output (the exec() call itself can succeed even
				// when the kernel then refuses to run the binary, such as a 32-bit ELF on
				// a 64-bit-only device).
				ex.printStackTrace();
				err = true;
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... i) {
			text.setText(getString(R.string.runningxof, i[0], tries));
		}

		@Override
		protected void onPostExecute(Void o) {
			if(arch64 == null && (mode & B64) != 0)
				text.setText(R.string.unsupported);
			else if(arch32 == null && (mode & B64) == 0)
				text.setText(R.string.unsupported);
			else if(err)
				text.setText(R.string.unknown);
			else
				text.setText(NumberFormat.getIntegerInstance().format(max));
			if(mode != (MT | B64))
				new Dhrystone().execute(mode + 1);
			else
				startButton.setEnabled(true);
		}
	}
}
