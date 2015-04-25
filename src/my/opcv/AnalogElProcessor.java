package my.opcv;

import java.util.ArrayList;

import my.nn.DataSet;
import my.nn.PerceptronNetwork;
import my.nn.SerializeNN;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvSVM;

import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;

public class AnalogElProcessor extends ElektrometerImageMatProcessor {
	
	AnalogElProcessor(OpencvLoader opcvActivity) {
		super(opcvActivity);
	}

	@Override
	public Mat process(Mat input)
    {
    	//proveï pouze jednou
			if(!this.alreadyDone)
			{
				this.initAI();
				this.alreadyDone = true;
				this.saveDir = "snimaniSaveAnalog";
			}
			
	 		this.webcam_image = input;
	 		
	 		Imgproc.cvtColor(webcam_image, webcam_image, Imgproc.COLOR_RGBA2RGB); //konverze bez alfa kanalu, jinak nejde getrectsubpix
	 		
	 		double pomer = 280./65.;
	  		double rectWidth = this.eyeSize*pomer;
	  		double rectHeigh = this.eyeSize; //zmìna velikosti "oka"
	  		int inputCenterX = webcam_image.cols()/2;
	  		int inputCenterY = webcam_image.rows()/2;
	//  		
	  		Rect centerRect=new Rect(inputCenterX-(int)rectWidth/2,inputCenterY-(int)rectHeigh/2,(int)rectWidth,(int)rectHeigh);
	//  		
			    this.eyeRect = new Mat(webcam_image, centerRect); //NÍŽE DO tohoto obrázku preslím
			    
			    if(this.snimaniSaveSnimanyElektromerNum>0)
			    {
			    	this.saveElektromery = true;
			    	//nastavuj jen když ukládám obrázky elektromìrù
			    	this.eyeWithoutRect = this.eyeRect.clone(); //uchovávám jen pro ukládání eye (èistého výseku bez orámování kolem èíselníku)
			    }
	//		    opcvActivity.setImageEye(eyeRect);
			    
			    ArrayList<StatusAreaAnalog> statusAreaCandidates = this.getAreaCandidates(eyeRect); //všichni kandidáti na èíselník
			    ArrayList<StatusAreaAnalog> statusAreaBySVM = getDetectedStatusAreas(statusAreaCandidates); //detekované statusArei klasifikované svm		    
	//		    //projdi všechny plochy s èíselníkem
			    for(StatusAreaAnalog statusArea : statusAreaBySVM) //projdu statusArei, které byly klasifikovány SVM
			    {
				    	//nakreslý okolo detekovaného èíselníku obrys
				    	org.opencv.core.Point rect_points[] = new org.opencv.core.Point[4];
				    statusArea.rectBorder.points(rect_points);
				    for( int j = 0; j < 4; j++ )
				       Core.line(this.eyeRect, rect_points[j], rect_points[(j+1)%4], new Scalar(0,255,0), 1);
				    
				    if(this.doProcess) //zajistí rozpoznání pouze když uživatel klikne na tlaèítko
			    	{
				    	this.doProcess = false;
						opcvActivity.enablePokracujBtt();
						
				    	opcvActivity.setImageStatusArea(statusArea.imgOrig);
				    	
			// 			Highgui.imwrite("data/test/"+(num++)+".jpg", statusArea.imgYchannel);
			 			//extrahuj z èíselníku obrázky èísel
			 			ArrayList<NumberSegment> extractedNumbers = this.segmentStatusAreaAndExtractNumbers(statusArea);
			 			
			 			//projdi všechny obrázky èísel na èíselníku
			 			for(NumberSegment numberSegment : extractedNumbers)
			 			{
			 				//vytvoøit a ulož data pro uèení nn
			 			    //Highgui.imwrite(appDir+"cislaZmobilu/"+(num++)+".jpg", numberSegment.img);
			 			    
			 			    Character rozpoznaneCisloChar = this.getRozpoznaneCislo(numberSegment.img); //rozpoznej znak
			 			    
			 			    //ulož rozpoznané a pozici rozpoznaného
			 			    statusArea.recognizedNumbers.add(rozpoznaneCisloChar); //pøidej rozpoznaný znak - znaky nejsou seøazeny
			 			    statusArea.numbersPosition.add(numberSegment.pos); //díky pos mùžee následnì správnì poskládat èísla na èíselníku jak jdou za sebou
			 			}
			 			statusArea.createOrderedImagesAndResult(extractedNumbers); //vytvoøí seøazenou kolekci vyseklých èísel od prvního k poslednímu(z leva do prava)
			 			
			 			opcvActivity.setImagesNumbers(statusArea.orderedNumbersImgs); //zobraz obrázky seøazených vyseklých èísel èíselníku
			 			
			 			if(statusArea.result.length()>4 && !"nerozpoznano".equals(statusArea.result))
				 		{
			 				this.result = statusArea.result;
			 				//Core.putText(eyeRect, statusArea.result, new Point(30, 30), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0,0,200),2);
				 		}
			 			
			 			opcvActivity.setResultText(statusArea.result);
			    	}
		 			
			    }
			    
			    opcvActivity.setImageEye(eyeRect);
			    
			    return input;
	    }

