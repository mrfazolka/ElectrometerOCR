package my.opcv;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.opencv.core.Mat;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MatSerializerJson {
	public static String matToJson(Mat mat){        
	    JsonObject obj = new JsonObject();

	    if(mat.isContinuous()){
	        int cols = mat.cols();
	        int rows = mat.rows();
	        int elemSize = (int) mat.elemSize();    

	        byte[] data = new byte[cols * rows * elemSize];

	        mat.get(0, 0, data);
	        int a = mat.type();
	        obj.addProperty("rows", mat.rows()); 
	        obj.addProperty("cols", mat.cols()); 
	        obj.addProperty("type", mat.type());

	        // We cannot set binary data to a json object, so:
	        // Encoding data byte array to Base64.
	        String dataString = new String(Base64.encode(data, Base64.DEFAULT));

	        obj.addProperty("data", dataString);            

	        Gson gson = new Gson();
	        String json = gson.toJson(obj);
	        
	        return json;
	    } else {
	        Log.e("MatSerializer::Class", "Mat not continuous.");
	    }
	    return "{}";
	}

	public static Mat matFromJson(String json){
	    JsonParser parser = new JsonParser();
	    JsonObject JsonObject = parser.parse(json).getAsJsonObject();

	    int rows = JsonObject.get("rows").getAsInt();
	    int cols = JsonObject.get("cols").getAsInt();
	    int type = JsonObject.get("type").getAsInt();

	    String dataString = JsonObject.get("data").getAsString();       
	    byte[] data = Base64.decode(dataString.getBytes(), Base64.DEFAULT); 

	    Mat mat = new Mat(rows, cols, type);
	    mat.put(0, 0, data);

	    return mat;
	}
	
	public static void saveMatAsJson(Mat m, String path)
	{
		String jsonStr = MatSerializerJson.matToJson(m);
		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(path)));
			bos.write(jsonStr.getBytes("UTF8"));
			bos.flush();
			bos.close();
			
			
//			FileWriter fw = new FileWriter(path);
//			BufferedWriter bw = new BufferedWriter(fw);
//			bw.write(jsonStr);
//			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Mat loadMatFromJsonFile(String path)
	{
		try {
			return MatSerializerJson.matFromJson(getStringFromFile(path));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line).append("\n");
	    }
	    reader.close();
	    return sb.toString();
	}

	public static String getStringFromFile (String filePath) throws Exception {
	    File fl = new File(filePath);
	    FileInputStream fin = new FileInputStream(fl);
	    String ret = convertStreamToString(fin);
	    fin.close();        
	    return ret;
	}
}
