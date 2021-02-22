package ng.com.obkm.exquisitor.SVMAssets;

import java.util.List;

import ng.com.obkm.exquisitor.Fragments.HomeFragment;
import ng.com.obkm.exquisitor.LocalSVM.svm;
import ng.com.obkm.exquisitor.LocalSVM.svm_problem;
import ng.com.obkm.exquisitor.LocalSVM.svm_model;
import ng.com.obkm.exquisitor.LocalSVM.svm_node;
import ng.com.obkm.exquisitor.LocalSVM.svm_parameter;
import ng.com.obkm.exquisitor.Utilities.Converter;

import static ng.com.obkm.exquisitor.LocalSVM.svm.svm_predict_distance;


public class SVMBuild {


    private static svm_model model = new svm_model();
    private static String TAG = "TestSVM";

    /**
     * Set parameters for the SVM model
     * @return parameters
     */
    public static svm_parameter buildParameter(){
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.degree = 3;
        param.gamma = 0; //experiment
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 40;
        param.C = 10; //experiment
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 0;
        param.probability = 0;
        param.nr_weight = 0;
        // trying to assign higher penality for class 1 --> experiment
        param.weight_label = new int[2];
        param.weight_label[0] = 1;
        param.weight_label[1] = -1;
        param.weight = new double[2];
        param.weight[0] = 1;
        param.weight[1] = 5;
        return param;
    }

    /**
     * Builds an SVM problem from ratings and training data
     * @param ratings positive (1.0) or negative (-1.0) ratings
     * @param trainingData two dimensional array with features and probabilities
     * @return
     */
    public static svm_problem buildSVMProblem(double[] ratings, svm_node[][] trainingData) {
        svm_problem problem = new svm_problem();
        problem.l = ratings.length;
        problem.x = trainingData;
        problem.y = ratings;
        return problem;
    }

    /**
     * Trains an SVM model using SVM parameters and SVM problem
     * @param ratingsList
     * @param vectorValuesList
     * @param vectorLabelsList
     * @return
     */
    public static svm_model buildModel(List<Integer> ratingsList, List<float[]> vectorValuesList, List<int[]> vectorLabelsList) {
        // Building SVM parameters
        long beginning = System.nanoTime();
        svm_parameter param = buildParameter();

        // Building SVM training data
        svm_node[][] sparseTrainingVectors = buildSparseVectorValuesArray(ratingsList.size(), vectorValuesList,vectorLabelsList );
        double[] ratings = Converter.convertLabelsListToArray(ratingsList);

        // Building SVM problem
        svm_problem problem = buildSVMProblem(ratings, sparseTrainingVectors);
        // Checking parameters for model
        String error_msg = svm.svm_check_parameter(problem,param);
        if(error_msg != null) {
            System.err.print("Error: "+error_msg+"\n");
            System.exit(1);
        }
        else {
            model = svm.svm_train(problem, param);
        }
        long end = System.nanoTime() - beginning;
        return model;
    }

    //sparse vector
    private static svm_node[][] buildSparseVectorValuesArray(int numberOfRatings, List<float[]> vectorValuesList, List<int[]> vectorLabelsList ) {
        long startTime = System.nanoTime();
        svm_node[][] probs = new svm_node[numberOfRatings][HomeFragment.NUMBER_OF_HIGHEST_PROBS];

        for(int i = 0; i < numberOfRatings; i++)
        {
            svm_node[] svmNodeVector = buildOneSparseVector6(vectorValuesList.get(i), vectorLabelsList.get(i));
            for(int j = 0; j < HomeFragment.NUMBER_OF_HIGHEST_PROBS; j++){
                probs[i][j] = svmNodeVector[j];
            }
        }
        long elapsedTime = System.nanoTime() - startTime;
        return probs;
    }

