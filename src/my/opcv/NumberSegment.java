/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.opcv;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;

/**
 *
 * @author Ondøej Hanzlík
 */
public class NumberSegment {
    public Mat img;
//    public Mat imgProccessed;
    public MatOfPoint contour;
    public Rect pos;
    NumberSegment(){};
    public NumberSegment(Mat i, Rect p, MatOfPoint contour){
		img=i;
		pos=p;
		this.contour = contour;
    }
}