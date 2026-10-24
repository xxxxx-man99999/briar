package org.briarproject.bramble.api.voice;

import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/** Metadata for a compressed voice message transferred as a normal attachment. */
@Immutable
@NotNullByDefault
public final class VoiceMessage {

	public static final String MIME_TYPE = "audio/mp4";
	public static final int MAX_DURATION_SECONDS = 300;

	private final String fileName;
	private final long durationMs;
	private final long sizeBytes;

	public VoiceMessage(String fileName, long durationMs, long sizeBytes) {
		if (durationMs < 0 || durationMs > MAX_DURATION_SECONDS * 1000L)
			throw new IllegalArgumentException("Invalid voice duration");
		if (sizeBytes < 0) throw new IllegalArgumentException("Invalid voice size");
		this.fileName = fileName;
		this.durationMs = durationMs;
		this.sizeBytes = sizeBytes;
	}

	public String getFileName() { return fileName; }
	public long getDurationMs() { return durationMs; }
	public long getSizeBytes() { return sizeBytes; }
	public String getMimeType() { return MIME_TYPE; }
}
