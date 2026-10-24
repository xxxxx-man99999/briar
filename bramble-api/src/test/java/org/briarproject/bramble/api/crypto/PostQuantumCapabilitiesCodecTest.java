package org.briarproject.bramble.api.crypto;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostQuantumCapabilitiesCodecTest {

	@Test
	public void supportedCapabilitiesRoundTrip() throws Exception {
		PostQuantumCapabilities original =
				new PostQuantumCapabilities(true, true, true, false);
		byte[] encoded = PostQuantumCapabilitiesCodec.encode(original);
		PostQuantumCapabilities decoded = PostQuantumCapabilitiesCodec.decode(encoded);

		assertArrayEquals(new byte[] {2, 1, 7}, encoded);
		assertTrue(decoded.isSupported());
		assertTrue(decoded.isEnabled());
		assertTrue(decoded.isSendEnabled());
		assertFalse(decoded.isReceiveEnabled());
	}

	@Test
	public void disabledCapabilitiesEncodeExplicitly() throws Exception {
		byte[] encoded = PostQuantumCapabilitiesCodec.encode(
				PostQuantumCapabilities.local(false));
		PostQuantumCapabilities decoded = PostQuantumCapabilitiesCodec.decode(encoded);

		assertArrayEquals(new byte[] {2, 1, 0}, encoded);
		assertFalse(decoded.isSupported());
		assertFalse(decoded.isEnabled());
		assertFalse(decoded.isSendEnabled());
		assertFalse(decoded.isReceiveEnabled());
	}

	@Test
	public void version1CapabilitiesRemainDecodable() throws Exception {
		PostQuantumCapabilities decoded = PostQuantumCapabilitiesCodec.decode(
				new byte[] {1, 1});
		assertTrue(decoded.isSupported());
		assertTrue(decoded.isEnabled());
		assertTrue(decoded.isSendEnabled());
		assertTrue(decoded.isReceiveEnabled());
	}

	@Test
	public void directionalMismatchDisablesHybridUse() {
		PostQuantumCapabilities sendOnly =
				new PostQuantumCapabilities(true, true, true, false);
		PostQuantumCapabilities receiveOnly =
				new PostQuantumCapabilities(true, true, false, true);
		assertTrue(sendOnly.canUseHybridWith(receiveOnly));
		assertTrue(receiveOnly.canUseHybridWith(sendOnly));
		PostQuantumCapabilities disabledPeer =
				new PostQuantumCapabilities(true, false, true, true);
		assertFalse(sendOnly.canUseHybridWith(disabledPeer));
	}

	@Test
	public void unknownAlgorithmFallsBackToClassical() throws Exception {
		PostQuantumCapabilities decoded = PostQuantumCapabilitiesCodec.decode(
				new byte[] {2, 99, 15});
		assertFalse(decoded.isSupported());
		assertFalse(decoded.isEnabled());
	}

	@Test(expected = IOException.class)
	public void malformedRecordIsRejected() throws Exception {
		PostQuantumCapabilitiesCodec.decode(new byte[] {2, 1});
	}
}
