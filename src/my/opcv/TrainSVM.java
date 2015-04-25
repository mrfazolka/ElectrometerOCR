package my.opcv;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

/**
 *
 * @author Ondøej
 */
public class TrainSVM {
    public Mat classes = new Mat();//(numPlates+numNoPlates, 1, CV_32FC1);
    public Mat trainingData = new Mat();//(numPlates+numNoPlates, imageWidth*imageHeight, CV_32FC1 );

    private Mat trainingImages = new Mat();
    private ArrayList<Integer> trainingLabels = new ArrayList();
    
    final int imageWidth=144;
    final int imageHeight=33;
    
    public TrainSVM()
    {
	
    }

    public void createDataSet(String platesDir, String noPlatesDir)
    {
		int numPlates = this.loadReshapedImagesFromFile(platesDir, true); //naèti svmDataSet obrázkù reprezentujících spz
		int numNoPlates = this.loadReshapedImagesFromFile(noPlatesDir, false); //naèti svmDataSet obrázkù, které nejsou spz
		
		trainingData = new Mat(trainingImages.rows(), this.imageWidth*this.imageHeight, CvType.CV_32FC1);
		trainingImages.copyTo(trainingData);
		trainingData.convertTo(trainingData, CvType.CV_32FC1);
	//	trainingLabels.copyTo(classes);
		classes.convertTo(classes, CvType.CV_32SC1);
    }
    
    private int loadReshapedImagesFromFile(String imageDirPath, boolean plate) //naète resizlé obrázky a pøidá do trénovacích sad podle toho jestli jde o spz nebo ne
    {
		int fileCount=0;
		// File representing the folder that you select using a FileChooser
		File dir = new File(imageDirPath);
		// array of supported extensions (use a List if you prefer)
		final String[] EXTENSIONS = new String[]{
		    "gif", "png", "bmp", "jpg" // and other formats you need
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
		
		//projde obrázky ve složce
		Mat matImg = new Mat();
		if (dir.isDirectory()) { // make sure it's a directory
	            for (final File f : dir.listFiles(IMAGE_FILTER)) {
	                matImg = Highgui.imread(f.getPath(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
			Mat reshapedImg = matImg.reshape(1, 1);
			
			this.trainingImages.push_back(reshapedImg);
			if(plate)
			{
			    this.classes.push_back(Mat.ones(1, 1, CvType.CV_32FC1)); //vloží do matice nový øádek o jednom sloupci s hodnotou jedna
			}
			else
			{
			    this.classes.push_back(Mat.zeros(1, 1, CvType.CV_32FC1)); //vloží do matice nový øádek o jednom sloupci s hodnotou nula
			}
			
			fileCount++;
	            }
	        }
		
		return fileCount;
    }
    
    public void saveDataSet()
    {
	TaFileStorage fs = new TaFileStorage();
	fs.open("data/SVMmy.xml", TaFileStorage.WRITE);
	fs.writeMat("TrainingData", this.trainingData);
	fs.writeMat("classes", this.classes);
	fs.release();
    }
}
