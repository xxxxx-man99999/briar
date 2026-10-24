package org.briarproject.briar.android.view;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import org.briarproject.briar.R;
import org.briarproject.briar.android.view.EmojiTextInputView.OnKeyboardShownListener;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.util.Objects.requireNonNull;

@UiThread
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class TextInputView extends LinearLayout {

	@Nullable TextSendController textSendController;
	final EmojiTextInputView textInput;
	private final ImageButton voiceButton;
	private final View voiceRecordingBar;
	private final View compositeSendButton;
	private final Chronometer voiceRecordingTimer;

	public TextInputView(Context context) { this(context, null); }
	public TextInputView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }

	public TextInputView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setSaveEnabled(true);
		setOrientation(VERTICAL);
		setLayoutTransition(new LayoutTransition());
		LayoutInflater inflater = (LayoutInflater) requireNonNull(context.getSystemService(LAYOUT_INFLATER_SERVICE));
		inflater.inflate(getLayout(), this, true);
		TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.TextInputView);
		String hint = attributes.getString(R.styleable.TextInputView_hint);
		boolean allowEmptyText = attributes.getBoolean(R.styleable.TextInputView_allowEmptyText, false);
		attributes.recycle();
		textInput = findViewById(R.id.emojiTextInput);
		voiceButton = findViewById(R.id.voiceButton);
		voiceRecordingBar = findViewById(R.id.voiceRecordingBar);
		compositeSendButton = findViewById(R.id.compositeSendButton);
		voiceRecordingTimer = findViewById(R.id.voiceRecordingTimer);
		textInput.setAllowEmptyText(allowEmptyText);
		if (hint != null) textInput.setHint(hint);
	}

	@LayoutRes protected int getLayout() { return R.layout.text_input_view; }

	@Nullable @Override protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		if (textSendController != null) superState = textSendController.onSaveInstanceState(superState);
		return superState;
	}

	@Override protected void onRestoreInstanceState(Parcelable state) {
		if (textSendController != null) super.onRestoreInstanceState(textSendController.onRestoreInstanceState(state));
		else super.onRestoreInstanceState(state);
	}

	public <T extends TextSendController> void setSendController(T controller) {
		textSendController = controller;
		textInput.setTextInputListener(textSendController);
	}
	public void setOnVoiceClickListener(@Nullable OnClickListener listener) { voiceButton.setOnClickListener(listener); }
	public void setVoiceRecording(boolean recording) {
		voiceButton.setImageResource(recording ? android.R.drawable.ic_menu_close_clear_cancel : android.R.drawable.ic_btn_speak_now);
		voiceButton.setContentDescription(recording ? "Stop voice recording" : "Record voice message");
		voiceRecordingBar.setVisibility(recording ? VISIBLE : GONE);
		textInput.setVisibility(recording ? GONE : VISIBLE);
		compositeSendButton.setVisibility(recording ? GONE : VISIBLE);
		if (recording) {
			voiceRecordingTimer.setBase(SystemClock.elapsedRealtime());
			voiceRecordingTimer.start();
		} else {
			voiceRecordingTimer.stop();
		}
	}
	public void setVoiceEnabled(boolean enabled) { voiceButton.setEnabled(enabled); }
	public ImageButton getVoiceButton() { return voiceButton; }
	public EmojiTextInputView getEmojiTextInputView() { return textInput; }
	@Override public void setEnabled(boolean enabled) { throw new RuntimeException("Use controllers to enable/disable"); }
	public void setReady(boolean ready) { requireNonNull(textSendController).setReady(ready); }
	@Override public boolean requestFocus(int direction, Rect previouslyFocusedRect) { return textInput.requestFocus(direction, previouslyFocusedRect); }
	public void clearText() { textInput.clearText(); }
	public void setHint(@StringRes int res) { textInput.setHint(getContext().getString(res)); }
	public void setMaxTextLength(int maxLength) { textInput.setMaxLength(maxLength); }
	public boolean isKeyboardOpen() { return textInput.isKeyboardOpen(); }
	public void showSoftKeyboard() { textInput.showSoftKeyboard(); }
	public void hideSoftKeyboard() { textInput.hideSoftKeyboard(); }
	public void setOnKeyboardShownListener(@Nullable OnKeyboardShownListener listener) { textInput.setOnKeyboardShownListener(listener); }
}
