package org.briarproject.bramble.api.network;

import org.briarproject.nullsafety.NotNullByDefault;

/**
 * Tracks chunked progress for attachments that may be interrupted and resumed.
 * The offset is monotonic and can be restored after a connection drop.
 */
@NotNullByDefault
public final class ResumableTransfer {

	private final long totalBytes;
	private final int chunkSize;
	private long acknowledgedBytes;

	public ResumableTransfer(long totalBytes, NetworkStatus status) {
		if (totalBytes < 0) throw new IllegalArgumentException("Negative size");
		this.totalBytes = totalBytes;
		this.chunkSize = AdaptiveTransfer.chunkSize(status);
	}

	public long getTotalBytes() {
		return totalBytes;
	}

	public long getAcknowledgedBytes() {
		return acknowledgedBytes;
	}

	public boolean isComplete() {
		return acknowledgedBytes == totalBytes;
	}

	public int nextChunkLength() {
		long remaining = totalBytes - acknowledgedBytes;
		return (int) Math.min(chunkSize, remaining);
	}

	/** Acknowledges bytes that were durably received by the peer. */
	public void acknowledge(long bytes) {
		if (bytes < 0 || acknowledgedBytes + bytes > totalBytes)
			throw new IllegalArgumentException("Invalid acknowledgement");
		acknowledgedBytes += bytes;
	}

	/** Restores a previously persisted remote offset after reconnect. */
	public void resumeFrom(long offset) {
		if (offset < 0 || offset > totalBytes)
			throw new IllegalArgumentException("Invalid resume offset");
		if (offset < acknowledgedBytes)
			throw new IllegalArgumentException("Resume offset moved backwards");
		acknowledgedBytes = offset;
	}
}
