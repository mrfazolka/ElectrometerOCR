package my.opcv;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Random;

import my.nn.DataSet;
import my.nn.InputLayer;
import my.nn.PerceptronLayer;
import my.nn.PerceptronNetwork;
import my.nn.Sample;
import my.nn.SerializeNN;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvSVM;

import android.os.Environment;
import android.widget.ImageView;

public abstract class ElektrometerImageMatProcessor {
	public static final String TAG = "ElektrometerImageMatProcess::Class";
	
	public static final String appDir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/ElektroOCR/data/";
	
	OpencvLoader opcvActivity;
	
	boolean doProcess = true;
	
	public Mat eyeRect;
	public Mat eyeWithoutRect;
	public Mat webcam_image;
	
	public int snimaniSaveSnimanyElektromerNum = 0;
	public int snimaniSaveStatusAreaNum = 0;
	public int snimaniSaveNumberNum = 0;
	public boolean saveElektromery = false;
	
	int num=0;
    
    final int numberWidth = 40; 
    final int numberHeight = 64; 
    
    final int eyeSize = 140;
    //
    
    public String result = "";
    
    //NN
    public PerceptronNetwork net;
    public SerializeNN serializeNN;
    private DataSet dataSet;
    
    //SVM
    CvSVM svmClassifier;
//    CvSVM svmNumberClassifier;
    
    ImageView statusAreaImageView;
    boolean alreadyDone=false;
    
    public String saveDir = null;
	
    ElektrometerImageMatProcessor(OpencvLoader opcvActivity)
    {
    	this.opcvActivity = opcvActivity;
    }
    
    public abstract Mat process(Mat input);
    
    
    
    
    // leadn NN //
    protected void createNNbyParamsAndLearn(String jsonMatNumbersForDatasetPath, String layerParam)
    {
         this.net = new PerceptronNetwork();
        //vytvoří dataSet pro učení 
         dataSet = this.createDataSetFromJsonMats(jsonMatNumbersForDatasetPath);
	 	
	 	 InputLayer inp = new InputLayer(64);
         Random rnd = new Random(10);
         PerceptronLayer hidden = new PerceptronLayer(30); //rozpozná vše - při orig obrázcích - hodnota 20
         PerceptronLayer out = new PerceptronLayer(10);
         hidden.connect(inp);
         out.connect(hidden);
         this.net.addLayer(inp);
         this.net.addLayer(hidden);
         this.net.addLayer(out);
	 	 this.net.initWeight(-0.3, 0.3, rnd); //rozpozná vše - při orig obrázcích
	 	 
         Double chyba = 0.0;
	 	 double eta = 0.01;
	 	 double alpha = 0.0;
         for(int i = 0; i < 3000; i++)
         {
             
             chyba = net.learnEpoch(dataSet, eta, alpha);
//             System.out.println("MSE = " + chyba);
//             System.out.println("RMS = " + Math.pow(chyba, 0.5));
         }
 	
	// 	net.print(System.out);
	 	System.out.println("nauceno");
     }
    
    private DataSet createDataSetFromJsonMats(String filePath)
    {
	HashMap<Mat, double[]> imageMap = this.loadJsonMatsFromFile(appDir+filePath);
	DataSet dataSet = new DataSet();
	
	//vypiš co je v poli atributů
	for(Mat img : imageMap.keySet())
	{
	    double[] output = imageMap.get(img);
	    double[] input = this.getNumberImageAttributes(img);
	    dataSet.add(new Sample(input, output));
	}
	
	System.out.println("Dataset created");

	return dataSet;
    }
   private HashMap<Mat, double[]> loadJsonMatsFromFile(String imageDirPath)
    {
	HashMap<Mat, double[]> mapOfBufferedImages = new HashMap();
	
	// File representing the folder that you select using a FileChooser
	File dir = new File(imageDirPath);
	// array of supported extensions (use a List if you prefer)
	final String[] EXTENSIONS = new String[]{
	    "json" // and other formats you need
	};
	// filter to identify images based on their extensions
	final FilenameFilter IMAGE_FILTER = new FilenameFilter() {
	    @Override
	    public boolean accept(final File dir, final String name) {
		for (final String ext : EXTENSIONS) {
		    if (name.endsWith("." + ext)) {
			return (true);
		    }
		}
		return (false);
	    }
	};
	
	Mat img = null;
	//projde obrázky ve složce
	if (dir.isDirectory()) { // make sure it's a directory
            for (final File f : dir.listFiles(IMAGE_FILTER)) {
		img = MatSerializerJson.loadMatFromJsonFile(f.getPath());
//		    mapOfBufferedImages.put(img, "0."+f.getName().replaceFirst("[.][^.]+$", ""));
		String znak = f.getName().replaceFirst("[.][^.]+$|[-][^-]+$", "");
		double[] sampleOutput = this.prevedNaInput(znak);

		mapOfBufferedImages.put(img, sampleOutput);
            }
        }
	else
	{
	    System.out.println("Nejedná se o složku");
	}
	
	return mapOfBufferedImages;
    }
   
   private double[] prevedNaInput(String znak) {
		int cislo = Integer.parseInt(znak);
		double[] output = {0,0,0,0,0,0,0,0,0,0}; //maska pro Sample output
		output[cislo] = 1;
		
		return output;
	    }

   protected Character getRozpoznaneCislo(Mat numberImg)
   {
		Character rozpoznaneCisloChar = '?';
		;

		double[] cisloAttributes = this.getNumberImageAttributes(numberImg);
		double[] vystupPredikceZnaku = this.rozpoznej(cisloAttributes);
		double rozpoznaneCislo = vystupPredikceZnaku[0];
		double pravdepodobnostSpravnosti = vystupPredikceZnaku[1];
		if(pravdepodobnostSpravnosti>0.8)
		    rozpoznaneCisloChar = (char) ('0' + (int)rozpoznaneCislo); 
		else
		    rozpoznaneCisloChar = '?'; 
		
		return rozpoznaneCisloChar;
   }
   

