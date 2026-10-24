package org.briarproject.briar.android.conversation;

import android.view.View;
import android.widget.ImageButton;

import org.briarproject.briar.R;
import org.briarproject.briar.android.attachment.AttachmentItem;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

@UiThread
@NotNullByDefault
final class VoiceViewHolder extends RecyclerView.ViewHolder {

	private final ImageButton playButton;

	VoiceViewHolder(View view) {
		super(view);
		playButton = view.findViewById(R.id.playVoice);
	}

	void bind(AttachmentItem item, View.OnClickListener listener) {
		playButton.setOnClickListener(listener);
		itemView.setOnClickListener(listener);
	}
}
