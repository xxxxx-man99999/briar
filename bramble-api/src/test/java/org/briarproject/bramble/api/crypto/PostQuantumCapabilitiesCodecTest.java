package org.briarproject.bramble.api.crypto;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostQuantumCapabilitiesCodecTest {

	@Test
	public void supportedCapabilitiesRoundTrip() throws Exception {
		PostQuantumCapabilities original = PostQuantumCapabilities.local(true);
		byte[] encoded = PostQuantumCapabilitiesCodec.encode(original);
		PostQuantumCapabilities decoded = PostQuantumCapabilitiesCodec.decode(encoded);

		assertArrayEquals(new byte[] {1, 1}, encoded);
		assertTrue(decoded.isSupported());
		assertTrue(decoded.isEnabled());
	}

	@Test
	public void disabledCapabilitiesEncodeCompactly() throws Exception {
		byte[] encoded = PostQuantumCapabilitiesCodec.encode(
				PostQuantumCapabilities.local(false));
		PostQuantumCapabilities decoded = PostQuantumCapabilitiesCodec.decode(encoded);

		assertArrayEquals(new byte[] {1, 0}, encoded);
		assertFalse(decoded.isSupported());
		assertFalse(decoded.isEnabled());
	}

	@Test
	public void unknownAlgorithmFallsBackToClassical() throws Exception {
		PostQuantumCapabilities decoded = PostQuantumCapabilitiesCodec.decode(
				new byte[] {1, 99});
		assertFalse(decoded.isSupported());
		assertFalse(decoded.isEnabled());
	}

	@Test(expected = IOException.class)
	public void malformedRecordIsRejected() throws Exception {
		PostQuantumCapabilitiesCodec.decode(new byte[] {1});
	}
}
