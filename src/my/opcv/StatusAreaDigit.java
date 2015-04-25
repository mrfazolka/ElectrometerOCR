package my.opcv;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;

public class StatusAreaDigit extends StatusArea {
	
	public Mat imgAdaptiveThresh;
	
	public StatusAreaDigit(Mat imgOrig, Mat imgYchannel, Mat adaptiveThresh, Rect position, RotatedRect rectBorder)
	   {
		this.imgOrig = imgOrig;
		this.imgYchannel = imgYchannel;
		this.imgAdaptiveThresh = adaptiveThresh;
		this.position = position;
		this.rectBorder = rectBorder;
	   }
}
