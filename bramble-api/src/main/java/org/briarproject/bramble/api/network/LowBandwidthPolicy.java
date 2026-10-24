package org.briarproject.bramble.api.network;

import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public final class LowBandwidthPolicy {

	public static final int NORMAL_CHUNK_BYTES = 16 * 1024;
	public static final int CELLULAR_3G_CHUNK_BYTES = 4 * 1024;
	public static final int CELLULAR_2G_CHUNK_BYTES = 1024;

	private LowBandwidthPolicy() {
	}

	public static int getChunkSize(NetworkStatus status) {
		if (status.getCellularNetworkType() == CellularNetworkType.G2) {
			return CELLULAR_2G_CHUNK_BYTES;
		}
		if (status.getCellularNetworkType() == CellularNetworkType.G3) {
			return CELLULAR_3G_CHUNK_BYTES;
		}
		return NORMAL_CHUNK_BYTES;
	}

	public static boolean shouldPreferText(NetworkStatus status) {
		return status.isLowBandwidthCellular();
	}

	public static int getRecommendedPollInterval(int currentInterval,
			NetworkStatus status) {
		if (!status.isLowBandwidthCellular()) return currentInterval;
		long interval = (long) currentInterval * 2L;
		return (int) Math.min(interval, Integer.MAX_VALUE);
	}
}
