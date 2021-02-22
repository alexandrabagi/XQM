package ng.com.obkm.exquisitor.Entity;

public class Candidate {

    Integer mImageID;
    Double mDistance;

    public Candidate(Integer imageID, Double distance){
        this.mImageID = imageID;
        this.mDistance = distance;
    }

    public Integer getImageID() {
        return mImageID;
    }

    public void setImageID(int id) {
        mImageID = id;
    }

    public Double getDistance() {
        return mDistance;
    }

    public void setDistance(double distance) {
        mDistance = distance;
    }
}
