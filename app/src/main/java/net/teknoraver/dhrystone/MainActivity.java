package net.teknoraver.dhrystone;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onStart() {
		super.onStart();
		new Dhrystone().execute();
	}

	private class Dhrystone extends AsyncTask
	{
		private String result;
		private String arch = detectCpu();

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

		@Override
		protected Object doInBackground(Object[] params) {
			String binary = "dry-" + arch;
			final File file = new File(getFilesDir(), binary);
			try {
				getFilesDir().mkdirs();
				final InputStream in = getAssets().open(binary);
				final FileOutputStream out = new FileOutputStream(file);
				final byte[] buf = new byte[65536];
				int len;
				while ((len = in.read(buf)) > 0)
					out.write(buf, 0, len);
				in.close();
				out.close();
				Runtime.getRuntime().exec(new String[]{"chmod", "755", file.getAbsolutePath()});
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
			}
			return null;
		}

		@Override
		protected void onPostExecute(Object o) {
			findViewById(R.id.progress).setVisibility(View.GONE);
			TextView r = (TextView) findViewById(R.id.result);
			StringBuilder txt = new StringBuilder()
				.append("ABI:\t").append(Build.CPU_ABI).append("\n")
				.append("ABI2:\t").append(Build.CPU_ABI2).append("\n")
				.append("arch:\t").append(arch).append("\nresult:\t").append(result);
			r.setText(txt);
			r.setVisibility(View.VISIBLE);
		}
	}
}
