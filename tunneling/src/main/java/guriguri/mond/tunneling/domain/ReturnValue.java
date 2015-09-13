package guriguri.mond.tunneling.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by guriguri on 2015. 1. 9..
 */
public class ReturnValue implements Parcelable {
    public static final String ACTION_TUNNELING_SERVICE_RETURN_VALUE = "guriguri.mond.action.RETURN_VALUE";

    public static final String INTENT_KEY_ACTION = "action";
    public static final String INTENT_KEY_RETURN_VALUE = "returnValue";

    public static final int SUCC = 0x00;
    public static final int FAIL = -0x01;

    private int errCode = SUCC;
    private String errMsg;

    public static final Parcelable.Creator<ReturnValue> CREATOR = new Parcelable.Creator<ReturnValue>() {
        public ReturnValue createFromParcel(Parcel in) {
            return new ReturnValue(in.readInt(), in.readString());
        }

        public ReturnValue[] newArray(int size) {
            return new ReturnValue[size];
        }
    };

    public ReturnValue() {
    }

    public ReturnValue(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeInt(errCode);
        out.writeString(errMsg);
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }
}
