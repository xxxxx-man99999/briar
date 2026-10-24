package org.briarproject.briar.android.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.customview.view.AbsSavedState;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.briarproject.briar.R;
import org.briarproject.bramble.voice.AndroidVoiceRecorder;
import org.briarproject.briar.android.attachment.AttachmentItemResult;
import org.briarproject.briar.android.attachment.AttachmentManager;
import org.briarproject.briar.android.attachment.AttachmentResult;
import org.briarproject.briar.android.view.ImagePreview.ImagePreviewListener;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.widget.Toast.LENGTH_LONG;
import static android.view.View.GONE;
import static androidx.customview.view.AbsSavedState.EMPTY_STATE;
import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static android.Manifest.permission.RECORD_AUDIO;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.briarproject.briar.android.view.TextSendController.SendState.SENT;
import static org.briarproject.briar.api.messaging.MessagingConstants.MAX_ATTACHMENTS_PER_MESSAGE;

@UiThread
@NotNullByDefault
public class TextAttachmentController extends TextSendController implements ImagePreviewListener {
	private static final int REQUEST_VOICE_PERMISSION = 4701;
	private final ImagePreview imagePreview;
	private final AttachmentListener attachmentListener;
	private final CompositeSendButton sendButton;
	private final AttachmentManager attachmentManager;
	private final AndroidVoiceRecorder voiceRecorder = new AndroidVoiceRecorder();
	private final List<Uri> imageUris = new ArrayList<>();
	private boolean loadingUris = false;
	private boolean recordingVoice = false;
	@Nullable private File voiceOutput;

	public TextAttachmentController(TextInputView v, ImagePreview imagePreview,
			AttachmentListener attachmentListener, AttachmentManager attachmentManager) {
		super(v, attachmentListener, false);
		this.attachmentListener = attachmentListener;
		this.imagePreview = imagePreview;
		this.attachmentManager = attachmentManager;
		this.imagePreview.setImagePreviewListener(this);
		sendButton = (CompositeSendButton) compositeSendButton;
		sendButton.setOnImageClickListener(view -> onImageButtonClicked());
		v.setOnVoiceClickListener(view -> onVoiceButtonClicked());
	}
	@Override protected void updateViewState() {
		super.updateViewState();
		if (loadingUris) sendButton.showProgress(true);
		else if (imageUris.isEmpty()) {
			sendButton.showProgress(false);
			sendButton.showImageButton(textIsEmpty, isSendButtonEnabled());
		} else {
			sendButton.showProgress(false);
			sendButton.showImageButton(false, isSendButtonEnabled());
		}
	}
	@Override protected boolean isTextInputEnabled() { return super.isTextInputEnabled() && !loadingUris && !recordingVoice; }
	@Override protected boolean isSendButtonEnabled() { return super.isSendButtonEnabled() && !loadingUris && !recordingVoice; }
	@Override protected boolean isBombVisible() { return super.isBombVisible() && (!textIsEmpty || !imageUris.isEmpty()); }
	@Override protected CharSequence getCurrentTextHint() {
		if (imageUris.isEmpty()) return super.getCurrentTextHint();
		return textInput.getContext().getString(R.string.image_caption_hint);
	}
	@Override public void onSendEvent() {
		if (canSend()) {
			if (loadingUris || recordingVoice) throw new AssertionError();
			listener.onSendClick(textInput.getText(), attachmentManager.getAttachmentHeadersForSending(), expectedTimer)
					.observe(attachmentListener, this::onSendStateChanged);
		}
	}
	@Override protected void onSendStateChanged(SendState sendState) {
		super.onSendStateChanged(sendState);
		if (sendState == SENT) reset();
	}
	@Override protected boolean canSendEmptyText() { return !imageUris.isEmpty(); }
	public void setImagesSupported() { sendButton.setImagesSupported(); }

