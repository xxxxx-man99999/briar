package org.briarproject.bramble.api.network;

import org.briarproject.nullsafety.NotNullByDefault;

/** Low-bandwidth transfer parameters shared by message/file transports. */
@NotNullByDefault
public final class AdaptiveTransfer {

	private AdaptiveTransfer() {}

	public static int chunkSize(NetworkStatus status) {
		return LowBandwidthPolicy.getChunkSize(status);
	}

	public static int maxRetries(NetworkStatus status) {
		if (status.getCellularNetworkType() == CellularNetworkType.G2) return 8;
		if (status.getCellularNetworkType() == CellularNetworkType.G3) return 5;
		return 3;
	}

	public static long retryDelayMs(NetworkStatus status, int attempt) {
		long base = status.getCellularNetworkType() == CellularNetworkType.G2 ? 2000L : 1000L;
		return Math.min(60000L, base << Math.min(attempt, 5));
	}

	public static boolean shouldQueue(NetworkStatus status) {
		return !status.isConnected() || status.getCellularNetworkType() == CellularNetworkType.G2;
	}
}
