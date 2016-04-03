package com.diy.blelib.dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.diy.blelib.R;

/**
 * @version V1.0 <提示弹框>
 * @author: Xs
 * @date: 2016-03-25 15:20
 */
public class TipsDialog extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "TipsDialog";
    private static OnclickTipsDialog mOnclickTip;

    public static TipsDialog getInstance (String title,String message,OnclickTipsDialog mOnclickTipsDialog) {
        mOnclickTip = mOnclickTipsDialog;
        TipsDialog t = new TipsDialog();
        Bundle b = new Bundle();
        b.putString("title",title);
        b.putString("message", message);
        t.setArguments(b);
        return t;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setCancelable(false);
        this.setStyle(DialogFragment.STYLE_NORMAL, R.style.TipsDialogStyle);
    }

    private View view;
    private Button tip_negative_btn,tip_positive_btn;
    private TextView tip_title,tip_message;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.tips_layout,container,false);
        tip_message = (TextView) view.findViewById(R.id.tip_message);
        tip_title = (TextView) view.findViewById(R.id.tip_title);
        view.findViewById(R.id.tip_negative_btn).setOnClickListener(this);
        view.findViewById(R.id.tip_positive_btn).setOnClickListener(this);
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");
        if(title == null)
            tip_title.setVisibility(View.GONE);
         else
            tip_title.setText(title);

        tip_message.setText(message);

        return view;
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tip_positive_btn) {
            mOnclickTip.confirm();
        }
        if (id == R.id.tip_negative_btn) {
            mOnclickTip.cancle();
        }
        dismiss();
    }
    public interface OnclickTipsDialog {
        void confirm();
        void cancle();
    }
}
