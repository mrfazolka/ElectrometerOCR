package my.opcv;

import java.util.ArrayList;

import my.nn.PerceptronNetwork;
import my.nn.SerializeNN;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvSVM;
import org.opencv.ml.CvSVMParams;

import android.util.Log;
import android.widget.Button;

public class DigitElProcessor extends ElektrometerImageMatProcessor{
	Mat adaptiveThr;

	Mat mask_mog2bg;
	
	double val1=1;
	double val2=1;
	double val3=1;
	double val4=1;
	
	DigitElProcessor(OpencvLoader opcvActivity) {
		super(opcvActivity);
	}

	@Override
	public Mat process(Mat input)
	{
		//proveď pouze jednou
		if(!this.alreadyDone)
		{
			this.initAI();
			this.alreadyDone = true;
			this.saveDir = "snimaniSaveDigit";
		}
		
		this.webcam_image = input;
 		
 		Imgproc.cvtColor(webcam_image, webcam_image, Imgproc.COLOR_RGBA2RGB); //konverze bez alfa kanalu, jinak nejde getrectsubpix
 		
 		double pomer = 280./65.;
  		double rectWidth = this.eyeSize*pomer;
  		double rectHeigh = this.eyeSize; //změna velikosti "oka"
  		int inputCenterX = webcam_image.cols()/2;
  		int inputCenterY = webcam_image.rows()/2;
//  		
  		Rect centerRect=new Rect(inputCenterX-(int)rectWidth/2,inputCenterY-(int)rectHeigh/2,(int)rectWidth,(int)rectHeigh);
//  		
//  		Mat input = webcam_image.clone();
  		//kreslí obdélnik - oko - nepotřebuji protože zobrazuji jen oko
//		    Core.rectangle(webcam_image, centerRect.tl(), centerRect.br(), new Scalar(255,0,0)); //draw rect in center of image
//		    
		this.eyeRect = new Mat(webcam_image, centerRect); //NÍŽE DO tohoto obrázku preslím
		    
		if(this.snimaniSaveSnimanyElektromerNum>0)
	    {
	    	this.saveElektromery = true;
	    	//nastavuj jen když ukládám obrázky elektroměrů
			this.eyeWithoutRect = this.eyeRect.clone(); //uchovávám jen pro ukládání eye (čistého výseku bez orámování kolem číselníku)
	    }
		
		ArrayList<StatusAreaDigit> statusAreaCandidates = this.getAreaCandidates(eyeRect); //všichni kandidáti na číselník
		ArrayList<StatusAreaDigit> statusAreaBySVM = getDetectedStatusAreas(statusAreaCandidates); //detekované spz klasifikované svm		
		
		for(StatusAreaDigit statusArea : statusAreaBySVM) //projdu statusArei, které byly klasifikovány SVM
	    {			
	    	//nakreslý okolo detekovaného číselníku obrys
	    	org.opencv.core.Point rect_points[] = new org.opencv.core.Point[4];
		    statusArea.rectBorder.points(rect_points);
		    for( int j = 0; j < 4; j++ )
		       Core.line(this.eyeRect, rect_points[j], rect_points[(j+1)%4], new Scalar(0,255,0), 1);
	    
		    if(this.doProcess) //zajistí rozpoznání pouze když uživatel klikne na tlačítko
	    	{
		    	this.doProcess = false;
				opcvActivity.enablePokracujBtt();
				
	    		opcvActivity.setImageStatusArea(this.resizeStatusArea(statusArea.imgOrig));
	    	
	// 			Highgui.imwrite("data/test/"+(num++)+".jpg", statusArea.imgYchannel);
				//extrahuj z číselníku obrázky čísel
				ArrayList<NumberSegment> extractedNumbers = this.segmentStatusAreaAndExtractNumbers(statusArea);
	//			
				//projdi všechny obrázky čísel na číselníku
				for(NumberSegment numberSegment : extractedNumbers)
				{
					//vytvořit a ulož data pro učení nn
				    //Highgui.imwrite(appDir+"cislaZmobilu/"+(num++)+".jpg", numberSegment.img);
				    
				    Character rozpoznaneCisloChar = this.getRozpoznaneCislo(numberSegment.img); //rozpoznej znak
				    
				    //ulož rozpoznané a pozici rozpoznaného
				    statusArea.recognizedNumbers.add(rozpoznaneCisloChar); //přidej rozpoznaný znak - znaky nejsou seřazeny
				    statusArea.numbersPosition.add(numberSegment.pos); //díky pos můžee následně správně poskládat čísla na číselníku jak jdou za sebou
				}
				statusArea.createOrderedImagesAndResult(extractedNumbers); //vytvoří seřazenou kolekci vyseklých čísel od prvního k poslednímu(z leva do prava)
				
				opcvActivity.setImagesNumbers(statusArea.orderedNumbersImgs); //zobraz obrázky seřazených vyseklých čísel číselníku
				
				if(statusArea.result.length()>4 && !"nerozpoznano".equals(statusArea.result))
		 		{
					this.result = statusArea.result;
					opcvActivity.setResultText(statusArea.result);
					//Core.putText(eyeRect, statusArea.result, new Point(30, 30), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0,0,200),2);
		 		}
				else if("nerozpoznano".equals(statusArea.result))
				{
					opcvActivity.setResultText("?");
				}
	    	}
	    }
	    
	    opcvActivity.setImageEye(eyeRect);
		
		return input;
	}
	
