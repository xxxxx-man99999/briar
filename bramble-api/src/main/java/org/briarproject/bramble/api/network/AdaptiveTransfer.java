package org.briarproject.bramble.api.network;

import org.briarproject.nullsafety.NotNullByDefault;

/**
 * Selects a conservative chunk size for resumable transfers based on the
 * currently available network. Smaller chunks reduce the amount of work that
 * has to be retransmitted when a low-bandwidth connection is interrupted.
 */
@NotNullByDefault
public final class AdaptiveTransfer {

	private static final int CHUNK_2G = 1024;
	private static final int CHUNK_3G = 4096;
	private static final int CHUNK_FAST = 16 * 1024;

	private AdaptiveTransfer() {
	}

	public static int chunkSize(NetworkStatus status) {
		if (!status.isConnected()) return CHUNK_2G;
		if (status.isWifi()) return CHUNK_FAST;

		switch (status.getCellularNetworkType()) {
			case G3:
				return CHUNK_3G;
			case G4:
				return CHUNK_FAST;
		case G2:
			case UNKNOWN:
			default:
				return CHUNK_2G;
		}
	}
}
