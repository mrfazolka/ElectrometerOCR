package my.nn;

import java.io.PrintStream;

public class Sample {
    private double[] inputs;
    private double[] outputs;
    
    public Sample(double[] inputs, double[] outputs){
        this.inputs = new double[inputs.length];
        this.outputs = new double[outputs.length];
        
        System.arraycopy(inputs, 0, this.inputs, 0, inputs.length);
        System.arraycopy(outputs, 0, this.outputs, 0, outputs.length);
    }
    
    Sample(int inputs, int outputs){
        this.inputs = new double[inputs];
        this.outputs = new double[outputs];
    }
    
    public double[] getInputs(){
        return inputs;
    }
    
    public double[] getOutputs(){
        return outputs;
    }
    
    public void print(PrintStream out){
        
    }
    
    public void setInputs(double[] inputs){
        System.arraycopy(inputs, 0, this.inputs, 0, inputs.length);
    }
    
    public void setOutputs(double[] outputs){
        System.arraycopy(outputs, 0, this.outputs, 0, outputs.length);
    }
}
