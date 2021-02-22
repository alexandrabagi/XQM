package ng.com.obkm.exquisitor.DataAccess;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ng.com.obkm.exquisitor.Database.IDPathSchema;
import ng.com.obkm.exquisitor.Database.VectorBaseHelper;
import ng.com.obkm.exquisitor.Database.VectorCursorWrapper;
import ng.com.obkm.exquisitor.Database.VectorDBSchema;
import ng.com.obkm.exquisitor.Database.VectorDBSchema.VectorTable;
import ng.com.obkm.exquisitor.Fragments.HomeFragment;

public class VectorLab {

    private static final String TAG = "VectorLab";
    private static VectorLab sVectorLab;

    private Context mContext;
    private static SQLiteDatabase mDatabase;

    public static VectorLab get(Context context) {
        if (sVectorLab == null) {
            sVectorLab = new VectorLab(context);
        }
        return sVectorLab;
    }

    private VectorLab(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new VectorBaseHelper(mContext)
                .getWritableDatabase();
    }

    public void addLabelProbToVectors(int imageID, int label, float prob){
        ContentValues values = getContentValuesForLabelProb(imageID, label, prob);
        mDatabase.insert(VectorTable.NAME, null, values);
    }

    private static VectorCursorWrapper queryItemsVectors(String whereClause, String[] whereArgs){
        Cursor cursor = mDatabase.query(
                VectorTable.NAME,
                null, // columns - null selects all columns
                whereClause,
                whereArgs,
                null, // groupBy
                null, // having
                null  // orderBy
        );
        return new VectorCursorWrapper(cursor);
    }

    private static VectorCursorWrapper queryItemsVectorsRandom(String whereClause, String[] whereArgs, String orderByArguments){
        Cursor cursor = mDatabase.query(
                VectorTable.NAME,
                null, // columns - null selects all columns
                whereClause,
                whereArgs,
                null, // groupBy
                null, // having
                orderByArguments  // orderBy
        );
        return new VectorCursorWrapper(cursor);
    }

    private static VectorCursorWrapper queryItemsIDPath(String whereClause, String[] whereArgs){
        Cursor cursor = mDatabase.query(
                IDPathSchema.IDPathTable.NAME,
                null, // columns - null selects all columns
                whereClause,
                whereArgs,
                null, // groupBy
                null, // having
                null  // orderBy
        );
//        Log.i("home", "VectorLab are we looking in idpath schema db ? " +  IDPathSchema.IDPathTable.NAME);
        return new VectorCursorWrapper(cursor);
    }

    public static float[] queryProbsAsFloats(int imageID) {
        String imageIDS = String.valueOf(imageID);
        float[] probsArray = new float[HomeFragment.NUMBER_OF_HIGHEST_PROBS];
        int i = 0;
        String WHERE = "CAST("+IDPathSchema.IDPathTable.Cols.IMAGEID+" as TEXT) = ?";
        VectorCursorWrapper cursor = queryItemsVectors(
                WHERE,
                new String[]{imageIDS}
        );
        try {
            while(cursor.moveToNext()) {
                probsArray[i] = cursor.getFloatProbs();
                i++;
            }
            return probsArray;
        } finally {
            cursor.close();
        }
    }

    public static int[] queryLabelsAsInts(int imageID) {
//        Log.i(TAG, "query one float is called");
        long startTime = System.nanoTime();
        int[] labelsArray = new int[HomeFragment.NUMBER_OF_HIGHEST_PROBS];
        int i = 0;
        //String WHERE = IDPathSchema.IDPathTable.Cols.IMAGEID+ " = ?";
        String WHERE = "CAST("+IDPathSchema.IDPathTable.Cols.IMAGEID+" as TEXT) = ?";

        VectorCursorWrapper cursor = queryItemsVectors(
                WHERE,
                new String[]{String.valueOf(imageID)});
        try {
            while(cursor.moveToNext()) {
                labelsArray[i] = cursor.getFeatureInts();
                i++;
            }
            return labelsArray;
        } finally {
            cursor.close();
//            long elapsedTime = System.nanoTime() - startTime;
//            System.out.println("Elapsed time queryLabelsAsInts nanosec: " + elapsedTime);
        }
    }

    // Step one: list of paths
    // Step two: SELECT feature FROM db WHERE imagePath = path;
    // Loop iterate through list

