package com.example.ch34demo.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.example.ch34demo.R;


public class CustomTextView extends AppCompatTextView {

    public CustomTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEnabled(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if(enabled){
            setTextColor(ContextCompat.getColor(getContext(), R.color.blue));
        }else {
            setTextColor(ContextCompat.getColor(getContext(), R.color.grey));
        }
        super.setEnabled(enabled);
    }

}
