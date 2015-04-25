package my.nn;

import java.io.PrintStream;
import java.io.Serializable;

public class InputLayer extends Layer implements Serializable{
    
    public InputLayer(int size){
        super (size);
        for(int i = 0; i < size; i++){
            neurons[i] = new InputNeuron();
        }
    }
    
    public void setInputs(double[] inputs){
        //pro vsechny inputy priradit hodnotu
        for(int i = 0; i < inputs.length; i++){
            neurons[i].y = inputs[i];
        }
    }

    @Override
    public void recall() {
    }

    @Override
    public void learn() {
        
    }

    public void print(PrintStream out) {
    }

    @Override
    public void print(PrintStream out, String layerType) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
