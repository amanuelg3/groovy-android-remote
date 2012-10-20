package com.linuxfunkar.mousekeysremote;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SimpleTextDialog extends Dialog {
	private TextView lbl;
	private EditText txt;

	private String oldText = "";

	private OnTextChangedListener listener;

	public SimpleTextDialog(Context context) {
		super(context);
	}

	public void show(String title, String label) {
		super.show();

		setTitle(title);
		lbl.setText(label);
		txt.setText(oldText);
	}

	public void show(String title, String label, String oldText) {
		this.oldText = oldText;

		show(title, label);
	}

	public void setOnTextChangedListener(OnTextChangedListener listener) {
		this.listener = listener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setCancelable(true);

		setContentView(R.layout.simple_text_dialog);

		lbl = (TextView) findViewById(R.id.stdialog_label);
		txt = (EditText) findViewById(R.id.stdialog_text);
		txt.addTextChangedListener(new TextWatcherAdapter() {
			@Override
			public void afterTextChanged(Editable s) {
				Button ok = (Button) findViewById(R.id.stdialog_pbOkay);
				ok.setEnabled(!s.toString().equals(oldText));
			}
		});

		Button ok = (Button) findViewById(R.id.stdialog_pbOkay);
		ok.setEnabled(false);
		ok.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.onTextChanged(txt.getText().toString());
				dismiss();
			}
		});

		Button cancel = (Button) findViewById(R.id.stdialog_pbCancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancel();
			}
		});
	}

	public interface OnTextChangedListener {
		void onTextChanged(String text);
	}
}
