package my.nn;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Random;

public class PerceptronLayer extends Layer implements Serializable{

    public PerceptronLayer(int size) {
        super(size);
        for (int i = 0; i < size; i++) {
            neurons[i] = new Perceptron();
        }
    }

    public void init(double min, double max, Random rnd) {
        for (int i = 0; i < neurons.length; i++) {
            ((Perceptron) neurons[i]).init(min, max, rnd);
        }
    }

    @Override
    public void recall() {
        for (Neuron neuron : neurons) {
            ((Perceptron) neuron).recall();
        }
    }

    @Override
    public void learn() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void connect(Layer inputs) {
        for (int i = 0; i < neurons.length; i++) {
            neurons[i].connect(inputs);
        }
    }

    public double calcOutputDelta(double[] d) {
        double outputDelta = 0;
        for (int i = 0; i < neurons.length; i++) {
            outputDelta += ((Perceptron) neurons[i]).calcOutputDelta(d[i]);
        }
        return outputDelta / neurons.length;

    }

    public void initEpoch() {
        for (Neuron neuron : neurons) {
            ((Perceptron) neuron).initEpoch();
        }
    }

    public void calculateDelta() {
        for (Neuron neuron : neurons) {
            ((Perceptron) neuron).calcDelta();
        }
    }

    public void propagateDelta(PerceptronLayer fromLayer) {
        for (int j = 0; j < neurons.length; j++) {
            ((Perceptron) neurons[j]).propagateDelta(fromLayer, j);
        }
    }

    void weightUpdate(double eta, double alpha) {
        for (int j = 0; j < neurons.length; j++) {
            ((Perceptron) neurons[j]).weightUpdate(eta, alpha);
        }
    }

    @Override
    public void print(PrintStream out, String layerType) {
        if (neurons.length == 1) {
            out.printf("\n% .40f : %s y", neurons[0].y, layerType);
            out.printf("\n% .40f : %s threshold", ((Perceptron) neurons[0]).theta, layerType);

            for (int i = 0; i < ((Perceptron) neurons[0]).inputs.neurons.length; i++) {
                out.printf("\n% .40f : %s w[%d] ", ((Perceptron) neurons[0]).w[i], layerType, i);
            }
            out.printf("\n% .40f : %s delta", ((Perceptron) neurons[0]).delta, layerType);
            out.printf("\n% .40f : %s delta threshold", ((Perceptron) neurons[0]).deltaTheta, layerType);
            for (int i = 0; i < ((Perceptron) neurons[0]).inputs.neurons.length; i++) {
                out.printf("\n% .40f : %s delta w[%d]", ((Perceptron) neurons[0]).deltaW[i], layerType, i);
            }
        } else {
            for (int i = 0; i < neurons.length; i++) {
                out.printf("\n% .40f : %s y[%d]", neurons[i].y, layerType, i);
            }
            for (int i = 0; i < neurons.length; i++) {
                out.printf("\n% .40f : %s threshold[%d]", ((Perceptron) neurons[i]).theta, layerType, i);
            }

            for (int i = 0; i < neurons.length; i++) {
                for (int j = 0; j < ((Perceptron) neurons[i]).inputs.neurons.length; j++) {
                    out.printf("\n% .40f : %s w[%d][%d] ", ((Perceptron) neurons[i]).w[j], layerType, i, j);
                }
            }
            for (int i = 0; i < neurons.length; i++) {
                out.printf("\n% .40f : %s delta[%d]", ((Perceptron) neurons[i]).delta, layerType, i);
            }

            for (int i = 0; i < neurons.length; i++) {
                out.printf("\n% .40f : %s delta threshold[%d]", ((Perceptron) neurons[i]).deltaTheta, layerType, i);
            }
            for (int i = 0; i < neurons.length; i++) {
                for (int j = 0; j < ((Perceptron) neurons[0]).inputs.neurons.length; j++) {
                    out.printf("\n% .40f : %s delta w[%d][%d]", ((Perceptron) neurons[i]).deltaW[j], layerType, i, j);
                }
            }

            for (int i = 0; i < neurons.length; i++) {
                out.printf("\n% .40f : %s old delta threshold[%d]", ((Perceptron) neurons[i]).oldDeltaTheta, layerType, i);
            }
            for (int i = 0; i < neurons.length; i++) {
                for (int j = 0; j < ((Perceptron) neurons[0]).inputs.neurons.length; j++) {
                    out.printf("\n% .40f : %s old delta w[%d][%d]", ((Perceptron) neurons[i]).oldDeltaW[j], layerType, i, j);
                }
            }
        }
    }
}