	private void onVoiceButtonClicked() { if (recordingVoice) stopVoiceRecording(); else startVoiceRecording(); }
	private void startVoiceRecording() {
		Context context = textInput.getContext();
		if (!(context instanceof Activity)) {
			Toast.makeText(context, "Voice recording is unavailable", LENGTH_LONG).show();
			return;
		}
		Activity activity = (Activity) context;
		if (checkSelfPermission(context, RECORD_AUDIO) != PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(activity, new String[] {RECORD_AUDIO}, REQUEST_VOICE_PERMISSION);
			Toast.makeText(context, "Allow microphone access, then tap the microphone again", LENGTH_LONG).show();
			return;
		}
		try {
			voiceOutput = new File(context.getCacheDir(), "voice-" + System.currentTimeMillis() + ".m4a");
			voiceRecorder.start(voiceOutput);
			recordingVoice = true;
			onStartingMessage();
			textInput.setVoiceRecording(true);
			updateViewState();
		} catch (Exception e) {
			if (voiceOutput != null) voiceOutput.delete();
			voiceOutput = null;
			Toast.makeText(context, "Unable to start voice recording", LENGTH_LONG).show();
		}
	}
	private void stopVoiceRecording() {
		Context context = textInput.getContext();
		try {
			voiceRecorder.stop();
			recordingVoice = false;
			textInput.setVoiceRecording(false);
			updateViewState();
			File output = requireNonNull(voiceOutput);
			LiveData<AttachmentResult> result = attachmentManager.storeAttachments(singletonList(Uri.fromFile(output)), false);
			result.observe(attachmentListener, new Observer<AttachmentResult>() {
				@Override public void onChanged(@Nullable AttachmentResult attachmentResult) {
					if (attachmentResult == null) { result.removeObserver(this); return; }
					for (AttachmentItemResult itemResult : attachmentResult.getItemResults()) {
						if (itemResult.hasError()) {
							attachmentManager.cancel(); output.delete(); voiceOutput = null;
							Toast.makeText(context, "Voice attachment failed", LENGTH_LONG).show();
							result.removeObserver(this); return;
						}
					}
					if (attachmentResult.isFinished()) {
						result.removeObserver(this);
						listener.onSendClick(null, attachmentManager.getAttachmentHeadersForSending(), expectedTimer)
								.observe(attachmentListener, state -> onVoiceSendStateChanged(state, output));
					}
				}
			});
		} catch (Exception e) {
			recordingVoice = false;
			textInput.setVoiceRecording(false);
			updateViewState();
			if (voiceOutput != null) voiceOutput.delete();
			voiceOutput = null;
			attachmentManager.cancel();
			Toast.makeText(context, "Unable to finish voice recording", LENGTH_LONG).show();
		}
	}
	private void onVoiceSendStateChanged(SendState state, File output) {
		onSendStateChanged(state);
		if (state == SENT || state == SendState.ERROR) {
			output.delete(); voiceOutput = null;
			if (state == SendState.ERROR) attachmentManager.cancel();
		}
	}
	private void onImageButtonClicked() {
		if (!sendButton.hasImageSupport()) {
			Context ctx = imagePreview.getContext();
			Builder builder = new Builder(ctx, R.style.OnboardingDialogTheme);
			builder.setTitle(ctx.getString(R.string.dialog_title_no_image_support));
			builder.setMessage(ctx.getString(R.string.dialog_message_no_image_support));
			builder.setPositiveButton(R.string.got_it, null); builder.show(); return;
		}
		if (attachmentListener.getLifecycle().getCurrentState() != DESTROYED) attachmentListener.onAttachImageClicked();
	}
	public void onImageReceived(@Nullable List<Uri> newUris) {
		if (newUris == null) return;
		if (loadingUris || !imageUris.isEmpty() || recordingVoice) throw new AssertionError();
		onNewUris(false, newUris);
	}
	private void onNewUris(boolean restart, List<Uri> newUris) {
		if (newUris.isEmpty()) return;
		if (loadingUris) throw new AssertionError();
		if (textIsEmpty) onStartingMessage();
		loadingUris = true;
		if (newUris.size() > MAX_ATTACHMENTS_PER_MESSAGE) { newUris = newUris.subList(0, MAX_ATTACHMENTS_PER_MESSAGE); attachmentListener.onTooManyAttachments(); }
		imageUris.addAll(newUris); updateViewState(); imagePreview.showPreview(ImagePreviewItem.fromUris(imageUris));
		LiveData<AttachmentResult> result = attachmentManager.storeAttachments(imageUris, restart);
		result.observe(attachmentListener, new Observer<AttachmentResult>() {
			@Override public void onChanged(@Nullable AttachmentResult attachmentResult) {
				if (attachmentResult == null) result.removeObserver(this);
				else { boolean noError = onNewAttachmentItemResults(attachmentResult.getItemResults());
					if (noError && attachmentResult.isFinished()) { onAllAttachmentsCreated(); result.removeObserver(this); }
				}
			}
		});
	}
	private boolean onNewAttachmentItemResults(Collection<AttachmentItemResult> itemResults) {
		if (!loadingUris) throw new AssertionError();
		for (AttachmentItemResult result : itemResults) { if (result.hasError()) { onError(result.getErrorMsg()); return false; } imagePreview.loadPreviewImage(result); }
		return true;
	}
	private void onAllAttachmentsCreated() { if (!loadingUris) throw new AssertionError(); loadingUris = false; updateViewState(); }
	private void reset() { imagePreview.setVisibility(GONE); imageUris.clear(); loadingUris = false; updateViewState(); }
	@Override public Parcelable onSaveInstanceState(@Nullable Parcelable superState) { SavedState state = new SavedState(superState == null ? EMPTY_STATE : superState); state.imageUris = imageUris; return state; }
	@Override @Nullable public Parcelable onRestoreInstanceState(Parcelable inState) { SavedState state = (SavedState) inState; if (!imageUris.isEmpty()) throw new AssertionError(); if (state.imageUris != null) onNewUris(true, state.imageUris); return state.getSuperState(); }
	@Override public void onCancel() {
		textInput.clearText();
		if (recordingVoice) { voiceRecorder.cancel(); recordingVoice = false; textInput.setVoiceRecording(false); }
		if (voiceOutput != null) { voiceOutput.delete(); voiceOutput = null; }
		attachmentManager.cancel(); reset();
	}
	@UiThread private void onError(@Nullable String errorMsg) {
		if (errorMsg == null) errorMsg = imagePreview.getContext().getString(R.string.image_attach_error);
		Toast.makeText(textInput.getContext(), errorMsg, LENGTH_LONG).show(); onCancel();
	}
	public void showImageOnboarding(Activity activity) {
		int color = ContextCompat.getColor(activity, R.color.briar_primary);
		Drawable drawable = VectorDrawableCompat.create(activity.getResources(), R.drawable.ic_image, null);
		new MaterialTapTargetPrompt.Builder(activity, R.style.OnboardingDialogTheme).setTarget(sendButton)
				.setPrimaryText(R.string.dialog_title_image_support).setSecondaryText(R.string.dialog_message_image_support)
				.setBackgroundColour(color).setIconDrawable(drawable).show();
	}
	private static class SavedState extends AbsSavedState {
		@Nullable private List<Uri> imageUris;
		private SavedState(Parcelable superState) { super(superState); }
		private SavedState(Parcel in) { super(in); //noinspection unchecked
			imageUris = in.readArrayList(Uri.class.getClassLoader()); }
		@Override public void writeToParcel(Parcel out, int flags) { super.writeToParcel(out, flags); out.writeList(imageUris); }
		public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
			@Override public SavedState createFromParcel(Parcel in) { return new SavedState(in); }
			@Override public SavedState[] newArray(int size) { return new SavedState[size]; }
		};
	}
	@UiThread public interface AttachmentListener extends SendListener { void onAttachImageClicked(); void onTooManyAttachments(); }
}
