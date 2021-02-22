package ng.com.obkm.exquisitor.DataAccess;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class NegativeImageLab {

    private static NegativeImageLab sNegativeImageLab;

    private Context mContext;
    private static List<String> mNegativePaths;

    public static NegativeImageLab get(Context context) {
        if (sNegativeImageLab == null) {
            sNegativeImageLab = new NegativeImageLab(context);
        }
        return sNegativeImageLab;
    }

    private NegativeImageLab(Context context) {
        mContext = context.getApplicationContext();
        mNegativePaths = new ArrayList<>();
    }

    public List<String> getNegativeImagePaths() {
        return mNegativePaths;
    }

    public void addNegativeImage(String path) {
        mNegativePaths.add(path);
    }

    public void removeNegativeImage(String path) {

        mNegativePaths.remove(path);
    }

    public static void resetNegList(){
        mNegativePaths.clear();
    }

    public int getNegativeLabSize() {
        return mNegativePaths.size();
    }
}
