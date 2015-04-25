package my.nn;

import java.io.PrintStream;
import java.io.Serializable;



public abstract class Neuron implements Serializable{
   
    public double y;
    protected Layer inputs;
    
    public abstract void recall();  //vybaveni y
    
    public abstract void learn();   // uceni
    
    public void connect(Layer inputs){
        this.inputs = inputs;
    }
    
    public void print(PrintStream out){
        out.printf("\n% .15f : y",y);
    }
    
}