	//--------------------------------------
	
	
	
	   public boolean verifyNumberSizes(Mat r){
		//Char sizes 45.0fx77.0f
		double aspect=40.0/55.0;
		double charAspect= (double)r.cols()/(double)r.rows();
	//	System.out.println(aspect+" vs "+charAspect);
		double error=0.1;
		float minHeight=20; //MIN VÝŠKA obrázku v resizlém èíselníku - 20
		float maxHeight=50; //MAX VÝŠKA obrázku v resizlém èíselníku - 50
		//We have a different aspect ratio for number 1, and it can be ~0.2
		double minAspect=0.1;
		double maxAspect=aspect+aspect*error;
		double area= r.rows() * r.cols();
		
		double min = (minHeight*aspect*minHeight)*0.9; // minimum area
		double max = maxHeight*aspect*maxHeight; // maximum area
		
		if(area<min || area>max)
		    return false;
		
		if(charAspect > minAspect && charAspect < maxAspect && r.rows() >= minHeight && r.rows() < maxHeight)
		    return true;
		else
		    return false;
	
	   }
	   
	   //ovìøí velikost obdélníku kolem contoury
	   private boolean verifyStatusAreaSizes(RotatedRect rect)
	   {
			double error=0.5; //orig: 0,4
			//pomìry šíøka/výška podle nahoøe vypsaných namìøených velikostí stavové plochy
			final double aspect=485./117.;
			
			final int minHeigh = 15; //orig 15
			final int maxHeigh = 350; //orig 125
			
			double min = minHeigh*aspect*minHeigh; // minimum area
			double max = maxHeigh*aspect*maxHeigh; // maximum area
			
			double rmin= aspect-aspect*error;
			double rmax= aspect+aspect*error;
			
			double area= rect.size.height * rect.size.width;
			double r = (double)rect.size.width / (double)rect.size.height;
			
			if(r<1)
			{
			    r = 1/r;
			}
			
			if((area<min || area>max) || (r<rmin || r>rmax )){
			    return false;
			}
			else
			{
			    return true;
			}
	   }
	   
	   private Mat cropRect(RotatedRect rr, Mat input)
	   {
	   	RotatedRect rect = rr.clone();
	       Mat M = new Mat(), rotated = new Mat(), cropped = new Mat();
	       float angle = (float)rect.angle;
	       Size rect_size = rect.size;
	       if (rect.angle < -45.) {
	           angle += 90.0;
	           double origW=rect_size.width;
			    rect_size.width = rect_size.height;
			    rect_size.height = origW;
	       }
	       // get the rotation matrix
	       M = Imgproc.getRotationMatrix2D(rect.center, angle, 1.0);
	       // perform the affine transformation
	       Imgproc.warpAffine(input, rotated, M, input.size(), Imgproc.INTER_CUBIC);
	       // crop the resulting image    	        
	       Imgproc.getRectSubPix(rotated, rect_size, rect.center, cropped);
	       
	   	return cropped;
	   }
	   
