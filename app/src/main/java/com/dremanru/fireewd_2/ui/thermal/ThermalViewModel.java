package com.dremanru.fireewd_2.ui.thermal;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ThermalViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ThermalViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is notifications fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}