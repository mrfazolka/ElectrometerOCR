package my.opcv;

import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TableRow;

public class OpencvLoader extends Activity implements CvCameraViewListener2{
	public static final String TAG = "OpencvLoader::Activity";
	private CameraBridgeViewBase mOpenCvCameraView;
	
	public ElektrometerImageMatProcessor elMatProcess;
	private Mat myMat;
	
	private String processMode = "digit";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        /** Create a TextView and set it to display
        * text from a constant string.
//        */
//        TextView tv = new TextView(this);
//        tv.setText(stringFromJNI());
//        setContentView(tv);
        
//        this.elMatProcess = new AnalogElProcessor(this);
        this.elMatProcess = new DigitElProcessor(this);
        
//        Log.i(TAG, stringFromJNI());
        
        Log.i(TAG, "called onCreate");
	    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	    setContentView(R.layout.activity_opcv);
	    
	    Button button = (Button) findViewById(R.id.btSaveElektromery);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                OpencvLoader.this.elMatProcess.snimaniSaveSnimanyElektromerNum++;
            }
        });
        
        Button buttonPokracuj = (Button) findViewById(R.id.button1);
        buttonPokracuj.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                OpencvLoader.this.elMatProcess.doProcess=true;
                Button buttonPokracuj = (Button) findViewById(R.id.button1);
                buttonPokracuj.setEnabled(false);
                buttonPokracuj.setText("Running");
                buttonPokracuj.setTextColor(Color.GRAY);
            }
        });
        
        Button btUloz = (Button) findViewById(R.id.button2);
        btUloz.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Intent myIntent = new Intent(OpencvLoader.this, SaveActivity.class);
            	EditText text = (EditText) findViewById(R.id.editText1);
   	  		 	String res = text.getText().toString();
            	myIntent.putExtra("result", res);
            	OpencvLoader.this.startActivity(myIntent);
            }
        });
        
       //attach a listener to check for changes in state
        Switch mySwitch = (Switch) findViewById(R.id.switch1);
        //set the switch to ON 
        mySwitch.setChecked(true);
        mySwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
       
         @Override
         public void onCheckedChanged(CompoundButton buttonView,
           boolean isChecked) {
       
           processMode = processMode == "digit" ? "analog" : "digit"; //switch analog/digit
         }
        });
	    
	    mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.OpenCvView);
	    mOpenCvCameraView.setCvCameraViewListener(this);
	}
	
	/** A native method that is implemented by the
     *	'testjni' native library, which is packaged
     * with this application.
     */
     public native String stringFromJNI();
     /** Load the native library where the native method
     * is stored.
     */
     static {
           System.loadLibrary("testjni");
     }
     
     
	 ///////////////////////// OPENCV /////////////////////////
     private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
     {
 	    @Override
 	    public void onManagerConnected(int status) {
 	        switch (status) {
 	            case LoaderCallbackInterface.SUCCESS:
 	            {
 	            	Log.i(TAG, "OpenCV loaded successfully");
 	                mOpenCvCameraView.enableView();
 	                
 	            } break;
 	            default:
 	            {
 	                super.onManagerConnected(status);
 	            } break;
 	        }
 	    }
 	};

 	@Override
	public void onResume()
	{
	    super.onResume();
//	    if(!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback)) 
//	    	mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
	    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
	}

 	@Override
	 public void onPause()
	 {
	     super.onPause();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }

	 public void onDestroy() {
	     super.onDestroy();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }

	 public void onCameraViewStarted(int width, int height) {
	 }

	 public void onCameraViewStopped() {
	 }

	 public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		 if(this.processMode == "digit" && this.elMatProcess instanceof AnalogElProcessor) //bylo pøepnuto z analog na digit
		 {
			 this.elMatProcess = new DigitElProcessor(this);
		 }
		 else if(this.processMode == "analog" && this.elMatProcess instanceof DigitElProcessor)
		 {
			 this.elMatProcess = new AnalogElProcessor(this);
		 }
			 
		 this.elMatProcess.process(inputFrame.rgba());
		 //myMat = webcam_image;
		 
		 return myMat;
	 }
	 
     public void helloworld()
     {
    	        // make a mat and draw something
    	        Mat m = Mat.zeros(100,400, CvType.CV_8UC3);
    	        Core.putText(m, "hi there ;)", new Point(30,80), Core.FONT_HERSHEY_SCRIPT_SIMPLEX, 2.2, new Scalar(200,200,0),2);

    	        // convert to bitmap:
    	        Bitmap bm = Bitmap.createBitmap(m.cols(), m.rows(),Bitmap.Config.ARGB_8888);
    	        Utils.matToBitmap(m, bm);

    	        // find the imageview and draw it!
    	        ImageView iv = (ImageView) findViewById(R.id.imageView1);
    	        iv.setImageBitmap(bm);
    	        
    	        this.myMat = Mat.zeros(100,400, CvType.CV_8UC3);
    }
     
     
    //////////// vykreslování ///////////////

	public void setImageEye(Mat mat)
	{
	  final Mat m = mat;
	   runOnUiThread(new Runnable() {
	      @Override
	      public void run() {
	    	// convert to bitmap:
		        Bitmap bm = Bitmap.createBitmap(m.cols(), m.rows(),Bitmap.Config.ARGB_8888);
		        Utils.matToBitmap(m, bm);
	
		        // find the imageview and draw it!
		        ImageView iv = (ImageView) findViewById(R.id.imageView1);
		        iv.setImageBitmap(bm);
	      }
	  });
	}
	
	public void setImageStatusArea(Mat mat)
	{
	  final Mat m = mat;
	   runOnUiThread(new Runnable() {
	      @Override
	      public void run() {
	    	// convert to bitmap:
		        Bitmap bm = Bitmap.createBitmap(m.cols(), m.rows(),Bitmap.Config.ARGB_8888);
		        Utils.matToBitmap(m, bm);
	
		        // find the imageview and draw it!
		        ImageView iv = (ImageView) findViewById(R.id.imageView2);
		        iv.setImageBitmap(bm);
	      }
	  });
	}
	
	public void setImagesNumbers(ArrayList<NumberSegment> nums)
	{
	  final ArrayList<ImageView> imgViews = new ArrayList<ImageView>();
	  for(NumberSegment numberSegment : nums)
	  {
		   ImageView imgView = new ImageView(this);
//		   imgView.getLayoutParams().height = imgView.getLayoutParams().WRAP_CONTENT;
//		   imgView.getLayoutParams().width = imgView.getLayoutParams().WRAP_CONTENT;
		   Bitmap bm = null;
		   int width = numberSegment.img.cols();
		   int height = numberSegment.img.rows();
		   bm = Bitmap.createBitmap(numberSegment.img.cols(), numberSegment.img.rows(),Bitmap.Config.ARGB_8888);
		   Utils.matToBitmap(numberSegment.img, bm);
		   bm = Bitmap.createScaledBitmap(bm, (int)(width*2.3), (int)(height*2.3), true);
	       imgView.setImageBitmap(bm);
	       
	       Bitmap bmp = Bitmap.createBitmap((int)(width*1.5), (int)(height*2.3), Bitmap.Config.ARGB_8888);
	       ImageView empty = new ImageView(this);
	       empty.setImageBitmap(bmp);
	       
	       imgViews.add(empty);
		   imgViews.add(imgView);
	  }
	  
	   runOnUiThread(new Runnable() {
	      @Override
	      public void run() {
	    	 TableRow tr = (TableRow) findViewById(R.id.tableRow1);
	    	 tr.removeAllViews();
	    	 for(ImageView iv : imgViews)
			 {
	    		 tr.addView(iv);
			 }
	      }
	  });
	}

	
	public void setResultText(String result)
	{
	   final String r = result;
	   runOnUiThread(new Runnable() {
	      @Override
	      public void run() {
	    	  EditText text = (EditText) findViewById(R.id.editText1);
	  		 text.setText(r);
	      }
	  });
	}
    
    public void enablePokracujBtt()
    {
 	   runOnUiThread(new Runnable() {
 	      @Override
 	      public void run() {
 	    	 Button buttonPokracuj = (Button) findViewById(R.id.button1);
             buttonPokracuj.setEnabled(true);
             buttonPokracuj.setText("Start!");
             buttonPokracuj.setTextColor(Color.rgb(0,100,0));
 	      }
 	  });
 	}   
}
