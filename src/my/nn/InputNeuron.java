package my.nn;

import java.io.Serializable;

public class InputNeuron extends Neuron implements Serializable{

    public InputNeuron(){
        
    }
    
    public void set(double y){
        this.y = y;
    }
    
    @Override
    public void recall() {
        
    }

    @Override
    public void learn() {
        
    }
    
}
