package com.wirehall.audiorecorder.setting.pathpref;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Environment;
import androidx.preference.DialogPreference;
import android.util.AttributeSet;

public class PathPreference extends DialogPreference {

    public PathPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PathPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PathPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public PathPreference(Context context) {
        super(context);
    }

    @Override
    public void onAttached() {
        super.onAttached();

        // set the summery to persisted path if available otherwise set empty
        String value = getPersistedString("");
        setSummary(getPersistedString(value));
    }

    /**
     * Called when a Preference is being inflated and the default value attribute needs to be read
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        String s = a.getString(index);
        if (s == null)
            s = Environment.getExternalStorageDirectory().getAbsolutePath();
        return s;
    }

    @Override
    protected boolean persistString(String value) {
        boolean isPersisted = super.persistString(value);
        if (isPersisted) {
            this.notifyChanged();
        }
        return isPersisted;
    }

}