	   private Mat resizeStatusArea(Mat cropedRect)
	   {
			Mat resultResized = new Mat(); //resizlý obrázek spz z originálu obrazu
			resultResized.create(65,280, CvType.CV_8UC3);
			Imgproc.resize(cropedRect, resultResized, resultResized.size(), 0, 0, Imgproc.INTER_CUBIC);
	
			return resultResized;
	   }
	   
	   private Mat histeq(Mat in)
	   {
		Mat out = new Mat(in.size(), in.type());
		if(in.channels()==3){
		    Mat hsv = new Mat();
		    ArrayList<Mat> hsvSplit=new ArrayList();
		    //CV_BGR2HSV = 40;
		    Imgproc.cvtColor(in, hsv, 40);
		    Core.split(hsv, hsvSplit);
		    Imgproc.equalizeHist(hsvSplit.get(2), hsvSplit.get(2));
		    Core.merge(hsvSplit, hsv);
		    //CV_HSV2BGR = 54
		    Imgproc.cvtColor(hsv, out, 54);
		}else if(in.channels()==1){
		    Imgproc.equalizeHist(in, out);
		}
		
		return out;
	   }
	   
	   private Rect getCenterRect(Mat input, double size)
	   {
		double pomer = 280./65.;
		double rectWidth = size*pomer;
		double rectHeigh = this.eyeSize; //zmìna velikosti "oka"
		int inputCenterX = input.cols()/2;
		int inputCenterY = input.rows()/2;
		
		Rect centerRect=new Rect(inputCenterX-(int)rectWidth/2,inputCenterY-(int)rectHeigh/2,(int)rectWidth,(int)rectHeigh);
		
		return centerRect;
	   }
	
