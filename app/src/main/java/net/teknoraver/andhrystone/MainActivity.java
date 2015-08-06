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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;

public class MainActivity extends Activity {
	private final String arch = detectCpu();
	private TextView dhrystones;

	@SuppressWarnings("deprecation")
	private String detectCpu() {
		if(Build.CPU_ABI.startsWith("arm64"))
			return "arm64";
		if(Build.CPU_ABI.startsWith("arm"))
			return "arm";
		if(Build.CPU_ABI.startsWith("x86_64"))
			return "x86_64";
		if(Build.CPU_ABI.startsWith("x86"))
			return "x86";
		if(Build.CPU_ABI.startsWith("mips64"))
			return "mips64";
		if(Build.CPU_ABI.startsWith("mips"))
			return "mips";

		return null;
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
		dhrystones = (TextView)findViewById(R.id.dhrystones_st);

		manufacturer.setText(Build.MANUFACTURER);
		brand.setText(Build.BRAND);
		model.setText(Build.MODEL);
		abi.setText(Build.CPU_ABI);

		if(arch != null)
			architecture.setText(arch);
		else
			dhrystones.setText(R.string.unknown);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(arch != null)
			new Dhrystone().execute(5);
	}

	private class Dhrystone extends AsyncTask<Integer, Integer, Void>
	{
		private int tries;
		private int max = 0;

		@Override
		protected Void doInBackground(Integer... params) {
			tries = params[0];
			final String binary = "dry-" + arch;
			final File file = new File(getFilesDir(), binary);
			try {
				getFilesDir().mkdirs();

				{
					final InputStream in = getAssets().open(binary);
					final FileOutputStream out = new FileOutputStream(file);
					final byte buf[] = new byte[65536];
					for (int len; (len = in.read(buf)) > 0; )
						out.write(buf, 0, len);
					in.close();
					out.close();
				}
				Runtime.getRuntime().exec(new String[]{"chmod", "755", file.getAbsolutePath()}).waitFor();
				for(int i = 1; i <= tries; i++) {
					publishProgress(i);
					final BufferedReader stdout = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(file.getAbsolutePath()).getInputStream()));
					for(String line; (line = stdout.readLine()) != null; ) {
						if(line.startsWith("Dhrystones per Second:")) {
							int len = Integer.valueOf(line.substring(line.lastIndexOf('	') + 1));
							max = Math.max(len, max);
							break;
						}
					}
				}
			} catch (final IOException | InterruptedException ex) {
				ex.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... i) {
			dhrystones.setText(getString(R.string.runningxof, i[0], tries));
		}

		@Override
		protected void onPostExecute(Void o) {
			dhrystones.setText(NumberFormat.getIntegerInstance().format(max));
		}
	}
}
