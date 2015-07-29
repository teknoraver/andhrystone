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
			new Dhrystone().execute();
	}

	private class Dhrystone extends AsyncTask
	{
		private static final int TRIES = 5;
		private int iter;
		private String result;
		private int max = 0;

		@Override
		protected Object doInBackground(Object[] params) {
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
				for(iter = 1; iter <= TRIES; iter++) {
					len = Runtime.getRuntime().exec(file.getAbsolutePath()).getInputStream().read(buf);
					max = Math.max(Integer.valueOf(new String(buf, 0, len)), max);
					publishProgress(null);
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
		protected void onProgressUpdate(Object[] values) {
			dhrystones.setText(getString(R.string.runningxof, iter, TRIES));
		}

		@Override
		protected void onPostExecute(Object o) {
			dhrystones.setText(NumberFormat.getIntegerInstance().format(max));
		}
	}
}