    public Map<Integer, List<Float>> queryUnseenProbs(){
        Map<Integer,List<Float>> unseenProbs = new HashMap<>();
        String WHERE = "CAST("+ VectorDBSchema.VectorTable.Cols.SEEN+" as TEXT) = ?";
        VectorCursorWrapper cursor = queryItemsVectors(
                WHERE,
                new String[]{String.valueOf(0)}
        );
        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            List<Float> list;
            while (cursor.moveToNext()) {
//                String path = cursor.getPath();
                int imageID = cursor.getImageIDVectorsTable();
                if (!unseenProbs.containsKey(imageID)) {
                    list = new ArrayList<Float>();
                    list.add(cursor.getFloatProbs());
                    unseenProbs.put(cursor.getImageIDVectorsTable(), list);
                } else {
                    list = unseenProbs.get(imageID);
                    list.add(cursor.getFloatProbs());
                    unseenProbs.put(imageID, list);
                }
            }
            return unseenProbs;
        }
        finally {
            cursor.close();
        }
    }

    public Map<Integer, List<Integer>> queryUnseenFeatures(){
        Map<Integer, List<Integer>> unseenLabels = new HashMap<>();
        String WHERE = "CAST("+ VectorDBSchema.VectorTable.Cols.SEEN+" as TEXT) = ?";
        VectorCursorWrapper cursor = queryItemsVectors(
                WHERE,
                new String[]{String.valueOf(0)}
        );
        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            List<Integer> list;
            while(cursor.moveToNext()){
                int imageID = cursor.getImageIDVectorsTable();
//                Log.i(TAG, "keyset " + unseenLabels.entrySet());
                if (!unseenLabels.containsKey(imageID))
                {
                    list =  new ArrayList<Integer>();
                    list.add(cursor.getFeatureInts());
                    unseenLabels.put(imageID, list);
                }
                else {
                    list = unseenLabels.get(imageID);
                    list.add(cursor.getFeatureInts());
                    unseenLabels.put(imageID, list);
                }
            }
            return unseenLabels;
        } finally {
            cursor.close();
        }
    }

   /* private static ContentValues getContentValuesForLabelProb(int imageID, int topLabels, float topProbs) {
//        Log.i("DB", "getContentValuesForLabelProb was called");
        ContentValues values = new ContentValues();
        values.put("CAST("+IDPathSchema.IDPathTable.Cols.IMAGEID+" as TEXT) = ?", imageID);
        values.put("CAST("+VectorTable.Cols.SEEN+" as TEXT) = ?", 0);
        values.put(VectorTable.Cols.FEATURES, topLabels);
        values.put(VectorTable.Cols.PROBS, topProbs);
        return values;
    }*/

    private static ContentValues getContentValuesForLabelProb(int imageID, int topLabels, float topProbs) {
//        Log.i("DB", "getContentValuesForLabelProb was called");
        ContentValues values = new ContentValues();
        values.put(VectorTable.Cols.IMAGEID, imageID);
        values.put(VectorTable.Cols.SEEN, 0);
        values.put(VectorTable.Cols.FEATURES, topLabels);
        values.put(VectorTable.Cols.PROBS, topProbs);
        return values;
    }

    public void updateSeen(int imageID){
       //Log.i("here", "updateRated was called" + imageID);
//        Log.i("DB", "update imageID: " + imageID);
        ContentValues values = new ContentValues();
        values.put("seen", 1);
//        Log.i("DB", "table name " + VectorTable.NAME);
//        Log.i("DB", "values " + values);
//        Log.i("DB", "column " + VectorTable.Cols.IMAGEID);
        mDatabase.update(
                VectorTable.NAME, values, "CAST("+IDPathSchema.IDPathTable.Cols.IMAGEID+" as TEXT) = ?", new String[] {String.valueOf(imageID)});
    }

    public void removeSeen() {
        ContentValues values = new ContentValues();
        //Log.i("home", "remove rated is called.");
        values.put("seen", 0);
        mDatabase.update(VectorTable.NAME, values, "CAST("+VectorTable.Cols.SEEN+" as TEXT)" + "= ?", new String[] {String.valueOf(1)});
    }

    public static void removeSeenForID(int id){
        ContentValues values = new ContentValues();
//        Log.i("DB", "remove rated is called.");
        values.put("seen", 0);
        mDatabase.update(VectorTable.NAME, values, "CAST("+IDPathSchema.IDPathTable.Cols.IMAGEID+" as TEXT) = ? AND " + "CAST("+VectorTable.Cols.SEEN+" as TEXT) = ?", new String[] {String.valueOf(id), String.valueOf(1)});
    }

    /*public void addIDPathPairs(int imageID, String path) {
        ContentValues values = new ContentValues();
        values.put("CAST("+IDPathSchema.IDPathTable.Cols.IMAGEID+" as TEXT) = ?", imageID);
        values.put(IDPathSchema.IDPathTable.Cols.PATH, path);
        mDatabase.insert(IDPathSchema.IDPathTable.NAME, null, values);
    }*/

    public void addIDPathPairs(int imageID, String path) {
        Log.i(TAG, "addIDPathPairs was called " + path + "imageID " + imageID);
        ContentValues values = new ContentValues();
        values.put(IDPathSchema.IDPathTable.Cols.IMAGEID, imageID);
        values.put(IDPathSchema.IDPathTable.Cols.PATH, path);
        mDatabase.insert(IDPathSchema.IDPathTable.NAME, null, values);
    }

    public static int getIDFromPath(String path) {
        //Log.i("DB", "getIDFromPath was called" + path);
        VectorCursorWrapper cursor = queryItemsIDPath(
                IDPathSchema.IDPathTable.Cols.PATH + " = ?",
                new String[]{path}
        );
        try {
            if (cursor.getCount() == 0) {
                return -1;
            }
            cursor.moveToFirst();
            return cursor.getImageIDIDPathTable();
        } finally {
            cursor.close();
        }
    }

    public String getPathFromID(int imageID) {
        String idString = String.valueOf(imageID);
        String WHERE = "CAST("+IDPathSchema.IDPathTable.Cols.IMAGEID+" as TEXT) = ?";
        VectorCursorWrapper cursor = queryItemsIDPath(
                WHERE,
                new String[]{idString}
        );
        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getImagePathIDPathTable();
        } finally {
            cursor.close();
        }
    }

    public int queryRandomUnseen() {
        String WHERE = "CAST(" + VectorDBSchema.VectorTable.Cols.SEEN + " as TEXT) =? ";
        String ORDERBY = "RANDOM() LIMIT 1";

        VectorCursorWrapper cursor = queryItemsVectorsRandom(
                WHERE,
                new String[]{String.valueOf(0)},
                ORDERBY
        );
        System.out.println("!!!!!!!!!!!!!!!!!! cursor count: " + cursor.getCount());
        try {
            if (cursor.getCount() == 0) {
                return -1;
            }
            cursor.moveToFirst();
            return cursor.getImageIDVectorsTable();
        } finally {
            cursor.close();
        }
    }

    public void deleteEntryFromDB(String path) {
        System.out.println("deleteEntryFromDB was called");
        System.out.println("path in deleteEntry: " + path);
        int imageID = -1;
        VectorCursorWrapper cursor = queryItemsIDPath(
                IDPathSchema.IDPathTable.Cols.PATH + " = ?",
                new String[]{path}
        );
        try {
            if (cursor.getCount() == 0) {
                return;
            }
            cursor.moveToFirst();
            imageID = cursor.getImageIDIDPathTable();
        } finally {
            cursor.close();
        }
        String idString = String.valueOf(imageID);
        String WHERE = "CAST("+IDPathSchema.IDPathTable.Cols.IMAGEID+" as TEXT) = ?";
        System.out.println("imageID in deleteEntry: " + imageID);
        mDatabase.delete(VectorTable.NAME,
                WHERE,
                new String[]{idString});
        mDatabase.delete(IDPathSchema.IDPathTable.NAME,
                IDPathSchema.IDPathTable.Cols.PATH + " = ?",
                new String[]{path});
    }

    // Source: https://stackoverflow.com/questions/35333864/get-the-max-of-id-and-store-value-in-sqlite-db
    public int getLastImageID() {
//        VectorCursorWrapper cursor = queryItemsIDPath(
//                IDPathSchema.IDPathTable.Cols.IMAGEID + " = ?",
//                new String[]{"MAX("+ IDPathSchema.IDPathTable.Cols.IMAGEID +")"}
//        );
//        try {
//            if (cursor.getCount() == 0) {
//                return -1;
//            }
//            cursor.moveToFirst();
//            return cursor.getImageIDIDPathTable();
//        } finally {
//            cursor.close();
//        }
        int maxID = -1;
        Cursor cursor = mDatabase.rawQuery("Select max(IMAGEID) from IDPathTable",null);
        if (cursor != null) {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()){
                maxID = cursor.getInt(0);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return maxID;
    }
}
