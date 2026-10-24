package org.briarproject.briar.android.conversation;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.briarproject.briar.R;
import org.briarproject.briar.android.attachment.AttachmentItem;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

@UiThread
@NotNullByDefault
final class VoiceViewHolder extends RecyclerView.ViewHolder {

    private final ImageButton playButton;
    private final TextView title;
    private final TextView meta;

    VoiceViewHolder(View view) {
        super(view);
        playButton = view.findViewById(R.id.playVoice);
        title = view.findViewById(R.id.voiceTitle);
        meta = view.findViewById(R.id.voiceMeta);
    }

    void bind(AttachmentItem item, View.OnClickListener listener) {
        title.setText("Voice message");
        meta.setText("Tap to play");
        playButton.setOnClickListener(listener);
        itemView.setOnClickListener(listener);
    }
}
