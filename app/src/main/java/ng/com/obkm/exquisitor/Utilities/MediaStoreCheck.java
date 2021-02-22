package ng.com.obkm.exquisitor.Utilities;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ng.com.obkm.exquisitor.ServerClient;

public class MediaStoreCheck{

    private final String TAG = "MedieStoreCheck";
    private Cursor cursor = null;
    private Context mContext;
    private List<String> newImageList = new ArrayList<>();

    public MediaStoreCheck(Context c){
        mContext =c;
    }

    public List<String> checkForNewImages() { //TODO: would be nice to make this String[]

        try {
            Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            String[] projection = new String[]{
                    MediaStore.Images.ImageColumns._ID,
                    MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.DISPLAY_NAME,
            };

            cursor = mContext.getContentResolver().query(contentUri, projection, null, null, MediaStore.Images.ImageColumns._ID);
            System.out.println("Cursor count: " + cursor.getCount());

            long twodays = 172800000;
            long lastUpdate = SharedPreferenceUtil.getLastAnalysisInMilliSecSharedPreferences(mContext); //TODO: delete - twodays
            Date d = new Date();
            long today = d.getTime();
//            long twodaysAgo = today - twodays;
            long twodaysAgo = lastUpdate;


            if (cursor.moveToFirst()) {
                    //int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID);
                    int dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN);
                    System.out.println("Date taken index: " + dateTakenIndex);
                    int displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME);
                    System.out.println("Display name index: " + displayNameIndex);

                    while (cursor.moveToNext()){
                        //long mediaId = cursor.getLong(idIndex);
                        String filename = cursor.getString(displayNameIndex);
                        System.out.println("Filename: " + filename);
                        long millis = cursor.getLong(dateTakenIndex);
                        // scoped storage - access file via uri instead of filepath + filename
                        //Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId);
                        Log.i(TAG, "we retrieve: filename " + filename + " min " + lastUpdate);
                        System.out.println("date: " + twodaysAgo);
                        System.out.println("millis: " + millis);
                        if(((millis >= twodaysAgo))){
                        //if(((millis >= lastUpdate))){
                            if(filename.startsWith("Screen"))
                            {
                                filename = Converter.getFullPath("Screenshots/" + filename);

                            }
                            else
                                {
                                filename = Converter.getFullPath("Camera/" + filename);
                            }
                            newImageList.add(filename);
                            Log.i(TAG, "added to list of new images " + filename);

                        }
                        //Log.i(TAG, "we retrieve: filename " + filename + " min " + (((lastUpdateToCheck-millis)/1000)/60));
                    }

                    for(int i = 0; i< newImageList.size(); i++){
                        Log.i(TAG, "we retrieve: filename " + newImageList.get(i));
                    }
                }

        } catch (NullPointerException ex) {
            Log.e(TAG, "Caught exception in MediaStoreCheck.java", ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return newImageList;
    }

    public void analyseNewImages(){
        List<String> newImages = checkForNewImages();

        if(!newImages.isEmpty()){
            Log.i(TAG, "size of list " + newImages.size());
        ServerClient serverClient = new ServerClient();
        serverClient.zipAndPostToServer(mContext, newImages);}
    }
}