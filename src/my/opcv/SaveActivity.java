package my.opcv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

public class SaveActivity extends Activity {
	public static final String appDir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/ElektroOCR/data/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_save);
		
		Intent intent = getIntent();
		final String res = intent.getStringExtra("result");
		
		EditText text = (EditText) findViewById(R.id.editText1);
		text.setText(res);
		
		Button saveBt = (Button) findViewById(R.id.button1);
		saveBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {
            		String saveLine = "";
            		Time today = new Time(Time.getCurrentTimezone());
            		today.setToNow();
            		
            		EditText text = (EditText) findViewById(R.id.editText1);
            		EditText num = (EditText) findViewById(R.id.editText2);
            		RadioGroup g = (RadioGroup) findViewById(R.id.gr);
            		int radioButtonID = g.getCheckedRadioButtonId();
            		View radioButton = g.findViewById(radioButtonID);
            		int idx = g.indexOfChild(radioButton);
            		
            		
            		String cas = today.format("%k:%M:%S");
            		String res = text.getText().toString();
            		String cisId = num.getText().toString();
            		String tarif="";
            		if(idx==0)
            			tarif = "Vysoký tarif";
            		else
            			tarif = "Nízký tarif";
            		
            		saveLine += cas+" | "+cisId+" | "+res+" | "+tarif+";";
					writeToFile(saveLine);
					
					Intent myIntent = new Intent(SaveActivity.this, OpencvLoader.class);
					SaveActivity.this.startActivity(myIntent);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.save, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void writeToFile(String radek) throws IOException {
		File f = new File(appDir+"vysledky.txt");
		if(!f.exists()) {
		    f.createNewFile();
		} 
		FileOutputStream oFile = new FileOutputStream(f, true); 
		oFile.write((radek+"\n").getBytes());
		oFile.close();
	}
}
