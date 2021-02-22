//package ng.com.obkm.exquisitor.SVMAssets;
//
//import android.content.Intent;
//import android.util.Log;
//
//import java.util.Arrays;
//
//import ng.com.obkm.exquisitor.Activities.PhotoItemActivity;
//import ng.com.obkm.exquisitor.DataAccess.VectorLab;
//import ng.com.obkm.exquisitor.Utilities.Converter;
//
//public class SVMDataPrep {
//
//    /**
//     * Builds training data from image data received from PhotoItemActivity
//     * @param data
//     */
//    protected void buildTrainingData(Intent data) {
//
//        String path = PhotoItemActivity.getVectorPath(data);
//        // path transformation
//        path = Converter.getShortPath(path);
//
//        int imageID = VectorLab.getIDFromPath(path);
//
//        //long startTime1 = System.nanoTime();
//        float[] vectorValues = VectorLab.queryProbsAsFloats(imageID);
//        //Log.i(TAG, "VectorValues: " + Arrays.toString(vectorValues));
//        //long elapsedTime1 = System.nanoTime() - startTime1;
//        //System.out.println("Elapsed time vectorValues nanosec: " + elapsedTime1);
//
//        //long startTime2 = System.nanoTime();
//        int[] vectorLabels = VectorLab.queryLabelsAsInts(imageID);
//        //long elapsedTime2 = System.nanoTime() - startTime2;
//        //System.out.println("Elapsed time vectorLabels nanosec: " + elapsedTime2);
//
//        int vectorRating = PhotoItemActivity.getVectorRating(data);
//        //Log.i(TAG, "Rating: " + vectorRating);
//
//        trainingDataValues.add(vectorValues);
//        //Log.i(TAG, "training data size: " + trainingDataValues.size());
//
//        trainingDataLabels.add(vectorLabels);
//        ratings.add(vectorRating);
//        numberOfFeedback++;
//    }
//}
