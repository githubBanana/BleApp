package com.diy.blelib.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.diy.blelib.R;

import org.w3c.dom.Text;

/**
 * Created by siushen on 2016/3/22.
 */
public class ProgressDialog extends DialogFragment {

    public static String Tag = ProgressDialog.class.getSimpleName();
    public static ProgressDialog getInstance(String message){
        ProgressDialog p = new ProgressDialog();
        Bundle bundle = new Bundle();
        bundle.putString("message",message);
        p.setArguments(bundle);
        return p;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AlertDialogStyle);
    }

    private View view;
    private TextView tv;
  /*  @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        view = LayoutInflater.from(getActivity()).inflate(R.layout.progress_layout,null);
        AlertDialog alertDialog = builder.setView(view).create();
      *//*  WindowManager.LayoutParams lp=alertDialog.getWindow().getAttributes();
        lp.alpha=0.5f;
        alertDialog.getWindow().setAttributes(lp);*//*

        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.progress_layout);
        return alertDialog;

//        return alertDialog;
    }*/
    //用 alertdialog 修改不了默认的白色
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.progress_layout,container,false);
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
