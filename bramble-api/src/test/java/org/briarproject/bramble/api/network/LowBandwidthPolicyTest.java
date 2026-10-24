package org.briarproject.bramble.api.network;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LowBandwidthPolicyTest {

	@Test
	public void test2GPolicy() {
		NetworkStatus status = new NetworkStatus(true, false, false,
				CellularNetworkType.G2);
		assertEquals(LowBandwidthPolicy.CELLULAR_2G_CHUNK_BYTES,
				LowBandwidthPolicy.getChunkSize(status));
		assertTrue(LowBandwidthPolicy.shouldPreferText(status));
	}

	@Test
	public void test3GPolicy() {
		NetworkStatus status = new NetworkStatus(true, false, false,
				CellularNetworkType.G3);
		assertEquals(LowBandwidthPolicy.CELLULAR_3G_CHUNK_BYTES,
				LowBandwidthPolicy.getChunkSize(status));
	}

	@Test
	public void testWifiPolicy() {
		NetworkStatus status = new NetworkStatus(true, true, false);
		assertEquals(LowBandwidthPolicy.NORMAL_CHUNK_BYTES,
				LowBandwidthPolicy.getChunkSize(status));
		assertEquals(100, LowBandwidthPolicy.getRecommendedPollInterval(100,
				status));
	}
}
