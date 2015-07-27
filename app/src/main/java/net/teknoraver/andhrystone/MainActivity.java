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
import java.util.StringTokenizer;

public class MainActivity extends Activity {
	private String arch = detectCpu();

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

		((TextView)findViewById(R.id.manufacturer)).setText(Build.MANUFACTURER);
		((TextView)findViewById(R.id.brand)).setText(Build.BRAND);
		((TextView)findViewById(R.id.model)).setText(Build.MODEL);
		((TextView)findViewById(R.id.abi)).setText(Build.CPU_ABI);
		if(arch != null)
			((TextView)findViewById(R.id.arch)).setText(arch);
		else
			((TextView)findViewById(R.id.dhrystones)).setText(R.string.unknown);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(arch != null)
			new Dhrystone().execute();
	}

	private class Dhrystone extends AsyncTask
	{
		private String result;

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
				len = Runtime.getRuntime().exec(file.getAbsolutePath()).getInputStream().read(buf);
				StringBuilder dotted = new StringBuilder(20);
				for(int i = 0; i < len; i++){
					if(i != 0 && i % 3 == 0)
						dotted.insert(0, '.');
					dotted.insert(0, (char)buf[len - i - 1]);
				}
				result = dotted.toString();
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
		protected void onPostExecute(Object o) {
			((TextView)findViewById(R.id.dhrystones)).setText(result);
		}
	}
}
