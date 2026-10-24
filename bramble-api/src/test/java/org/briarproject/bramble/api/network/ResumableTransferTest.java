package org.briarproject.bramble.api.network;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResumableTransferTest {

	@Test
	public void test2GTransferResumesAfterInterruption() {
		NetworkStatus status = new NetworkStatus(true, false, false,
				CellularNetworkType.G2);
		ResumableTransfer transfer = new ResumableTransfer(3500, status);

		assertEquals(1024, transfer.nextChunkLength());
		transfer.acknowledge(1024);
		transfer.acknowledge(1024);
		assertEquals(1024, transfer.nextChunkLength());
		assertFalse(transfer.isComplete());

		// Simulate persisted remote progress after the connection drops.
		ResumableTransfer resumed = new ResumableTransfer(3500, status);
		resumed.resumeFrom(2048);
		assertEquals(1024, resumed.nextChunkLength());
		resumed.acknowledge(1024);
		resumed.acknowledge(428);
		assertTrue(resumed.isComplete());
	}

	@Test
	public void test3GUsesLargerChunks() {
		NetworkStatus status = new NetworkStatus(true, false, false,
				CellularNetworkType.G3);
		ResumableTransfer transfer = new ResumableTransfer(10000, status);
		assertEquals(4096, transfer.nextChunkLength());
		transfer.resumeFrom(4096);
		assertEquals(4096, transfer.nextChunkLength());
	}
}
