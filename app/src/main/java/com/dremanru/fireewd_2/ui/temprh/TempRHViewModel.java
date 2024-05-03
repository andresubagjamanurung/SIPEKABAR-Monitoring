package com.dremanru.fireewd_2.ui.temprh;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TempRHViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public TempRHViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}