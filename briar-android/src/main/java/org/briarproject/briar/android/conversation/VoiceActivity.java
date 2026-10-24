package org.briarproject.briar.android.conversation;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BriarActivity;
import org.briarproject.briar.android.attachment.AttachmentItem;
import org.briarproject.briar.api.attachment.Attachment;
import org.briarproject.briar.api.attachment.AttachmentReader;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.inject.Inject;

import androidx.annotation.Nullable;

import static java.util.Objects.requireNonNull;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class VoiceActivity extends BriarActivity {

	public static final String ATTACHMENT = "attachment";

	@Inject
	AttachmentReader attachmentReader;

	private MediaPlayer player;
	private Button playButton;
	private TextView status;
	@Nullable private File playbackFile;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_voice);
		playButton = findViewById(R.id.playVoiceButton);
		status = findViewById(R.id.voiceStatus);
		AttachmentItem item = requireNonNull(getIntent().getParcelableExtra(ATTACHMENT));
		playButton.setOnClickListener(v -> togglePlayback());
		loadAttachment(item);
	}

	private void loadAttachment(AttachmentItem item) {
		status.setText("Loading voice message…");
		runOnDbThread(() -> {
			try {
				Attachment attachment = attachmentReader.getAttachment(item.getHeader());
				File file = new File(getCacheDir(), "playback-" + System.currentTimeMillis() + ".m4a");
				try (InputStream in = attachment.getStream();
						FileOutputStream out = new FileOutputStream(file)) {
					byte[] buffer = new byte[8192];
					int read;
					while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
				}
				runOnUiThreadUnlessDestroyed(() -> {
					playbackFile = file;
					status.setText("Voice message ready");
					playButton.setEnabled(true);
				});
			} catch (Exception e) {
				runOnUiThreadUnlessDestroyed(() -> {
					status.setText("Unable to load voice message");
					playButton.setEnabled(false);
				});
			}
		});
	}

	private void togglePlayback() {
		if (player != null && player.isPlaying()) {
			player.pause();
			playButton.setText("Play voice message");
			status.setText("Paused");
			return;
		}
		if (playbackFile == null) return;
		try {
			if (player == null) player = new MediaPlayer();
			else player.reset();
			player.setDataSource(playbackFile.getAbsolutePath());
			player.setOnPreparedListener(mp -> {
				mp.start();
				playButton.setText("Pause voice message");
				status.setText("Playing");
			});
			player.setOnCompletionListener(mp -> {
				playButton.setText("Play voice message");
				status.setText("Finished");
			});
			player.prepareAsync();
		} catch (Exception e) {
			status.setText("Unable to play voice message");
		}
	}

	@Override
	protected void onDestroy() {
		if (player != null) {
			player.release();
			player = null;
		}
		if (playbackFile != null) playbackFile.delete();
		super.onDestroy();
	}
}
