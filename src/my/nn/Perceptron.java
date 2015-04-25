package my.nn;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Random;

public class Perceptron extends Neuron implements Serializable{

    public double[] w; //vektor vah
    public double theta; //prah
    
    public double delta; // chyba připadající a jeden neuron
    public double[] deltaW; // požadovaná váhy
    public double deltaTheta; // požadovaná změna prahu
    public double oldDeltaTheta = 0; // požadovaná stara změna prahu
    public double[] oldDeltaW; //předchozí hodnota deltaW pro účel setrvačnosti
    
    
    public Perceptron(){}
    
    public Perceptron(int numberOfInputs){
        w = new double[numberOfInputs];
        deltaW = new double[numberOfInputs];
        oldDeltaW = new double[numberOfInputs];
    }
    
    public void init(double min, double max, Random rnd){
        
        for(int i = 0; i<w.length; i++){
            w[i] = rnd.nextDouble()*(max - min) + min;
        }
        theta  = rnd.nextDouble()*(max - min) + min;
    }
    
    @Override
    public void recall() {
        double phi = theta;
        
        for(int i=0; i < inputs.neurons.length; i++){
            phi += w[i]*inputs.neurons[i].y;
        }
        y = 1/(1+Math.exp(-phi));
    }

    @Override
    public void learn() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void connect(Layer inputs){
        this.inputs = inputs;
        w = new double[inputs.neurons.length];
        deltaW = new double[inputs.neurons.length];
        oldDeltaW = new double[inputs.neurons.length];
    }
    
    public double calcOutputDelta(double d){ //ok
        double diff = (d - y);
        delta = y * (1 - y) * diff;
        return diff * diff;
    }
    
    public void initEpoch(){
        for(int j = 0; j <deltaW.length; j++){
            deltaW[j] = 0;
        }
        deltaTheta = 0;
    }
    
    public void calcDelta(){ // pouzivame
        for(int j = 0; j <deltaW.length; j++){
            deltaW[j] += /*eta **/ delta * inputs.neurons[j].y;
             
            //System.out.println(deltaW[j]);
        }
        
        deltaTheta += /*eta * */ delta;
    }
    
    public void propagateDelta(PerceptronLayer fromLayer, int j){
        double diff = delta - y;
        delta = 0;
        for(int i = 0; i < fromLayer.neurons.length; i++){
            delta += ((Perceptron)fromLayer.neurons[i]).delta * ((Perceptron)fromLayer.neurons[i]).w[j];
        }
        delta = y * (1 - y) * delta;
    }
    
    public void print (PrintStream out){
        super.print(out);
//        out.printf("\n% .15f : threshold", theta);
        for(int i = 0; i < inputs.neurons.length; i++){
//            out.printf("\n% .15f : w[%d] ", w[i], i);
        }
//        out.printf("\n% .15f : delta", delta);
//        out.printf("\n% .15f : deltaThreshold", deltaTheta);
        for(int i = 0; i < inputs.neurons.length; i++){
//            out.printf("\n% .15f : deltaW[%d]", deltaW[i], i);
        }
    }

    void weightUpdate(double eta, double alpha) {
        for(int i = 0; i < w.length; i++){
            double dW = eta * deltaW[i] + alpha * oldDeltaW[i];
            w[i] += dW;
            oldDeltaW[i] = dW;
        }
        
        theta += eta * deltaTheta + alpha * oldDeltaTheta;
        oldDeltaTheta = theta;
        //theta = eta * theta + alpha * deltaTheta;
        
    }
}