	private ArrayList<StatusAreaDigit> getAreaCandidates(Mat eyeRect)
	   {

		//použít pro nastavení vývojového prostředí s posuvníky
//		this.debugUI();
		
//		Mat orig = eyeRect.clone();
		
		Mat img_gray = new Mat();
		img_gray = getYChannelFromYUV(eyeRect);
		
		Mat black = this.segmentLinkedNumbers(img_gray);
		
		ArrayList<MatOfPoint> contoursBlack = new ArrayList<MatOfPoint>();
	    Imgproc.findContours(black, contoursBlack, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
	    //Mat black2 = Mat.ones(eyeRect.size(), CvType.CV_8UC3);
	    ArrayList<StatusAreaDigit> verifiedStatusAreas = new ArrayList<StatusAreaDigit>();
	    for (MatOfPoint itc : contoursBlack)
		{
	    	MatOfPoint2f itcConv = new MatOfPoint2f( itc.toArray() );
		    RotatedRect mr = Imgproc.minAreaRect(itcConv);
		    if(this.verifyStatusAreaSizes(mr)) //TODO: protáhnout mr kousek do prava nebo na obě strany ... abych měl všechny čísla v číselníku ... pro jistotu
		    {
//		    	//vykresli orámování okolo čísla
//						    	org.opencv.core.Point rect_points[] = new org.opencv.core.Point[4];
//							    mr.points(rect_points);
//							    for( int j = 0; j < 4; j++ )
//							       Core.line(this.eyeRect, rect_points[j], rect_points[(j+1)%4], new Scalar(255,255,255), 1);
//				//odkomentovat pro zobrazení detekovaných	    			    opcvActivity.setImageEye(eyeRect);
				// -- //

		    	if(mr.angle < -45. || mr.angle > 45.)
		    	{
		    		mr.size.height *= 1.06;
		    		mr.center.x *= 1.03;
		    	}
		    	else
		    	{
		    		mr.size.width *= 1.06;
		    		mr.center.y *= 1.03;
		    	}
		    	
		    	//crop statusArea(s)
		    	Mat cropedAdaptiveThrRect = this.cropRect(mr, this.adaptiveThr);
//	   			Mat resizedStatusAreaAdaptiveThr = this.resizeStatusArea(cropedAdaptiveThrRect);
//		    	opcvActivity.setImageStatusArea(this.resizeStatusArea(cropedAdaptiveThrRect));
		    	
	   			Mat cropedImgOrig = this.cropRect(mr, eyeRect);
//	   			Mat resizedStatusAreaOrig = this.resizeStatusArea(cropedImgOrig);
	   			
	   			Mat cropedImgGray = this.cropRect(mr, img_gray);
	   			Mat resizedStatusAreaGray = this.resizeStatusArea(cropedImgGray);
//	   			opcvActivity.setImageStatusArea(resizedStatusAreaGray);
	   			
	   			//pro učení svm
//	   			Highgui.imwrite(appDir+"EL_statusAreaDigit_k_uceni_svm/"+(this.num++)+".jpg", resizedStatusAreaGray);
	   			
	   			verifiedStatusAreas.add( new StatusAreaDigit(cropedImgOrig, resizedStatusAreaGray, cropedAdaptiveThrRect, mr.boundingRect(), mr) );
		    }
		}
		
	    return verifiedStatusAreas;
	   }
	
	private Mat segmentLinkedNumbers(Mat img_gray) {
		Mat blured = new Mat();
		//int v4 = (int)((int)val4%2==0 ? val4+1 : val4);
		Imgproc.GaussianBlur(img_gray, blured, new Size(13, 13), 0); //13, 13
		
//		Mat t = new Mat();
		this.adaptiveThr = new Mat();
		int v1 = (int)((int)val1%2==0 ? val1+1 : val1); //int val1 pro adapt thresh
		Imgproc.adaptiveThreshold(blured, this.adaptiveThr, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 59, 3); //(int)((int)val1%2==0 ? val1+1 : val1) - předposlední parametr .. ideál 59
		
		Size lk = new Size(val2,val3);
		Mat dilate = new Mat();
 	    Imgproc.dilate(adaptiveThr, dilate, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1,10)));
 	    ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	    Imgproc.findContours(dilate, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
//		Mat black = Mat.ones(dilate.size(), CvType.CV_8UC3);
//		Imgproc.drawContours(black, contours, -1, new Scalar(255,0,0), 1);
		//opcvActivity.setImageEye(black);
	    
		//přidat vyseknutí snímaného obdélníku a ověřit jestli jde o číslo
		Mat black = Mat.ones(dilate.size(), CvType.CV_8UC3);
		ArrayList<RotatedRect> verifiedNumberAreas = new ArrayList<RotatedRect>();
		//odkomentovat pro zobrazení detekovaných
//		Mat detected = eyeRect.clone();
//		Imgproc.drawContours(detected, contours, -1, new Scalar(255,255,255), -1);
		for (MatOfPoint itc : contours)
		{
		    //ověř jestil jde o stavovou plochu (číselník)
		    MatOfPoint2f itcConv = new MatOfPoint2f( itc.toArray() );
		    RotatedRect mr = Imgproc.minAreaRect(itcConv);
		    String numberType = this.verifyNumberAreaSizes(mr);
		    if(numberType!=null) //jde o číslo - jedna nebo jiné číslo
		    {
		    	//vykresli orámování okolo čísla
//		    			    	org.opencv.core.Point rect_points[] = new org.opencv.core.Point[4];
//		    				    mr.points(rect_points);
//		    				    for( int j = 0; j < 4; j++ )
//		    				       Core.line(detected, rect_points[j], rect_points[(j+1)%4], new Scalar(255,0,0), 1);
		    	//odkomentovat pro zobrazení detekovaných	    	
		    	// -- //
		    	
		    	//vykreslení contour do černého obrázku
//		    	Imgproc.drawContours(black, contours, contours.indexOf(itc), new Scalar(255,255,255), -1);

		    	//vykresly bounding rect
		    	//Rect br = Imgproc.boundingRect(itc);
		    	Rect br = mr.boundingRect();
		    	
		    	if(numberType.equals("numOne")) //posun obdélníku okolo jedničky do leva a roztažení do prava
		    	{
		    		br.width = (int)(br.width*3.1);
		    		br.x=(int)(br.x*0.93);
//		    		br.width = (int)(br.width*1);
		    	}
		    	else
		    	{
		    		br.width = (int)(br.width*1.5); //roztažení obdélníku okolo ostatních čísel do prava
		    	}
		    	Core.rectangle(black, br.br(), br.tl(), new Scalar(255,255,255), -1);
		    	
		    	verifiedNumberAreas.add(mr);
		    }
		}

		Imgproc.cvtColor(black, black, Imgproc.COLOR_RGB2GRAY);
		
		//odkomentovat pro zobrazení detekovaných
//		opcvActivity.setImageStatusArea(detected);
		
		return black;
	}

