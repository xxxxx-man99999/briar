package org.briarproject.bramble.voice;

import android.media.MediaRecorder;

import org.briarproject.bramble.api.voice.VoiceMessage;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;
import java.io.IOException;

/**
 * Small Android voice recorder. The resulting compressed M4A file is suitable
 * for the normal Briar attachment pipeline and can use the low-bandwidth
 * queue/chunk/resume path.
 *
 * The caller must obtain RECORD_AUDIO runtime permission before start().
 */
@NotNullByDefault
public final class AndroidVoiceRecorder {

	private static final int MAX_DURATION_MS = VoiceMessage.MAX_DURATION_SECONDS * 1000;
	private MediaRecorder recorder;
	private File output;
	private long startedAt;

	public void start(File outputFile) throws IOException {
		if (recorder != null) throw new IllegalStateException("Already recording");
		output = outputFile;
		File parent = output.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs())
			throw new IOException("Unable to create recording directory");

		MediaRecorder r = new MediaRecorder();
		r.setAudioSource(MediaRecorder.AudioSource.MIC);
		r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		// Keep the bitrate low for cellular transfer, but don't force a sampling
		// rate. Some older Android/Samsung AAC encoders reject 16 kHz even though
		// they support AAC/MPEG-4 perfectly well.
		r.setAudioEncodingBitRate(24000);
		r.setMaxDuration(MAX_DURATION_MS);
		r.setOutputFile(output.getAbsolutePath());
		try {
			r.prepare();
		r.start();
		} catch (RuntimeException e) {
			try {
				r.reset();
			} finally {
				r.release();
			}
			throw new IOException("Unable to start AAC voice recorder", e);
		}
		recorder = r;
		startedAt = System.currentTimeMillis();
	}

	public VoiceMessage stop() {
		if (recorder == null) throw new IllegalStateException("Not recording");
		MediaRecorder r = recorder;
		recorder = null;
		try {
			r.stop();
	} finally {
			r.release();
	}
		long duration = Math.min(MAX_DURATION_MS, System.currentTimeMillis() - startedAt);
		return new VoiceMessage(output.getName(), duration, output.length());
	}

	public void cancel() {
		if (recorder == null) return;
		MediaRecorder r = recorder;
		recorder = null;
		try {
			r.reset();
		} finally {
			r.release();
		}
		if (output != null) output.delete();
	}
}
