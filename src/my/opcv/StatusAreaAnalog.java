package my.opcv;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;

public class StatusAreaAnalog extends StatusArea {
	public Mat imgCanny;
	
	public StatusAreaAnalog(Mat imgOrig, Mat imgYchannel, Mat imgCanny, Rect position, RotatedRect rectBorder)
	  {
		this.imgOrig = imgOrig;
		this.imgYchannel = imgYchannel;
		this.imgCanny = imgCanny;
		this.position = position;
		this.rectBorder = rectBorder;
	  }
}