    // containes all the nodes for all the ratings and builds a 2-dimensional array with it
//    private static svm_node[][] buildVectorValuesArray(int numberOfRatings, List<float[]> vectorValuesList) {
//        svm_node[][] probs = new svm_node[numberOfRatings][1001];
//
//        for(int i = 0; i < numberOfRatings; i++)
//        {
//            svm_node[] svmNodeVector = buildOneDenseVector1001(vectorValuesList.get(i));
//            for(int j = 0; j < 1001; j++){
//                probs[i][j] = svmNodeVector[j];
//            }
//        }
//        return probs;
//    }

    //Used in create SVM model
    // build a vector based on all 1000 features
//    private static svm_node[] buildOneDenseVector1001(float[] probs){
//        svm_node[] svmNodeArray = new svm_node[1001];
//        for (int i = 0; i < 1001; i++){
//            svmNodeArray[i] = buildOneIndexLabelEntry(i, probs[i]);
//        }
//        return svmNodeArray;
//    }

    //Used in create SVM model
    // build a vector based on all 1000 features
    private static svm_node[] buildOneSparseVector6(float[] probsLists, int[] labelList){
        long startTime = System.nanoTime();
        svm_node[] svmNodeArray = new svm_node[HomeFragment.NUMBER_OF_HIGHEST_PROBS];
        for (int i = 0; i < HomeFragment.NUMBER_OF_HIGHEST_PROBS; i++){
            svmNodeArray[i] = buildOneIndexLabelEntry(labelList[i], probsLists[i]);
        }
        long elapsedTime = System.nanoTime() - startTime;
        return svmNodeArray;
    }

    // Used to create SVM model
    // Turn String values to Float for 6 best feature values AND other feature values with 0
//    private static float[] getFloatValuesDense(String values) {
//        String[] stringValues = values.split(" ");
//
//        float[] floatValues = new float[1001];
//        // add float probabilites to right position in 1001 long probability array
//        for (int i = 0; i < 1001; i++) {
//            floatValues[i] = Float.parseFloat(stringValues[i]);
//        }
//        return floatValues;
//    }

    // used in prediction
    // Building one vector -> array of nodes
//    private static svm_node[] buildOneSparseVector(String bestProbsString, String bestLabelsString) {
//        float[] probsArray = getFloatValues(bestProbsString);
//        int[] labelsArray = getIntValues(bestLabelsString);
//        svm_node[] svmNodeArray = new svm_node[HomeFragment.NUMBER_OF_HIGHEST_PROBS];
//        for (int i = 0; i < 6; i++){
//            svmNodeArray[i] = buildOneIndexLabelEntry(labelsArray[i], probsArray[i]);
//        }
//        return svmNodeArray;
//    }

    // used in prediction
    // Turn String values to Float array
//    private static float[] getFloatValues(String values) {
//        String[] stringValues = values.split(" ");
//        float[] floatValues = new float[HomeFragment.NUMBER_OF_HIGHEST_PROBS];
//        for (int i = 0; i < stringValues.length; i++) {
//            floatValues[i] = Float.parseFloat(stringValues[i]);
//        }
//        return floatValues;
//    }

    // one node = (index,value) --> one probability value in the vector --> if we work with the entire vector, we would need 1000
    public static svm_node buildOneIndexLabelEntry(int index, double x) {
        long startTime = System.nanoTime();
        svm_node node = new svm_node();
        node.index = index;
        node.value = x;
        long elapsedTime = System.nanoTime() - startTime;
        return node;
    }

//    private static int[] getIntValues(String values) {
//        String[] stringValues = values.split(" ");
//        int[] intValues = new int[stringValues.length];
//        for (int i = 0; i < stringValues.length; i++) intValues[i] = Integer.parseInt(stringValues[i]);
//        return intValues;
//    }

    public static double doPredictionDistanceBased(svm_model model, float[] probsLists, int[] labelList) {
        long startTime = System.nanoTime();
        // Get the vector of the image that we want to predict
        svm_node[] node = buildOneSparseVector6(probsLists, labelList);
        long elapsedTime = System.nanoTime() - startTime;
        return svm_predict_distance(model, node);
    }
}