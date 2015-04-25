package my.nn;

import java.io.Serializable;
import java.util.ArrayList;

public class Network implements Serializable{

    protected ArrayList<Layer> layers = new ArrayList<Layer>();

    public void addLayer(Layer layer) {
        layers.add(layer);
    }

    public double[] recall(double[] input) {
        ((InputLayer) layers.get(0)).setInputs(input);
        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).recall();
        }
        return layers.get(layers.size() - 1).getOutputs();
    }
    
    public Layer outputLayer(){
        return layers.get(layers.size() - 1);
    
    }
}