   private double[] getNumberImageAttributes(Mat numberImg)
   {
		final int squareWidth = 5;
		final int squareHeight = 8;
		double attributes[] = new double[64];
		
		int i=0;
		int j=0;
		int attrPos = 0;
		while(i<numberImg.rows()) //přeskakuj po obdélnících od zhora dolů
		{
		    j=0;
		    while(j<numberImg.cols()) //přeskakuj po čtvercích z leva do prava
		    {
			//projdi vnitřní obdélník a spočítej černé pixely
			int blackSum=0;
			for(int sqRow=i; sqRow<i+squareHeight; sqRow++)
			    for(int sqCol=j; sqCol<j+squareWidth; sqCol++)
			    {
					if(numberImg.get(sqRow,sqCol)[0] == 0) //jde o černý pixel?
					    blackSum++;
			    }
			attributes[attrPos++] = blackSum;
			
			j+=squareWidth;
		    }
		    
		    i+=squareHeight;
		}
//    		System.out.println(attributes.length);
		
		return attributes;
   }
   
   private double[] rozpoznej(double[] input)
   {
	//vypočti výstupy pro zadaný input k rozpoznani
	double[] vystupySite = this.net.recall(input);

	double[] znakHodnota = this.getRozpoznano(vystupySite);

	return znakHodnota;
   }
   
   private double[] getRozpoznano(double[] vystupySite)
   {
	double prahNerozpoznani = 0.5;
	double maxY = 0;
	int maxIndex = 0; 

	int i = 0;
	for(double neuronY : vystupySite)
	{
	    if(neuronY > maxY)
	    {
		maxY = neuronY;
		maxIndex = i;
	    }
	    
	    i++;
	}
	
	double[] znakHodnota = {maxIndex, maxY}; //maxIndex reprezentuje pořadí neuronu výstupní vrstvy, který nejvíce excituje - pořadí neuronu je rovno rozpoznanému číslu; maxY je hodnota excitace(z kolika procent si neuron myslí, že jde o číslo, které daný neuron reprezentuje(jak moc si třetí neuron myslí, že předložené číslo na obrázku byla trojka)
	return znakHodnota;
	
   }
   
   public void saveEyeStatusAreaAndNumbers(Mat eyeWithoutRect, Mat eyeRect, Mat statusArea, Mat oneNumber, int numberAtStatusAreaIndex, String saveFolder)
   {   
	if(saveFolder == null)
	    saveFolder = this.saveDir+"/";
	
    	if(this.saveElektromery) 
   	{
   		Mat eyeWithoutRectBGR = new Mat();
   		Mat eyeRectBGR = new Mat();
   		Mat statusAreaBGR = new Mat();
   		Imgproc.cvtColor(eyeWithoutRect, eyeWithoutRectBGR, Imgproc.COLOR_RGB2BGR);
   		Imgproc.cvtColor(eyeRect, eyeRectBGR, Imgproc.COLOR_RGB2BGR);
   		Imgproc.cvtColor(statusArea, statusAreaBGR, Imgproc.COLOR_RGB2BGR);
	    	
	    	if(numberAtStatusAreaIndex==1)
	    	{
	    		this.snimaniSaveStatusAreaNum++;
		    	File elektromerDir = new File (appDir+saveFolder+this.snimaniSaveSnimanyElektromerNum);
		    	elektromerDir.mkdirs();
	    		
	    		String eyeRectFileName = this.snimaniSaveSnimanyElektromerNum+"/"+this.snimaniSaveStatusAreaNum+"_eyeRect.jpg";
	    		String eyeFileName = this.snimaniSaveSnimanyElektromerNum+"/"+this.snimaniSaveStatusAreaNum+"_eye.jpg";
	    		String statusAreaFileName = this.snimaniSaveSnimanyElektromerNum+"/"+this.snimaniSaveStatusAreaNum+"_area.jpg";
	    		Highgui.imwrite(appDir+saveFolder+eyeFileName, eyeWithoutRectBGR);
	    		Highgui.imwrite(appDir+saveFolder+eyeRectFileName, eyeRectBGR);
	    		Highgui.imwrite(appDir+saveFolder+statusAreaFileName, statusAreaBGR);
	    		
	    		String eyeFileNameJson = this.snimaniSaveSnimanyElektromerNum+"/"+this.snimaniSaveStatusAreaNum+"_eye.json";
			    MatSerializerJson.saveMatAsJson(eyeWithoutRect.clone(), appDir+saveFolder+eyeFileNameJson); //ulo� matici serializovanou do jsonu
	    	}
		    
		//save number
	    String numberFileName = this.snimaniSaveSnimanyElektromerNum+"/"+this.snimaniSaveStatusAreaNum+"_num"+numberAtStatusAreaIndex+".jpg"; // ��slo statusArei(�iseln�ku)/pocet kolikr�t bylo aplikac� ukl�d�no ��slo-po�ad� ��sla na jednom ��seln�ku.jpg
		Highgui.imwrite(appDir+saveFolder+numberFileName, oneNumber);
		String numberFileNameJson = this.snimaniSaveSnimanyElektromerNum+"/"+this.snimaniSaveStatusAreaNum+"_num"+numberAtStatusAreaIndex+".json";   
		MatSerializerJson.saveMatAsJson(oneNumber, appDir+saveFolder+numberFileNameJson);
		
		this.snimaniSaveNumberNum++;
   	}
   }
   
}
