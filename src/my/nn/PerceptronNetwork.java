package my.nn;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Random;

public class PerceptronNetwork extends Network implements Serializable{    

    //elimininovat etu na urovni site
    //vytisknout graf 101x101 a bodiky odpovidaji <0,1>, barva odpovida y, nakreslit dělící linii mezi neurony jinou barvou dle vah a prahů w11 * x1 + w12*x2 + vlnka  = 0 x2 = -w11 * x1 - vlnka / w12 najit pruseciky na hranach
    /* 1 iterace */
    public PerceptronNetwork() {
        
    }

    public double learn(double[] inputs, double[] outputs, double eta) {
        double se = 0;
        recall(inputs);
        se = ((PerceptronLayer) outputLayer()).calcOutputDelta(outputs);
        for (int i = layers.size() - 1; i > 1; i--) {
            ((PerceptronLayer) layers.get(i - 1)).propagateDelta((PerceptronLayer) layers.get(i));
            ((PerceptronLayer) layers.get(i)).calculateDelta();
        }
        ((PerceptronLayer) layers.get(1)).calculateDelta();
        //print(System.out);
        //System.out.println();
        return se;
    }

    /* 1 epocha */
    public double learnEpoch(DataSet trainingSet, double eta, double alpha) {
        double mse = 0;

        //initEpoch for Network
        for (int i = 1; i < layers.size(); i++) {
            ((PerceptronLayer) layers.get(i)).initEpoch();
        }
        for (int i = 0; i < trainingSet.size(); i++) {
            Sample s = trainingSet.get(i);
            mse += learn(s.getInputs(), s.getOutputs(), eta);
            //print(System.out);
            //System.out.println();
        }
        weightUpdate(eta, alpha);
        //System.out.println();
        //System.out.print("Weight update: ");
        //print(System.out);
        //drawLines();
        //System.out.printf("\n% .15f : mse", mse / trainingSet.size());
        //System.out.println();

        return mse / trainingSet.size();
    }

    public void learn(DataSet trainingSet, double minError, int maxEpoch, double minWeight, double maxWeight, Random rnd, double eta, double alpha) {
        double err;
        initWeight(minWeight, maxWeight, rnd);
        for (int i = 0; i < maxEpoch; i++) {
            err = this.learnEpoch(trainingSet, eta, alpha);
//            System.out.println(err);
            if (err < minError) {
                break;
            }

        }
    }

    public void weightUpdate(double eta, double alpha) {
        for (int i = layers.size() - 1; i > 0; i--) {
            ((PerceptronLayer) layers.get(i)).weightUpdate(eta, alpha);
        }
    }

    public void initWeight(double min, double max, Random rnd) {
        for (int i = 1; i < layers.size(); i++) {
            ((PerceptronLayer) layers.get(i)).init(min, max, rnd);
        }
    }

    public void print(PrintStream out) {
        ((PerceptronLayer) layers.get(layers.size() - 1)).print(out, "output");
        for (int i = layers.size() - 2; i > 0; i--) {
            ((PerceptronLayer) layers.get(i)).print(out, "hidden");
        }
    }

    public void print() {
       
    }    
}
