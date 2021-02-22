package ng.com.obkm.exquisitor.Database;

import android.database.Cursor;
import android.database.CursorWrapper;


import static ng.com.obkm.exquisitor.Database.VectorDBSchema.*;

public class VectorCursorWrapper extends CursorWrapper {

    /**
     * Creates a cursor wrapper.
     *
     * @param cursor The underlying cursor to wrap.
     */
    public VectorCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public float getFloatProbs() {
        float prob = getFloat(getColumnIndex(VectorTable.Cols.PROBS));
        return prob;
    }

    public int getFeatureInts() {
        return getInt(getColumnIndex(VectorTable.Cols.FEATURES));
    }

    public int getImageIDVectorsTable() {
        return getInt(getColumnIndex(VectorTable.Cols.IMAGEID));
    }

    public int getImageIDIDPathTable() {
        return getInt(getColumnIndex(IDPathSchema.IDPathTable.Cols.IMAGEID));
    }

    public String getImagePathIDPathTable() {
        return getString(getColumnIndex(IDPathSchema.IDPathTable.Cols.PATH));
    }
}
