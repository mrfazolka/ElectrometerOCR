package my.nn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import my.opcv.ElektrometerImageMatProcessor;
import android.util.Log;

/**
 *
 * @author Ondøej Hanzlík
 */
public class SerializeNN {
    //serializuje do txt
    public void saveToTxt(PerceptronNetwork nn, String nnFileName)
    {
	try
	{
	   FileOutputStream fileOut = new FileOutputStream(ElektrometerImageMatProcessor.appDir+nnFileName);
	   ObjectOutputStream out = new ObjectOutputStream(fileOut);
	   out.writeObject(nn);
	   out.close();
	   fileOut.close();
	   System.out.printf("Serialized data is saved in 'data/nn.ser'");
	}catch(IOException i)
	{
	    i.printStackTrace();
	}
    }
    
    //deserializuje z txt
    public PerceptronNetwork loadFromTxt(String nnFileName)
    {
    	PerceptronNetwork nn = null;
    	
    	try
    	{
    	   File nnSerFile = new File(ElektrometerImageMatProcessor.appDir+nnFileName);
    	   
    	   Log.i("SerializeNN", "File nn.ser exists!");
    		   
    	   FileInputStream fileIn = new FileInputStream(nnSerFile);
    	   ObjectInputStream in = new ObjectInputStream(fileIn);
    	   nn = (PerceptronNetwork) in.readObject();
    	   in.close();
    	   fileIn.close();
    	}catch(IOException i)
    	{
    	   Log.i("SerializeNN", "File nn.ser doesn't exists!");
    	   i.printStackTrace();
    	   return null;
    	}catch(ClassNotFoundException c)
    	{
    	   System.out.println("PerceptronNetwork class not found");
    	   c.printStackTrace();
    	   return null;
    	}
    	
    	return nn;
    }
}
