package ng.com.obkm.exquisitor.Utilities;

import android.os.Environment;

import java.util.List;

public class Converter {

    // List<Integer> --> int[]
    public static int[] convertIntListToArray(List<Integer> intList) {
        int[] intArray = new int[intList.size()];

        for(int i = 0; i < intList.size(); i++ ) {
            intArray[i] = intList.get(i);
        }

        return intArray;
    }

    // List<Float> --> float[]
    public static float[] convertFloatListToArray(List<Float> floatList) {
        float[] floatArray = new float[floatList.size()];

        for(int i = 0; i < floatList.size(); i++ ) {
            floatArray[i] = floatList.get(i);
        }

        return floatArray;
    }

    public static String getFullPath(String shortPath) {
        String fullPath = "";
        /*if(shortPath.startsWith("Screen")) shortPath = "Screenshots/" + shortPath;
        else shortPath = "Camera/" + shortPath;*/
        if (!shortPath.startsWith(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString())) {
            fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/" + shortPath;
            return fullPath;
        } else return shortPath;
    }

    public static String getShortPath(String fullPath) {
        String shortPath = "";
        //String directoryPath;
        /*if(fullPath.contains("Camera"))  directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/";
        else directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Screenshots/";*/
        if(fullPath.startsWith(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString())) {
            shortPath = fullPath.replace( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/", "");
            return shortPath;
        }
        return fullPath;
    }

    // String --> int[]
    public static int[] getIntLabels(String labels) {
        String[] labelsS = labels.replace("[", "").replace("]", "").split(",");
        int[] labelsIntArray = new int[labelsS.length];
        for (int i = 0; i < labelsS.length; i++) labelsIntArray[i] = Integer.parseInt(labelsS[i]);
        return labelsIntArray;

    }

    // String --> float[]
    public static float[] getFloatProbs(String probs) {
        String[] probsS = probs.replace("[", "").replace("]", "").split(",");
        float[] probsFloatArray = new float[probsS.length];
        for (int i = 0; i < probsS.length; i++) probsFloatArray[i] = Float.parseFloat(probsS[i]);
        return probsFloatArray;
    }

    // List<Integer> --> double[]
    public static double[] convertLabelsListToArray(List<Integer> ratingsList){
        double [] tmp = new double[ratingsList.size()];
        for (int i = 0 ; i < ratingsList.size(); i++){
            tmp[i] = (double) ratingsList.get(i);
        }
        return tmp;
    }

}
