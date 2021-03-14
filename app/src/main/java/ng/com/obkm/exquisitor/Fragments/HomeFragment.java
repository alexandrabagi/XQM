package ng.com.obkm.exquisitor.Fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import ng.com.obkm.exquisitor.Activities.PhotoItemActivity;
import ng.com.obkm.exquisitor.Activities.PhotoPagerActivity;
import ng.com.obkm.exquisitor.Entity.Candidate;
import ng.com.obkm.exquisitor.LocalSVM.svm_model;
import ng.com.obkm.exquisitor.Utilities.Converter;
import ng.com.obkm.exquisitor.Utilities.MediaStoreCheck;
import ng.com.obkm.exquisitor.Utilities.MyPagerAdapter;
import ng.com.obkm.exquisitor.Utilities.PictureUtils;
import ng.com.obkm.exquisitor.R;
import ng.com.obkm.exquisitor.SVMAssets.SVMBuild;
import ng.com.obkm.exquisitor.DataAccess.VectorLab;

import static ng.com.obkm.exquisitor.DataAccess.NegativeImageLab.resetNegList;
import static ng.com.obkm.exquisitor.DataAccess.PositiveImageLab.resetList;
import static ng.com.obkm.exquisitor.Utilities.Converter.getFullPath;
import static ng.com.obkm.exquisitor.Utilities.Converter.getShortPath;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    final String TAG = "home";
    final int REQUEST_CODE = 100;

    private List<String> mImagesOnScreenList = new ArrayList<>(); // used for populateMainScreen
    private List<String> mImagesOnScreenListSaved = new ArrayList<>(); // used for saving image list between ratings

    // Training data
    private static ArrayList<Integer> ratings = new ArrayList<>();
    private static List<float[]> trainingDataValues = new ArrayList<>();
    private static List<int[]> trainingDataLabels = new ArrayList<>();

    public static int NUMBER_OF_HIGHEST_PROBS = 5;
    protected static int numberOfFeedback = 0;

    // DB actions
    private VectorLab vectorLab;
    // Adding new image case
    private MediaStoreCheck mSC;


    public HomeFragment() {
    }

    ///// LIFECYCLE METHODS /////

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context mContext = getActivity();
        VectorLab.get(getActivity()).removeSeen();
        setHasOptionsMenu(true);
        mSC = new MediaStoreCheck(mContext);
    }

    /**
     * Called when activity is created and fragment is attached
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        vectorLab = VectorLab.get(getActivity());
        View v = inflater.inflate(R.layout.fragment_home_const, container, false);
        createPagerDialog();
        return v;
    }

    /**
     * Gets six random images for starting screen
     * or load images shown on the screen before app was paused.
     */
    @Override
    public void onStart() {
        super.onStart();

        if (numberOfFeedback < 1) {
            //mImagesOnScreenList = getRandomSixList();
            mImagesOnScreenList = makeStartScreenListForTesting();
        }
        if (!mImagesOnScreenListSaved.isEmpty()) {
            mImagesOnScreenList = mImagesOnScreenListSaved;
        }
        populateMainScreen(mImagesOnScreenList);
    }

    /**
     * Save images shown on the screen when app is paused.
     */
    @Override
    public void onPause() {
        super.onPause();
        mImagesOnScreenListSaved = mImagesOnScreenList;
    }

    // method to return always same 6 images on screen
    private List<String> makeStartScreenListForTesting()
    {
        Log.i("lifecycle", "makeStartScreenListForTesting: called ");
        List<String> testScreenList = new ArrayList<>();
        testScreenList.add(getFullPath("118501.jpg")); // starting picture
        addToSeenImagesFromLongPath(getFullPath("118501.jpg"));
        testScreenList.add(getFullPath("102000.jpg"));
        addToSeenImagesFromLongPath(getFullPath("102000.jpg"));
        testScreenList.add(getFullPath("121404.jpg"));
        addToSeenImagesFromLongPath(getFullPath("121404.jpg"));
        testScreenList.add(getFullPath("124001.jpg"));
        addToSeenImagesFromLongPath(getFullPath("124001.jpg"));
        testScreenList.add(getFullPath("141703.jpg"));
        addToSeenImagesFromLongPath(getFullPath("141703.jpg"));
        testScreenList.add(getFullPath("100100.jpg"));
        addToSeenImagesFromLongPath(getFullPath("100100.jpg"));

        for(int i = 0; i < testScreenList.size(); i++){
            Log.i("lifecycle", "added to start test screen images " + testScreenList.get(i));
        }
        //searchStart = false;
        return testScreenList;
    }

    private void addToSeenImagesFromLongPath(String longPath){
        //add to db as seen
        Log.i("here", "added to seen images: " + longPath);
        String shortPath = getShortPath(longPath);
        //Log.i(TAG, "path to update image short: " + shortPath);
        int imageID = VectorLab.getIDFromPath(shortPath);
        //Log.i(TAG, "path to update image short: " + imageID);
        vectorLab.updateSeen(imageID);
    }


    ///// CALLBACK FROM PhotoPagerActivity /////

    /**
     * Builds training data and sets new image on screen based on the result of PhotoItemActivity
     * @param requestCode defined as a final variable belonging to the fragment
     * @param resultCode OK or CANCELED
     * @param data Intent data from PhotoItemActivity
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Abort if request code or result code is not appropriate
        if (resultCode != getActivity().RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE) {
            if (data == null) {
                return;
            }
        }

        // Build training data from received data
        buildTrainingData(data);
        // Set on screen image set based on trained model results
        setImagesOnScreenList(data);
    }

    ///// DATA PREPARATION FOR SVM

    /**
     * Builds training data from image data received from PhotoItemActivity
     * @param data
     */
    protected void buildTrainingData(Intent data) {

        String path = PhotoPagerActivity.getVectorPath(data);

        // path transformation
        path = getShortPath(path);

        int imageID = VectorLab.getIDFromPath(path);

        float[] vectorValues = VectorLab.queryProbsAsFloats(imageID);
        int[] vectorLabels = VectorLab.queryLabelsAsInts(imageID);

        int vectorRating = PhotoPagerActivity.getVectorRating(data);

        // training data for model
        trainingDataValues.add(vectorValues);
        trainingDataLabels.add(vectorLabels);
        ratings.add(vectorRating);

        numberOfFeedback++;
    }


    ///// FINDING THE BEST CANDIDATES

    /**
     * Returns the best candidates based on the model that will be shown on the screen
     * Used by the "Fast Forward" function
     * @param model SVM model that is used to find the best candidates
     * @return a list of the paths of the best candidates marked by the model
     */
    private List<String> getBestCandidatesDistanceBased(svm_model model) {

        // Setting up a DS for the best candidates ids, paths and distances
        Map<Integer, Double> bestCandidates = new HashMap<>(); // 6 best distances
        Map<String, Double> bestCandidatePaths = new HashMap<>();
        List<String> bestPaths;

        // Look through the unseen image features-probabilities to find the closest to boundary
        Map<Integer, List<Integer>> unseenLabels = vectorLab.queryUnseenFeatures(); // Map <imageID, labelList>
        Map<Integer, List<Float>> unseenProbs = vectorLab.queryUnseenProbs(); // Map <imageID, probsList>

        Iterator<Map.Entry<Integer, List<Integer>>> it = unseenLabels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, List<Integer>> entry = it.next();
            Integer key = entry.getKey();
            List<Integer> labelsVectorList = unseenLabels.get(key);
            List<Float> probsVectorList = unseenProbs.get(key);

            //convert to arrays for model
            int[] testLabels = Converter.convertIntListToArray(labelsVectorList);
            float[] testProbs = Converter.convertFloatListToArray(probsVectorList);

            // get distance for each unseen image
            double distance = SVMBuild.doPredictionDistanceBased(model, testProbs, testLabels);

            if(bestCandidates.size() < 6 ) {
                bestCandidates.put(key, distance);
            }
            else {
                Comparator<Double> c = new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) { // -1 if o1 < o2
                        if (o1 < o2) return -1;
                        else if (o1.equals(o2)) return 0;
                        else return 1; // o1 > o2
                    }
                };
                Map.Entry<Integer, Double> min = null;
                // Getting the minimum distance value in bestCandidates
                for(Map.Entry<Integer, Double> bestEntry : bestCandidates.entrySet()) {
                    if(min == null || (c.compare(min.getValue(), bestEntry.getValue()) > 0)) {
                        min = bestEntry;
                    }
                }

                if(distance > min.getValue()) {
                    bestCandidates.remove(min.getKey());
                    bestCandidates.put(key, distance);
                }
            }
        }

        for (int key : bestCandidates.keySet()) {
            String path = vectorLab.getPathFromID(key);
            bestCandidatePaths.put(path, bestCandidates.get(key));
        }

        bestPaths = orderBestCandidatePaths(bestCandidatePaths);
        return bestPaths;
    }

    /**
     * Returns the best candidate based on the model that will be shown on the screen as the exchanged image
     * Used in the normal rating process
     * @param model SVM model that is used to find the best candidate
     * @return the path of the best candidate marked by the model
     */
    private String getOneBestCandidateDistanceBased(svm_model model) {

        Candidate bestCandidate = new Candidate(0, 0.0);

        // Look through the unseen image features-probabilities to find the closest to boundary
        Map<Integer, List<Integer>> unseenLabels = vectorLab.queryUnseenFeatures(); // Map <imageID, labelList>
        Map<Integer, List<Float>> unseenProbs = vectorLab.queryUnseenProbs(); // Map <imageID, probsList>

        Iterator<Map.Entry<Integer, List<Integer>>> it = unseenLabels.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<Integer, List<Integer>> entry = it.next();
            Integer key = entry.getKey();

            List<Float> probsVectorList = unseenProbs.get(key);
            List<Integer> labelsVectorList = unseenLabels.get(key);

            //convert to arrays for model
            int[] testLabels = Converter.convertIntListToArray(labelsVectorList);
            float[] testProbs = Converter.convertFloatListToArray(probsVectorList);

            // get distance for each unseen image
            double distance = SVMBuild.doPredictionDistanceBased(model, testProbs, testLabels);

            // look for the largest + distance
            if(distance > bestCandidate.getDistance()) {
                bestCandidate.setImageID(key);
                bestCandidate.setDistance(distance);
            }

        }
        // Return the best candidate
        // in case we only had negative examples and no image vector is on the positive side of the decision boundary
        if(bestCandidate.getDistance() == 0.0) {
            return getRandomImageFromDB();
        } else {
            return vectorLab.getPathFromID(bestCandidate.getImageID());
        }
    }

    /**
     * Deleting all stored data for a clean start
     */
    private void cleanAllForStartOver() {
        vectorLab.removeSeen();
        resetNegList();
        resetList();
        trainingDataValues.clear();
        trainingDataLabels.clear();
        ratings.clear();
        List<String> randomPathList = getRandomSixList();
        populateMainScreen(randomPathList);
        mImagesOnScreenList = randomPathList;
        mImagesOnScreenListSaved = mImagesOnScreenList;
        numberOfFeedback = 0;
    }

    protected static void removeTrainingExample(int imageID) {

        int[] labelsToRemove = VectorLab.queryLabelsAsInts(imageID);
        float[] valuesToRemove = VectorLab.queryProbsAsFloats(imageID);
        for (int i = 0; i < trainingDataLabels.size(); i++) {
            if (Arrays.equals(labelsToRemove, trainingDataLabels.get(i))) {
                trainingDataLabels.remove(trainingDataLabels.get(i));
                ratings.remove(i);
            }
        }
        for (int i = 0; i < trainingDataValues.size(); i++) {
            if (Arrays.equals(valuesToRemove, trainingDataValues.get(i))) {
                trainingDataValues.remove(trainingDataValues.get(i));
            }
        }
    }


    ///// SET IMAGES ON SCREEN /////

    /**
     * Loads mImagesOnScreenListSaved, change the clicked image to the bestCandidate
     * @param data
     */
    private void setImagesOnScreenList(Intent data) {
        String path = PhotoPagerActivity.getVectorPath(data); // path received from intent
        path = getFullPath(path);

        int index = -1;

        if (mImagesOnScreenListSaved.contains(path)) {
            index = mImagesOnScreenListSaved.indexOf(path); // check what is the index of the clicked image
            mImagesOnScreenListSaved.remove(path); // remove it from the images on screen
        } else {
            Log.e(TAG, "imagesOnScreen doesn't contain path");
        }

        if (numberOfFeedback < 2) {
            // we don't run SVM after the first rating (the distance would be NaN), replace clicked with random image
            String randomPath = getRandomImageFromDB();
            mImagesOnScreenListSaved.add(index, randomPath);
            updateSeenInDB(randomPath);
        } else {
            // we run SVM after the second rating, replace clicked with bestCandidate
            svm_model model = SVMBuild.buildModel(ratings, trainingDataValues, trainingDataLabels);
            String bestPath = getOneBestCandidateDistanceBased(model);

            String fullPathToAdd = getFullPath(bestPath);
            mImagesOnScreenListSaved.add(index, fullPathToAdd);
            updateSeenInDB(fullPathToAdd);
        }
    }

    /**
     * Updates the corresponding seen column in the DB
     * @param longPath full image path
     */
    private void updateSeenInDB(String longPath){
        String shortPath = getShortPath(longPath);
        int imageID = VectorLab.getIDFromPath(shortPath);
        vectorLab.updateSeen(imageID);
    }

    /**
     * Sets six images on the screen based on the imageList param
     * @param imagesList list of image paths to be set on screen
     */
    private void populateMainScreen(List<String> imagesList) {

        try {
            for (int i = 0; i < imagesList.size(); i++) {
                // Building resource name string
                String name = "galleryImage".concat(String.valueOf(i + 1));
                int resID = getResources().getIdentifier(name, "id", getActivity().getPackageName());
                final ImageView myImage = (ImageView) getView().findViewById(resID);

                // Update each image on the screen
                String pathFromList = imagesList.get(i);
                final String path = getFullPath(pathFromList);
                updateImageView(path, myImage);

                myImage.setOnClickListener(new View.OnClickListener() {
                    // Starting the new activity where image can be rated
                    @Override
                    public void onClick(View view) {
                        Intent intent = PhotoPagerActivity.newIntent(getActivity(), path);
                        startActivityForResult(intent, REQUEST_CODE);
                    }
                });
            }
        }
        catch (Exception e) {
            Toast.makeText(getActivity(), "You have seen a all images. Please start a new search. To continue", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets bitmap image for imageView
     * @param path String path of the image
     * @param myImage imageView from XML
     */
    private void updateImageView(String path, ImageView myImage) {
        File imgFile = new File(path);
        if (!imgFile.exists()) {
            Drawable noImageDrawable = getResources().getDrawable(R.drawable.no_photo_small);
            myImage.setImageDrawable(noImageDrawable);
            vectorLab.deleteEntryFromDB(getShortPath(path));
        } else {
            Bitmap myBitmap = PictureUtils.getThunbnail(imgFile.getPath());
            myImage.setImageBitmap(myBitmap);
        }
    }

    /**
     * Sets the next six best candidate in mImagesOnScreenList
     * for function "Fast Forward"
     */
    private void putNextSixBestIntoScreenListSaved(){

            if (numberOfFeedback < 2) { // we don't run SVM after the first rating (the distance would be NaN), replace clicked with random image
                Toast.makeText(getActivity(), "Please rate at least twice", Toast.LENGTH_SHORT).show();
            } else { // we run SVM after the second rating, replace clicked with bestCandidate
                svm_model model = SVMBuild.buildModel(ratings, trainingDataValues, trainingDataLabels);
                List<String> bestPaths = getBestCandidatesDistanceBased(model);
                    // add the next best from bestCandidatePaths
                    boolean added = false;
                    //Log.i(TAG, "Initial mImagesOnScreenList size " + mImagesOnScreenList.size());
                    mImagesOnScreenList.clear();
                    for (int index = 0; index < 6; index++) {
                        String pathToAdd = bestPaths.get(index);
                        String fullPathToAdd = getFullPath(pathToAdd);
                        Log.i("bestCandidate", "PathToAdd: " + fullPathToAdd);
                        //Log.i(TAG, "mImagesOnScreenList size " + mImagesOnScreenList.size());
                        if (!mImagesOnScreenList.contains(fullPathToAdd)) {
                            mImagesOnScreenList.add(index, fullPathToAdd);
                            //Log.i(TAG, "index " + index + " path " + fullPathToAdd);
                            updateSeenInDB(fullPathToAdd);
                            added = true;
                            //Log.i("bestCandidate", "One best candidate path was added");
                        }
                    }
                    if (!added)
                        Log.e("bestCandidate", "No path was added from orderedBestPaths list");
            }

        mImagesOnScreenListSaved = mImagesOnScreenList;
    }

    /**
     * Returns six random image from the database
     * @return list of six random image path
     */
    private List<String> getRandomSixList() {
        Set<String> pathSet = new HashSet<>();
        String path = "";
        while (pathSet.size() < 6) {
            path = getRandomImageFromDB();
            pathSet.add(path);
            // mark as seen in db
            updateSeenInDB(path);
        }
        return new ArrayList<>(pathSet);
    }

    /**
     * Returns a random image from the DB
     * @return String path of a random image
     */
    private String getRandomImageFromDB() {
        int randomID = vectorLab.queryRandomUnseen();
        return getFullPath(vectorLab.getPathFromID(randomID));
    }

    // SOURCE: https://stackoverflow.com/questions/28163279/sort-map-by-value-in-java

    /**
     * Orders the best candidate paths by the distance from the decision boundary
     * @param bestCandPaths map with the best candidate paths and distances
     * @return a list of best candidate paths ordered by the distance
     */
    private List<String> orderBestCandidatePaths(Map<String, Double> bestCandPaths) {

        List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(bestCandPaths.entrySet());
        List<String> orderedBestPaths = new ArrayList<>();

        Comparator<Map.Entry<String, Double>> comp = new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2 ) {
                return ( o1.getValue()).compareTo( o2.getValue()); // e.g. a<b -> -1
            }
        };

        Collections.sort(list, comp);
        for (Map.Entry<String, Double> entry : list) {
            orderedBestPaths.add(entry.getKey());
        }
        return orderedBestPaths;
    }


    ///// MENU CREATION /////

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_home, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.refresh:
                try{
                    mImagesOnScreenList = getRandomSixList();
                    populateMainScreen(mImagesOnScreenList);
                    mImagesOnScreenListSaved = mImagesOnScreenList;
                    return true;
                }
                catch (Exception e)
                {
                    Toast.makeText(getActivity(), "You have seen a all images. Please start a new search. To continue", Toast.LENGTH_SHORT).show();
                }
            case R.id.startOver:
                cleanAllForStartOver();
                return true;
            case R.id.next_Best:
                try { Log.i("limitTest", "remaining unseen images " + vectorLab.queryUnseenProbs().size());
                    putNextSixBestIntoScreenListSaved();
                    populateMainScreen(mImagesOnScreenList);
                    mImagesOnScreenListSaved = mImagesOnScreenList;
                }
                catch (Exception e) {
                    Toast.makeText(getActivity(), "You have seen a all images. Please start a new search. To continue", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.helpButton:
                openHelpDialog();
                return true;
            case R.id.updatepButton:
                System.out.println("Last id: " + vectorLab.getLastImageID());
                mSC.analyseNewImages();
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    ///// DIALOGS

    private void createPagerDialog() {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.intro_dialog);

        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.95);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.70);

        dialog.getWindow().setLayout(width, height);

        final MyPagerAdapter adapter = new MyPagerAdapter(getActivity(), dialog);
        final ViewPager pager = (ViewPager) dialog.findViewById(R.id.dialog_pager);
        // Credit: https://stackoverflow.com/questions/20586619/android-viewpager-with-bottom-dots
        TabLayout tabLayout = (TabLayout) dialog.findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(pager, true);
        pager.setAdapter(adapter);

        dialog.show();
    }

    private void openHelpDialog() {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.help);

        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.95);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.70);

        dialog.getWindow().setLayout(width, height);

        Button closeButton = dialog.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}