package my.nn;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataSet implements java.io.Serializable {

    private List<Sample> samples = new ArrayList<Sample>();
    Random rnd;
    int access;
    private List<Integer> selectedIndexes = new ArrayList<Integer>();

    public void loadFromText(String fileName) {
        BufferedReader br = null;
        int inputs = 0;
        int outputs = 0;
        boolean fileStructureValid = true;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();

            while (line != null) {
                if(line.startsWith("#!")) {
                    String[] structure = line.split(" ");
                    if(structure.length > 1){
                        inputs = Integer.parseInt(structure[1]);
                        if(structure.length > 2){
                                outputs = Integer.parseInt(structure[2]);
                        }
                        fileStructureValid = true;
                    }
                }
                else if(line.startsWith("#")) {
                }
                else if(fileStructureValid){
                    String[] values = line.split(",");
                    if(values.length == inputs + outputs){
                        Sample sample = new Sample(inputs, outputs);
                        double[] inputValues = new double[inputs];
                        double[] outputValues = new double[outputs];
                        for(int i = 0; i < inputs; i++){
                            inputValues[i] = Double.parseDouble(values[i]);
                        }
                        
                        for(int i = inputs; i<values.length; i++){
                            outputValues[i-inputs] = Double.parseDouble(values[i]);
                        }
                        sample.setInputs(inputValues);
                        sample.setOutputs(outputValues);
                        this.samples.add(sample);
                    }
                }
                    
                line = br.readLine();

            }
            br.close();
        } catch (Exception e) {
            try {
                br.close();
            } catch (Exception f) {
            }
        }
    }

    void loadFromCsv(String fileName) {

    }

    void load(String fileName) {
        DataSet dt = null;
        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            dt = (DataSet) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("DataSet class not found");
            c.printStackTrace();
        }
        this.access = dt.access;
        this.rnd = dt.rnd;
        this.samples = dt.samples;
        this.selectedIndexes = dt.selectedIndexes;
    }

    public void save(String fileName) {
        try {
            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in " + fileName);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    void saveToTextFile(String fileName) {
        FileWriter fstream = null;
        int inputs = 0;
        int outputs = 0;
        
        if(!samples.isEmpty()){
            inputs = samples.get(0).getInputs().length;
            outputs = samples.get(0).getOutputs().length;
        }
        
        try {
            fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);

            out.write("#! {0} {1}", inputs, outputs);
            for (Sample sample : samples) {
                for (Double d: sample.getInputs()){
                    out.write(Double.toString(d) + " ");
                }
                for (Double d: sample.getOutputs()){
                    out.write(Double.toString(d) + " ");
                }
                out.write("\r\n");
            }
            out.close();
            fstream.close();

        } catch (Exception e) {
        } finally {
            try {
                fstream.close();
            } catch (Exception e) {
            }
        }
    }

    public void saveToCsvFile(String filename) {

    }

    public int size() {
        int size = samples.size();
        return size;
    }

    public Sample get(int index) {
        if (index < samples.size() || index >= 0) {
            Sample sample = samples.get(index);
            return sample;
        } else {
            return null;
        }
    }

    public void randomInit(long seed, int access) {
        rnd = new Random(seed);
        this.access = access;
    }

    public Sample getRandom() {

        boolean indexValid = false;

        int nextIndex = 0;
        while (indexValid == false) {
            nextIndex = rnd.nextInt(samples.size());
            switch (access) {
                case 1: {
                    indexValid = true;
                }

                case 2: {
                    if (!selectedIndexes.contains(nextIndex)) {
                        selectedIndexes.add(nextIndex);
                        indexValid = true;
                    }
                }
            }
        }
        return samples.get(nextIndex);
    }

    public void append(DataSet dataset) {
        samples.addAll(dataset.getSamples());
    }

    public void clear() {
        samples.clear();
        selectedIndexes.clear();
    }

    public void remove(int index) {
        if (index < samples.size() || index >= 0) {
            samples.remove(index);
        }
    }

    public void remove(Sample sample) {
        samples.remove(sample);
    }

    public void add(Sample sample) {
        samples.add(sample);
    }

    public void print(PrintStream out) {

    }

    public List<Sample> getSamples() {
        return samples;
    }
}
