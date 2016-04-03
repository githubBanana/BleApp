package com.diy.blelib.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.diy.blelib.R;

/**
 * Created by siushen on 2016/3/22.
 */
public class ProgressTwoDialog extends DialogFragment {

    public static String Tag = ProgressTwoDialog.class.getSimpleName();
    public static ProgressTwoDialog getInstance(String message){
        ProgressTwoDialog p = new ProgressTwoDialog();
        Bundle bundle = new Bundle();
        bundle.putString("message",message);
        p.setArguments(bundle);
        return p;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AlertDialogTwoStyle);
    }

    private View view;
    private TextView tv;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.progresstwo_layout,container,false);
        String message = getArguments().getString("message");
        tv = (TextView) view.findViewById(R.id.progress_tv);
        if (message == null)
            tv.setVisibility(View.GONE);
        else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(message);
        }
        return view;
    }


    @Override
    public void dismiss() {
        super.dismiss();
    }
}
