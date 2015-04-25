package my.opcv;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;

public abstract class StatusArea {
    public Rect position;
    public Mat imgYchannel;
    public Mat imgOrig;
    public RotatedRect rectBorder;
    
    public ArrayList<Character> recognizedNumbers = new ArrayList<Character>();
    public ArrayList<Rect> numbersPosition = new ArrayList<Rect>();
    public ArrayList<NumberSegment> orderedNumbersImgs = new ArrayList<NumberSegment>();
    public String result;
    
   
   public String getRozpoznanaCisla() {
	String result="";
	//Order numbers
	ArrayList<Integer> orderIndex = new ArrayList();
	ArrayList<Integer> xpositions = new ArrayList();

	for(int i=0; i< numbersPosition.size(); i++){
	    orderIndex.add(i);
	    xpositions.add(numbersPosition.get(i).x);
	}

	if(xpositions.size()>0)
	{
	    float min=xpositions.get(0);
	    int minIdx=0;
	    for(int i=0; i< xpositions.size(); i++){
		min=xpositions.get(i);
		minIdx=i;
		for(int j=i; j<xpositions.size(); j++){
		    if(xpositions.get(j)<min){
			min=xpositions.get(j);
			minIdx=j;
		    }
		}
		int aux_i=orderIndex.get(i);
		int aux_min=orderIndex.get(minIdx);
		//orderIndex[i]=aux_min;
		orderIndex.set(i, aux_min);
		orderIndex.set(minIdx, aux_i);

		int aux_xi=xpositions.get(i);
		int aux_xmin=xpositions.get(minIdx);
	//        xpositions[i]=aux_xmin;
		xpositions.set(i, aux_xmin);
		xpositions.set(minIdx, aux_xi);
	    }
	    for(int i=0; i<orderIndex.size(); i++){
		result=result+recognizedNumbers.get(orderIndex.get(i));
	    }
//	    System.out.println(result);
	}
	else
	{
	    result = "nerozpoznano";
	}
	
	return result;
   }
   
   public void createOrderedImagesAndResult(ArrayList<NumberSegment> unsortedNumbersImgs)
   {
	   //Order numbers
		ArrayList<Integer> orderIndex = new ArrayList<Integer>();
		ArrayList<Integer> xpositions = new ArrayList<Integer>();
		ArrayList<NumberSegment> orderedNumbersImgs = new ArrayList<NumberSegment>();
		String result="";

		for(int i=0; i< numbersPosition.size(); i++){
		    orderIndex.add(i);
		    xpositions.add(numbersPosition.get(i).x);
		}

		if(xpositions.size()>0)
		{
		    float min=xpositions.get(0);
		    int minIdx=0;
		    for(int i=0; i< xpositions.size(); i++)
		    {
				min=xpositions.get(i);
				minIdx=i;
				for(int j=i; j<xpositions.size(); j++){
				    if(xpositions.get(j)<min){
					min=xpositions.get(j);
					minIdx=j;
				    }
				}
				int aux_i=orderIndex.get(i);
				int aux_min=orderIndex.get(minIdx);
				//orderIndex[i]=aux_min;
				orderIndex.set(i, aux_min);
				orderIndex.set(minIdx, aux_i);
	
				int aux_xi=xpositions.get(i);
				int aux_xmin=xpositions.get(minIdx);
			//        xpositions[i]=aux_xmin;
				xpositions.set(i, aux_xmin);
				xpositions.set(minIdx, aux_xi);
		    }
		    
		    for(int i=0; i<orderIndex.size(); i++)
		    {
		    	orderedNumbersImgs.add(unsortedNumbersImgs.get(orderIndex.get(i)));
		    	result=result+recognizedNumbers.get(orderIndex.get(i));
		    }
		    this.result=result;
		    this.orderedNumbersImgs = orderedNumbersImgs;
		}
		else
		{
			this.orderedNumbersImgs = new ArrayList<NumberSegment>(); //žádné znaky v èíselníku
			this.result = "nerozpoznano";
		}
   }
}
