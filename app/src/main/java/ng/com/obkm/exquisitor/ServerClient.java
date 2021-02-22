package ng.com.obkm.exquisitor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ng.com.obkm.exquisitor.Activities.MainActivity;
import ng.com.obkm.exquisitor.Database.VectorBaseHelper;
import ng.com.obkm.exquisitor.DataAccess.VectorLab;
import ng.com.obkm.exquisitor.Fragments.HomeFragment;
import ng.com.obkm.exquisitor.Utilities.Converter;
import ng.com.obkm.exquisitor.Utilities.SharedPreferenceUtil;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ServerClient {

    private final String TAG = "Server";

    private static final int BUFFER = 65536; // 524288
    private final int SIZE_OF_BATCH = 100;
    private static final String POST_URL = "http://192.168.0.12:5000";

    final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(360, TimeUnit.SECONDS)
            .connectTimeout(360, TimeUnit.SECONDS)
            .build();


    // Sources: https://heartbeat.fritz.ai/uploading-images-from-android-to-a-python-based-flask-server-691e4092a95e
    // https://github.com/ahmedfgad/AndroidFlask/blob/master/Part%201/FlaskServer/flask_server.py
    public void zipAndPostToServer(final Context c, final List<String> paths) {

        // Execute on background thread to avoid ANR
        Thread thread = new Thread(new Runnable() {

            public void run() {
                try {
                    String[] imagesToZipArray = new String[SIZE_OF_BATCH]; // 100
                    // All images on the phone
                    int numberOfImages = paths.size();
                    // Number of full bacthes
                    int iterations = numberOfImages / SIZE_OF_BATCH;
                    // Start of last batch
                    int firstOfLastBatch = iterations * SIZE_OF_BATCH;
                    // Number of images in last batch
                    int rest = numberOfImages % SIZE_OF_BATCH;

                    int i = 0;
                    while (iterations > 0) {
                        // fill a batch
                        for (int j = 0; j < SIZE_OF_BATCH; j++) {
                            imagesToZipArray[j] = paths.get(i + j);
                        }
                        File oneZipped = zip(imagesToZipArray, ("zipped_" + i));

                        //Create request body
                        //Change media type according to your file type
                        RequestBody fileRequestBody = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("file", ("zipped_" + i),
                                        RequestBody.create(MediaType.parse("application/zip"), oneZipped))
                                .build();
                        postRequest(c, fileRequestBody);

                        iterations--;
                        Log.i(TAG, "one batch posted.");
                    }
                    // if we have fewer than 100 images on the phone
                    // last batch or few images
                    if (rest > 0 || numberOfImages < SIZE_OF_BATCH) {
                        int y = 0;
                        String[] restImagesArray = new String[rest];

                        for (int z = firstOfLastBatch; z < numberOfImages; z++) {
                            restImagesArray[y] = paths.get(z);
                            y++;
                        }
                        File oneZipped = zip(restImagesArray, ("zipped_" + "rest"));

                        //Create request body
                        //Change media type according to your file type
                        RequestBody fileRequestBody = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("file", ("zipped_" + "rest"),
                                        RequestBody.create(MediaType.parse("application/zip"), oneZipped))
                                .build();
                        postRequest(c, fileRequestBody);

                        Log.i(TAG, "last batch posted.");
                    }

                    // Copy DB to external storage
                    // After response (feature vectors) is received
                    VectorBaseHelper.exportDB();

                } catch (Exception e) { // Runtimte exception can occur
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }


    public File zip(String[] _files, String zipFileName) {
        String internalPath = "/data/data/" + MainActivity.getPACKAGE_NAME();
        File zippedFile = new File(internalPath, zipFileName);

        try {
            Log.i(TAG, "One batch is being zipped.");

            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zippedFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER];

            for (int idx = 0; idx < _files.length; idx++) {
                FileInputStream fi = new FileInputStream(_files[idx]);
                origin = new BufferedInputStream(fi, BUFFER);

                ZipEntry entry = new ZipEntry(_files[idx].substring(_files[idx].lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "zip successful");
        return zippedFile;
    }


    private void postRequest(Context c, RequestBody fileRequestBody) {
        //create the request
        Request fileRequest = new Request.Builder().url(POST_URL)
                .post(fileRequestBody)
                .build();
        //send the request
        getResponse(client, fileRequest, c);
    }

    private void getResponse(OkHttpClient client, Request fileRequest, Context c) {

        try {
            VectorLab vl = VectorLab.get(MainActivity.getContext());

            Response response = client.newCall(fileRequest).execute();
            String responseText = response.body().string();
            JSONObject Jobject = new JSONObject(responseText);
            JSONArray vector_collection = Jobject.getJSONArray("vector_collection");

            for (int i = 0; i < vector_collection.length(); i++) {
                JSONObject obj = vector_collection.getJSONObject(i);
                String path = obj.getString("path");
                int imageID = vl.getLastImageID() + 1;
                vl.addIDPathPairs(imageID, path);
                String labels = obj.getString("labels");
                String probabilities = obj.getString("probabilities");
                int[] labelsIntArray = Converter.getIntLabels(labels);
                float[] probsFloatArray = Converter.getFloatProbs(probabilities);

                for (int j = 0; j < HomeFragment.NUMBER_OF_HIGHEST_PROBS; j++) {
                    // add labels and probs into database
                    vl.addLabelProbToVectors(imageID, labelsIntArray[j], probsFloatArray[j]);
                }
            }
            SharedPreferenceUtil.writeToSharedPreferences(c);

        } catch(IOException | JSONException e) {
            Log.i(TAG, "postRequest exception: " + e);
        }
    }
}