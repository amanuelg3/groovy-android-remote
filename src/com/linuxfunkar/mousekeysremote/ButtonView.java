package com.linuxfunkar.mousekeysremote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.util.AttributeSet;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;

public class ButtonView extends TableLayout {
	private SharedPreferences prefs;
	private int layout = 0;
	private Button lastButton;
	private OnKeyListener keyListener;
	private AdapterView.AdapterContextMenuInfo menuInfo;

	public ButtonView(Context context) {
		super(context);
	}

	public ButtonView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private void init(Context context) {
		prefs = context.getSharedPreferences("MY_PREFS", Activity.MODE_PRIVATE);
		layout = prefs.getInt("layout", 0);
		int numberOfKeyRows = prefs.getInt("numberOfKeyRows" + layout, 7);
		int numberOfKeyCols = prefs.getInt("numberOfKeyCols" + layout, 7);

		createButtons(numberOfKeyRows, numberOfKeyCols);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		if (!isInEditMode())
			init(getContext());
	}

	private void createButtons(int rows, int cols) {
		this.setWeightSum(rows);
		for (int i = 0; i < rows; i++) {
			TableRow row = new TableRow(getContext());
			row.setWeightSum(cols);

			for (int j = 0; j < cols; j++) {
				Button b = new Button(getContext());
				int key_num = i * cols + j + 1;
				b.setText(Preferences.getInstance(getContext()).getKeyLabel(
						key_num));
				b.setTag(R.id.button_id, "" + key_num);
				b.setTag(R.id.button_sticky, Boolean.valueOf(Preferences
						.getInstance(getContext()).isButtonSticky(key_num)));
				b.setTag(Integer.valueOf(Preferences.getInstance(getContext())
						.getKeyValue(key_num)));

				b.setVisibility((Preferences.getInstance(getContext())
						.isButtonVisible(key_num)) ? View.VISIBLE
						: View.INVISIBLE);

				int color = Preferences.getInstance(getContext()).getKeyColor(
						key_num);
				if (color != -1)
					b.getBackground().setColorFilter(
							new LightingColorFilter(color,
									android.R.color.black));

				b.setClickable(false);
				b.setLongClickable(true);
				b.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						lastButton = (Button) v;
						showContextMenuForChild(v);
						return true;
					}
				});

				b.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						lastButton = (Button) v;

						int key_num = Integer.valueOf(lastButton.getTag(
								R.id.button_id).toString());
						boolean sticky = Preferences.getInstance(getContext())
								.isButtonSticky(key_num);

						if (sticky) {
							if (event.getAction() == MotionEvent.ACTION_UP) {
								if (event.getEventTime() - event.getDownTime() < 2000) {
									lastButton.setPressed(!lastButton
											.isPressed());
									if (lastButton.isPressed())
										lastButton.setText(Preferences
												.getInstance(getContext())
												.getKeyLabel(key_num)
												.toUpperCase());
									else
										lastButton.setText(Preferences
												.getInstance(getContext())
												.getKeyLabel(key_num));
								} else {
									showContextMenuForChild(v);
									return true;
								}
							}
							onTouchEvent(event);
							return true;
						}
						onTouchEvent(event);
						return false;
					}
				});

				row.addView(b);

				b.setLayoutParams(new TableRow.LayoutParams(
						ViewGroup.LayoutParams.FILL_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT, 1));
			}

			addView(row);

			row.setLayoutParams(new TableLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (lastButton != null) {
			int keyValue = Integer.valueOf(lastButton.getTag().toString())
					.intValue();

			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					onKeyListener(lastButton, keyValue, new KeyEvent(
							KeyEvent.ACTION_DOWN, keyValue));
					return true;
				case MotionEvent.ACTION_UP:
					onKeyListener(lastButton, keyValue, new KeyEvent(
							KeyEvent.ACTION_UP, keyValue));
					return true;
			}

			lastButton = null;
		}
		return false;
	}

	public void unhideAll() {
		unhideAll(this);
	}

	private void unhideAll(ViewGroup root) {
		int i = 0;
		View child = root.getChildAt(i++);
		while (child != null) {
			if (!(child instanceof ViewGroup)) {
				child.setVisibility(View.VISIBLE);
				if (child instanceof Button) {
					Preferences.getInstance(getContext()).setButtonVisible(
							Integer.valueOf(child.getTag(R.id.button_id)
									.toString()), true);
				}
			} else
				unhideAll((ViewGroup) child);
			child = root.getChildAt(i++);
		}
	}

	@Override
	public boolean showContextMenuForChild(View originalView) {
		menuInfo = new AdapterView.AdapterContextMenuInfo(originalView, -1, -1);
		return super.showContextMenuForChild(originalView);
	}

	@Override
	protected ContextMenuInfo getContextMenuInfo() {
		return menuInfo;
	}

	protected void onKeyListener(View view, int keyValue, KeyEvent event) {
		if (keyListener != null)
			keyListener.onKey(view, keyValue, event);
	}

	public void setOnKeyListener(OnKeyListener listener) {
		keyListener = listener;
	}
}
