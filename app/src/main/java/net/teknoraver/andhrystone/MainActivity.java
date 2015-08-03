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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.StringTokenizer;

public class MainActivity extends Activity {
	private String arch = detectCpu();
	private TextView manufacturer;
	private TextView brand;
	private TextView model;
	private TextView abi;
	private TextView architecture;
	private TextView dhrystones;

	private String detectX86() {
		try {
			BufferedReader in = new BufferedReader(new FileReader("/proc/cpuinfo"));
			String line;
			while((line = in.readLine()) != null) {
				if(line.startsWith("flags	")) {
					StringTokenizer flags = new StringTokenizer(line, " ");
					while(flags.hasMoreTokens())
						if(flags.nextToken().equals("lm"))
							return "x86_64";
					break;
				}
			}
		} catch (IOException e) { }
		return "x86";
	}

	private String detectCpu() {
		if(Build.CPU_ABI.startsWith("arm64"))
			return "arm64";
		if(Build.CPU_ABI.startsWith("arm"))
			return "arm";
		if(Build.CPU_ABI.startsWith("x86"))
			return detectX86();
		if(Build.CPU_ABI.startsWith("mips64"))
			return "mips64";
		if(Build.CPU_ABI.startsWith("mips"))
			return "mips";

		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		manufacturer = (TextView)findViewById(R.id.manufacturer);
		brand = (TextView)findViewById(R.id.brand);
		model = (TextView)findViewById(R.id.model);
		abi = (TextView)findViewById(R.id.abi);
		architecture = (TextView)findViewById(R.id.arch);
		dhrystones = (TextView)findViewById(R.id.dhrystones);

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
			String binary = "dry-" + arch;
			final File file = new File(getFilesDir(), binary);
			try {
				getFilesDir().mkdirs();
				final InputStream in = getAssets().open(binary);
				final FileOutputStream out = new FileOutputStream(file);
				final byte buf[] = new byte[65536];
				int len;
				while ((len = in.read(buf)) > 0)
					out.write(buf, 0, len);
				in.close();
				out.close();
				Runtime.getRuntime().exec(new String[]{"chmod", "755", file.getAbsolutePath()}).waitFor();
				for(int i = 1; i <= tries; i++) {
					publishProgress(i);
					len = Runtime.getRuntime().exec(file.getAbsolutePath()).getInputStream().read(buf);
					max = Math.max(Integer.valueOf(new String(buf, 0, len)), max);
				}
			} catch (final IOException ex) {
				ex.printStackTrace();
				Toast.makeText(MainActivity.this, "error:" + ex, Toast.LENGTH_LONG);
				finish();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... i) {
			dhrystones.setText(getString(R.string.runningxof, i[0].intValue(), tries));
		}

		@Override
		protected void onPostExecute(Void o) {
			dhrystones.setText(NumberFormat.getIntegerInstance().format(max));
		}
	}
}