	private Mat getYChannelFromYUV(Mat in)
	   {
		Mat yuv = new Mat();
		Imgproc.cvtColor(in, yuv, Imgproc.COLOR_RGB2YUV);
		ArrayList<Mat> channels = new ArrayList<Mat>();
		Core.split( yuv, channels );
		
		return channels.get(0);
	   }
	
	private Mat cropRect(RotatedRect rr, Mat input)
	   {
	   	RotatedRect rect = rr.clone();
	   	// matrices we'll use
	       Mat M = new Mat(), rotated = new Mat(), cropped = new Mat();
	       // get angle and size from the bounding box
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

	//ověří velikost obdélníku kolem contoury
	   private boolean verifyStatusAreaSizes(RotatedRect rect)
	   {
			double error=0.65; //orig: 0,4     ;1		;0,65
			final double aspect=195/20; //200,50;      225,10;      195, 30
			
			final int minHeigh = 15; //orig 15
			final int maxHeigh = 90; //orig 125
			//zde počítám vlastně minimální a maximální obsahy - protože min výška*poměr=min šířka a minŠířka*minVýška = min obsah
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
	   
	   private String verifyNumberAreaSizes(RotatedRect rect)
	   {
		   double error=0.3; //orig: 0,4
			final double aspect=170./80.; //šířka x výška 160, 60               (100, 36; realita 46x84(ale musím zadat 84(170)/46(90))
			final double aspect1=250./40.; //šířka x výška 160, 60 (170/30)
			
			final int minHeigh = 10; //orig 15
			final int maxHeigh = 55;//(int)val3;//350; //orig 125 //
			double min = minHeigh*aspect*minHeigh; // minimum area
			double max = maxHeigh*aspect*maxHeigh; // maximum area
			double min1 = minHeigh*aspect1*minHeigh; // minimum area
			double max1 = maxHeigh*aspect1*maxHeigh; // maximum area
			
			double rmin= aspect-aspect*error;
			double rmax= aspect+aspect*error;
			double rmin1= aspect1-aspect1*error;
			double rmax1= aspect1+aspect1*error;
			
			//obsah a poměr obdélníku, u kterého zjišťuji jestli vyhovuje nebo ne
			double area= rect.size.height * rect.size.width;
			double r = (double)rect.size.width / (double)rect.size.height;
			
			if(r<1)
			{
			    r = 1/r;
			}
			
			Rect rc = rect.boundingRect();
			if(rc.width>rc.height || rc.height>135) //akceptuj jen vyšší než širší a max výška je o kousek menší, než výška eyeRect
				return null;
			
			if(!((area<min || area>max) || (r<rmin || r>rmax))) //jedná se o číslo - ne jedničku
				return "numOrd";
			else if(!((area<min1 || area>max1) || (r<rmin1 || r>rmax1))) //jedná se o jedničku
				return "numOne";
			else
				return null; //nejedná se o číslo
	   }
	   
	   private ArrayList<StatusAreaDigit> getDetectedStatusAreas(ArrayList<StatusAreaDigit> statusAreaCandidates)
	   {
		ArrayList<StatusAreaDigit> areas = new ArrayList<StatusAreaDigit>();
		for(int i=0; i< statusAreaCandidates.size(); i++)
		{
		    Mat img = statusAreaCandidates.get(i).imgYchannel;
		    Mat p = img.reshape(1, 1);
		    p.convertTo(p, CvType.CV_32FC1);
		    int response = (int)this.svmClassifier.predict( p );
		    if(response==1) //rozpoznána spz
		    {
				areas.add(statusAreaCandidates.get(i));
//				this.opcvActivity.setImageEye(statusAreaCandidates.get(i).adaptiveThresh);
		    }
		}
		
		return areas;
	   }
	   
	   private ArrayList<NumberSegment> segmentStatusAreaAndExtractNumbers(StatusAreaDigit statusArea)
	   {
		   Mat e = new Mat();
		   Imgproc.dilate(statusArea.imgAdaptiveThresh, e, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(4,13)));
//		   opcvActivity.setImageStatusArea(this.resizeStatusArea(e));
			Mat hierarchi = new Mat();
			ArrayList<NumberSegment> numberImgs = new ArrayList<NumberSegment>();
			ArrayList<MatOfPoint> contours1 = new ArrayList<MatOfPoint>();
			ArrayList<MatOfPoint> okContours = new ArrayList<MatOfPoint>();
			Imgproc.findContours(e,
				contours1,
				hierarchi,
				Imgproc.RETR_EXTERNAL , 
				Imgproc.CHAIN_APPROX_SIMPLE);
//			//zobrazení čtverců okolo čísel
//			Mat c = statusArea.imgOrig.clone();
			for (int contourPos=0; contourPos<contours1.size(); contourPos++)
			{
			    MatOfPoint itc = contours1.get(contourPos);
			    //Create bounding rect of object
			    Rect mr = Imgproc.boundingRect(itc);
			    	//zobrazení čtverců okolo čísel
//	    		    Core.rectangle(c, mr.br(), mr.tl(), new Scalar(0,255,0), 1);
	
			    //Crop image
			    Mat numberThr = new Mat(statusArea.imgAdaptiveThresh, mr); // -||-
			    
			    if(this.verifyNumberAreaSizesByMat(numberThr)) //kontrola velikosti obdélníku okolo čísla
			    {
		   			okContours.add(itc);
	   				//zobrazení čtverců okolo čísel
//	   				Core.rectangle(c, mr.br(), mr.tl(), new Scalar(0,255,0), 1);
		   			
	   				if(this.allowAdd(itc, numberImgs))
		   			{
		   					Mat normalizeNumber = this.normalizeNumber(numberThr);
		   					Core.bitwise_not(normalizeNumber, normalizeNumber);
		
		   					numberImgs.add(new NumberSegment(normalizeNumber, mr, itc));
		
		   		//			showImage(normalizeNumber, frame.jPanel3);
		   		//    			Highgui.imwrite("data/test/"+(num++)+".jpg", number);

		   					this.saveEyeStatusAreaAndNumbers(this.eyeWithoutRect, this.eyeRect, statusArea.imgOrig, normalizeNumber,numberImgs.size(), null); //uloží eye, statusAreu(číselník) a jeho čísla, když najde statusAreu, na které je detekováno alespoň jedno číslo
		   			}
			    }
			}
			//zobrazení čtverců okolo čísel
//		    opcvActivity.setImageStatusArea(c);
			return numberImgs;
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
	   
	   private boolean verifyNumberAreaSizesByMat(Mat rect)
	   {
		   double error=0.4; //orig: 0,4
			final double aspect=170./80.; //šířka x výška 160, 60               (100, 36; realita 46x84(ale musím zadat 84(170)/46(90))
			final double aspect1=135./40.;
			
			final int minHeigh = 20; //orig 15
			final int maxHeigh = 55;//(int)val3;//350; //orig 125 //
			double min = minHeigh*aspect*minHeigh; // minimum area
			double max = maxHeigh*aspect*maxHeigh; // maximum area
			double min1 = minHeigh*aspect1*minHeigh; // minimum area
			double max1 = maxHeigh*aspect1*maxHeigh; // maximum area
			
			double rmin= aspect-aspect*error;
			double rmax= aspect+aspect*error;
			double rmin1= aspect1-aspect1*error;
			double rmax1= aspect1+aspect1*error;
			
			double area= rect.rows() * rect.cols();
			double r = (double)rect.cols() / (double)rect.rows();
			
			if(r<1)
			{
			    r = 1/r;
			}
			
			if(!((area<min || area>max) || (r<rmin || r>rmax))) //jedná se o číslo - ne jedničku
				return true;
			else if(!((area<min1 || area>max1) || (r<rmin1 || r>rmax1))) //jedná se o jedničku
				return true;
			else
				return false; //nejedná se o číslo
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
		    if( Core.countNonZero(intersectImg) > 0 ) //počet bílých piselů je větší než 0 - tzn. že se contoury dotýkají
			add = false;
	//	    Highgui.imwrite("data/test/tested.jpg", drawTestedNumber);
	//	    Highgui.imwrite("data/test/exist"+(num++)+".jpg", drawExistNumber);
	//	    Highgui.imwrite("data/test/intersectImg"+(num++)+".jpg", intersectImg);
		}
		
		return add;
	   }
	   	   
	   
	   
	   
	   
	   
	   
	   
	   private void initAI() {
		   	Log.i(TAG, "Init AI");
				net = new PerceptronNetwork();
				serializeNN = new SerializeNN();
				
				//NN
				//load nn from ser file
				this.net = serializeNN.loadFromTxt("nnDigit.ser");
				
				// --- learn NN from images data ---
				//vytvoří dataSet pro učení 
//				this.createNNbyParamsAndLearn("numbersForTraining", null); //TODO: params
//				this.serializeNN.saveToTxt(net, "nnDigit.ser"); //save nn - serialize
				// --- END learn NN from images data ---
				//END NN
				
		//SVM
				//area SVM
				//load svm from ser file
				this.svmClassifier = new CvSVM();
				this.svmClassifier.load(appDir+"svmDigit.ser");
				
				//    //Set SVM params
//				CvSVMParams SVM_params = new CvSVMParams();
//				SVM_params.set_svm_type(CvSVM.C_SVC);
//				SVM_params.set_kernel_type(CvSVM.LINEAR);
//				SVM_params.set_degree(0);
//				SVM_params.set_gamma(1);
//				SVM_params.set_coef0(0);
//				SVM_params.set_C(1);
//				SVM_params.set_nu(0);
//				SVM_params.set_p(0);
//				SVM_params.set_term_crit(new TermCriteria(TermCriteria.MAX_ITER, 1000, 0.01));
////				
////				    //získej trénovací data svm pomocí vlastních obrázků
//				    TrainSVM trainSVM = new TrainSVM();
//				    trainSVM.createDataSet(appDir+"EL_statusAreaDigit_k_uceni_svm/dobre", appDir+"EL_statusAreaDigit_k_uceni_svm/spatne");
////			    //	    trainSVM.saveDataSet(); //save
////				    //Train SVM
//				    this.svmClassifier = new CvSVM(trainSVM.trainingData, trainSVM.classes, new Mat(), new Mat(), SVM_params);
//				    this.svmClassifier.save(appDir+"svmDigit.ser");
		//END SVM
		   }
	   
//	   private void debugUI()
//		{
//			SeekBar seek1 = (SeekBar) this.opcvActivity.findViewById(R.id.seekBar1);
//			val1 = seek1.getProgress();
//			SeekBar seek2 = (SeekBar) this.opcvActivity.findViewById(R.id.seekBar2);
//			val2 = seek2.getProgress();
//			SeekBar seek3 = (SeekBar) this.opcvActivity.findViewById(R.id.seekBar3);
//			val3 = seek3.getProgress();
//			SeekBar seek4 = (SeekBar) this.opcvActivity.findViewById(R.id.seekBar4);
//			val4 = seek4.getProgress();
//			val1 = val1 < 3 ? 3 : val1;
//			val2 = val2 < 1 ? 1 : val2;
//			val3 = val3 < 1 ? 1 : val3;
//			val4 = val4 < 1 ? 1 : val4;
//			
//			   opcvActivity.runOnUiThread(new Runnable() {
//			      @Override
//			      public void run() {
//			    	TextView t1 = (TextView) DigitElProcessor.this.opcvActivity.findViewById(R.id.textView1);
//					t1.setText("val1: "+val1);
//					TextView t2 = (TextView) DigitElProcessor.this.opcvActivity.findViewById(R.id.textView2);
//					t2.setText("val2: "+val2);
//					TextView t3 = (TextView) DigitElProcessor.this.opcvActivity.findViewById(R.id.textView3);
//					t3.setText("val3: "+val3);
//					TextView t4 = (TextView) DigitElProcessor.this.opcvActivity.findViewById(R.id.textView4);
//					t4.setText("val4: "+val4);
//			      }
//			  });
//			
//		}
	   
//	   private void debugUI()
//		{
//			SeekBar seek1 = (SeekBar) this.opcvActivity.findViewById(R.id.seekBar1);
//			val1 = seek1.getProgress();
//			SeekBar seek2 = (SeekBar) this.opcvActivity.findViewById(R.id.seekBar2);
//			val2 = seek2.getProgress();
//			val1 = val1 < 3 ? 3 : val1;
//			val2 = val2 < 1 ? 1 : val2;
//			
//			   opcvActivity.runOnUiThread(new Runnable() {
//			      @Override
//			      public void run() {
//			    	TextView t1 = (TextView) DigitElProcessor.this.opcvActivity.findViewById(R.id.textView1);
//					t1.setText("val1: "+val1);
//					TextView t2 = (TextView) DigitElProcessor.this.opcvActivity.findViewById(R.id.textView2);
//					t2.setText("val2: "+val2);
//			      }
//			  });
//			
//		}
}
