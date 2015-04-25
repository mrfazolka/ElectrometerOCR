package my.nn;

import java.io.PrintStream;
import java.io.Serializable;


public abstract class Layer implements Serializable{
    
    public Neuron[] neurons;
    
    public Layer(int size){
        neurons = new Neuron[size];
    }
    
    public Layer(){}
    
    public abstract void recall(); // ziskani vystupu
    public abstract void learn(); // uceni
    public double[] getOutputs(){
        double[] outputs = new double[neurons.length];
        for(int i = 0; i < neurons.length; i++){
            outputs[i] = neurons[i].y;
        }
        return outputs;
    }
    
    public abstract void print(PrintStream out, String layerType);
    
}