	   private ArrayList<StatusAreaAnalog> getAreaCandidates(Mat eyeRect)
	   {
			Mat img_gray = new Mat();
			img_gray = getYChannelFromYUV(eyeRect); //svìtlostní složka obrázku zaostøená
		
			Mat canny = new Mat();
			canny=otsuCanny(img_gray);
			
			ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			//zobrazení contourovaného eyeRect
			Imgproc.findContours(canny, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
			Mat black = Mat.ones(canny.size(), CvType.CV_8UC3);
			Imgproc.drawContours(black, contours, -1, new Scalar(255,0,0), 1);
			//opcvActivity.setImageEye(black);
			
			//pøidat vyseknutí snímaného obdélníku a ovìøit jestli jde o stavovou plochu
			ArrayList<StatusAreaAnalog> verifiedStatusAreas = new ArrayList<StatusAreaAnalog>();
			for (MatOfPoint itc : contours)
			{
			    //ovìø jestil jde o stavovou plochu (èíselník)
			    MatOfPoint2f itcConv = new MatOfPoint2f( itc.toArray() );
			    RotatedRect mr = Imgproc.minAreaRect(itcConv);
			    if(this.verifyStatusAreaSizes(mr))
			    {
				    
	   			Mat cropedCannyRect = this.cropRect(mr, canny);
	   			Mat resizedStatusAreaCanny = this.resizeStatusArea(cropedCannyRect);
	
	//   			//získat y channel originálu obrazu
	   			Mat cropedImgOrig = this.cropRect(mr, eyeRect);
	   			Mat resizedStatusAreaOrig = this.resizeStatusArea(cropedImgOrig);
	   			
	   			Mat cropedImgGray = this.cropRect(mr, img_gray);
	   			Mat resizedStatusAreaGray = this.resizeStatusArea(cropedImgGray);
	   			
	   			verifiedStatusAreas.add( new StatusAreaAnalog(resizedStatusAreaOrig, resizedStatusAreaGray, resizedStatusAreaCanny, mr.boundingRect(), mr) );
			    }
			}
		
			return verifiedStatusAreas;
	   }
	
	   //z detekovaného èíselníku separuj èíslice
	   private ArrayList<NumberSegment> segmentStatusAreaAndExtractNumbers(StatusArea statusArea)
	   {
			Mat _img = new Mat();
			double otsu_thresh_val = Imgproc.threshold( statusArea.imgYchannel.clone(), _img, 0, 255, Imgproc.THRESH_OTSU+Imgproc.THRESH_BINARY_INV );
			double high_thresh_val  = otsu_thresh_val*6/10; // 6/10
		        double lower_thresh_val = (otsu_thresh_val)*7/10; // 7/10
			Mat output = new Mat();
			Imgproc.Canny( statusArea.imgYchannel.clone(), output, lower_thresh_val, high_thresh_val );
	//    		showImage(output.clone(), frame.jPanel3);
			
			Mat hierarchi = new Mat();
			ArrayList<NumberSegment> numberImgs = new ArrayList<NumberSegment>();
			ArrayList<MatOfPoint> contours1 = new ArrayList<MatOfPoint>();
			ArrayList<MatOfPoint> okContours = new ArrayList<MatOfPoint>();
			Imgproc.findContours(output,
				contours1, // a vector of contours
				hierarchi,
				Imgproc.RETR_TREE,
				Imgproc.CHAIN_APPROX_NONE); 
			int hierarchiInt[] = new int[ (int) (hierarchi.total() * hierarchi.channels()) ]; 
			hierarchi.get(0, 0, hierarchiInt);
			
			ArrayList< MatOfPoint> hulls = new ArrayList<MatOfPoint>();
			
			
			for (int contourPos=0; contourPos<contours1.size(); contourPos++)
			{
			    MatOfPoint itc = contours1.get(contourPos);
			    //Create bounding rect of object
			    Rect mr = Imgproc.boundingRect(itc);
	//    		    Core.rectangle(result, mr.br(), mr.tl(), new Scalar(0,255,0));
	
			    //Crop image
			    Mat number = new Mat(statusArea.imgYchannel.clone(), mr);
			    //jedná se o obrázek èísla?
			    
			    if(this.verifyNumberSizes(number)) //kontrola velikosti obdélníku okolo èísla
			    {
	   			okContours.add(itc);
	   			
	   			if(this.allowAdd(itc, numberImgs))
	   			{
	   					Mat normalizeLinearStrNumber = new Mat();
	   					Core.normalize(number, normalizeLinearStrNumber, 0, 255, Core.NORM_MINMAX);
	   					Mat output1 = new Mat();
	   					Mat _img1 = new Mat();
	   					double otsu_thresh_val1 = Imgproc.threshold( normalizeLinearStrNumber, _img1, 0, 255, Imgproc.THRESH_OTSU+Imgproc.THRESH_BINARY );
	   					double high_thresh_val1  = otsu_thresh_val1*0.9;
	   					Imgproc.threshold(normalizeLinearStrNumber, output1, high_thresh_val1, 255, Imgproc.THRESH_BINARY_INV);
	   					Mat normalizeNumber = this.normalizeNumber(output1);
	
	   					numberImgs.add(new NumberSegment(normalizeNumber, mr, itc));
	
	   		//			showImage(normalizeNumber, frame.jPanel3);
	   		//    			Highgui.imwrite("data/test/"+(num++)+".jpg", number);
	   					
	   					this.saveEyeStatusAreaAndNumbers(this.eyeWithoutRect, this.eyeRect, statusArea.imgOrig, normalizeNumber,numberImgs.size(), null); //uloží eye, statusAreu(èíselník) a jeho èísla, když najde statusAreu, na které je detekováno alespoò jedno èíslo
	   			}
			    }
				Mat okContMat = new Mat();
				okContMat.create(statusArea.imgYchannel.size(), CvType.CV_8UC3);
				statusArea.imgYchannel.copyTo(okContMat);
	//    			okContMat.convertTo(okContMat, CvType.CV_8UC3);
		                Imgproc.drawContours(okContMat, okContours, -1, new Scalar(255,0,0), 2);
			}
			
			return numberImgs;
	   }
	   
	   private Mat sharpen(Mat input) {
		Mat sharpen = new Mat();
		Mat forBright = input.clone();
		Imgproc.GaussianBlur(forBright, sharpen, new Size(0, 0), 5);
		Core.addWeighted(forBright, 1.5, sharpen, -0.5, 0, sharpen);  //1.5, -0.5 //0,5,-0,1 DOBRÉ
		//zesvìtlení
	//	Mat brighterInput = new Mat();
	//	sharpen.convertTo(brighterInput, -1, alpha);
		
		return sharpen;
	   }
	
	   private Mat normalizeNumber(Mat in) {
			Core.bitwise_not(in, in);
			
			//Remap image
			int h=in.rows();
			int w=in.cols();
			Mat transformMat = Mat.eye(2,3,CvType.CV_32F);
			int m=Math.max(w,h);
			transformMat.put(0,2, m/2 - w/2);
			transformMat.put(1,2, m/2 - h/2);
		
			Mat warpImage = new Mat(m,m, in.type());
			warpImage.setTo(new Scalar(255,255,255));
	//    		Core.bitwise_not(warpImage, warpImage);
			Imgproc.warpAffine(in, warpImage, transformMat, warpImage.size(), Imgproc.INTER_LINEAR, Imgproc.BORDER_CONSTANT, new Scalar(0) );
			// end
			
			Mat out = new Mat();
			Imgproc.resize(in, out, new Size(this.numberWidth, this.numberHeight) );
			Core.bitwise_not(out, out);
			//opcvActivity.setImageEye(out);
	//    		Highgui.imwrite("data/test/"+(num++)+".jpg", out);
			return out;
	   }
	   
	   /////////////////////
	   private Mat otsuCanny(Mat input)
	   {
		Mat _img = new Mat();
		double otsu_thresh_val = Imgproc.threshold( input.clone(), _img, 0, 255, Imgproc.THRESH_OTSU+Imgproc.THRESH_BINARY_INV );
		double high_thresh_val  = otsu_thresh_val;
	       double lower_thresh_val = otsu_thresh_val * 0.5;
		Mat output = new Mat();
		Imgproc.Canny( input, output, lower_thresh_val, high_thresh_val );
		
		return output;
	   }
	   
	   private Mat likeAdaptiveCanny(Mat img)
	   {
		MatOfDouble mu = new MatOfDouble(), sigma = new MatOfDouble();
		Core.meanStdDev(img, mu, sigma);
	
		Mat edges = new Mat();
		Imgproc.Canny(img, edges, mu.get(0, 0)[0] - sigma.get(0, 0)[0], mu.get(0, 0)[0] + sigma.get(0, 0)[0]);
	
		return edges;
	   }
	   
	   private Mat getYChannelFromYUV(Mat in)
	   {
		Mat yuv = new Mat();
		Imgproc.cvtColor(in, yuv, Imgproc.COLOR_RGB2YUV);
		ArrayList<Mat> channels = new ArrayList<Mat>();
		Core.split( yuv, channels );
		
		return channels.get(0);
	   }
	
	   private ArrayList<StatusAreaAnalog> getDetectedStatusAreas(ArrayList<StatusAreaAnalog> statusAreaCandidates)
	   {
		//For each possible plate, classify with svm if it's a plate or no
		ArrayList<StatusAreaAnalog> areas = new ArrayList<StatusAreaAnalog>();
		for(int i=0; i< statusAreaCandidates.size(); i++)
		{
		    Mat img = statusAreaCandidates.get(i).imgYchannel;
		    Mat p = img.reshape(1, 1);
		    p.convertTo(p, CvType.CV_32FC1);
	//	    System.out.println(p.size());
		    int response = (int)this.svmClassifier.predict( p );
		    if(response==1) //rozpoznána spz
		    {
				areas.add(statusAreaCandidates.get(i));
	//    			showImage(posible_regions.get(i).plateImg, frame.jPanel2);
		    }
		}
		
		return areas;
	   }
	
	   private void initAI() {
	   	Log.i(TAG, "Init AI");
			net = new PerceptronNetwork();
			serializeNN = new SerializeNN();
			
			//normalizace numberImages pro trénování SVM
	//		this.normalizeFileNumberImages("data/test/numbersDobre", "data/test/numbersDobre/norm");
	//		this.normalizeFileNumberImages("data/test/numbersSpatne", "data/test/numbersSpatne/norm");
			
			//NN
			//load nn from ser file
			this.net = serializeNN.loadFromTxt("nnAnalog.ser");
			
			// --- learn NN from images data ---
//			this.createNNbyParamsAndLearn("numbersForTraining", null);
//			this.serializeNN.saveToTxt(net, "nnAnalog.ser"); //save nn - serialize
			// --- END learn NN from images data ---
			//END NN
			
			//SVM
			//area SVM
			//load svm from ser file
			this.svmClassifier = new CvSVM();
			this.svmClassifier.load(appDir+"svmAnalog.ser");
			
			//    //Set SVM params
//			CvSVMParams SVM_params = new CvSVMParams();
//			SVM_params.set_svm_type(CvSVM.C_SVC);
//			SVM_params.set_kernel_type(CvSVM.LINEAR);
//			SVM_params.set_degree(0);
//			SVM_params.set_gamma(1);
//			SVM_params.set_coef0(0);
//			SVM_params.set_C(1);
//			SVM_params.set_nu(0);
//			SVM_params.set_p(0);
//			SVM_params.set_term_crit(new TermCriteria(TermCriteria.MAX_ITER, 1000, 0.01));
			
	//		    //získej trénovací data svm pomocí vlastních obrázkù
	//		    TrainSVM trainSVM = new TrainSVM();
	//		    trainSVM.createDataSet(appDir+"EL_statusArea_k_uceni_svm/dobre", appDir+"EL_statusArea_k_uceni_svm/spatne");
	//	    //	    trainSVM.saveDataSet(); //save
	//		    //Train SVM
	//		    this.svmClassifier = new CvSVM(trainSVM.trainingData, trainSVM.classes, new Mat(), new Mat(), SVM_params);
	//		    this.svmClassifier.save(appDir+"svmAnalog.ser");
	//END SVM
	   }
	   
	   private boolean allowAdd(MatOfPoint numberContour, ArrayList<NumberSegment> numberImgs)
	   {
		boolean add = true;
		ArrayList<MatOfPoint> numberContourList = new ArrayList();
		numberContourList.add(numberContour);
		for(NumberSegment existNumber : numberImgs)
		{
		    ArrayList<MatOfPoint> existNumberContourList = new ArrayList();
		    existNumberContourList.add(existNumber.contour);
		    
		    Mat drawTestedNumber = Mat.zeros(65,280, CvType.CV_8UC1);
		    Imgproc.drawContours(drawTestedNumber, numberContourList, 0, new Scalar(255,255,255), -1);
		    Mat drawExistNumber = Mat.zeros(65,280, CvType.CV_8UC1);
		    Imgproc.drawContours(drawExistNumber, existNumberContourList, 0, new Scalar(255,255,255), -1);
		    Mat intersectImg = Mat.zeros(65,280, CvType.CV_8UC1);
		    Core.bitwise_and(drawTestedNumber, drawExistNumber, intersectImg);
		    if( Core.countNonZero(intersectImg) > 0 ) //poèet bílých piselù je vìtší než 0 - tzn. že se contoury dotýkají
			add = false;
	//	    Highgui.imwrite("data/test/tested.jpg", drawTestedNumber);
	//	    Highgui.imwrite("data/test/exist"+(num++)+".jpg", drawExistNumber);
	//	    Highgui.imwrite("data/test/intersectImg"+(num++)+".jpg", intersectImg);
		}
		
		return add;
	   }
}
