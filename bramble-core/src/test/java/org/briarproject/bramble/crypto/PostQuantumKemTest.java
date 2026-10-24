package org.briarproject.bramble.crypto;

import org.junit.Test;

import java.security.KeyPair;

import static org.junit.Assert.assertNotNull;

public class PostQuantumKemTest {

	@Test
	public void testMlKem768IsAvailable() throws Exception {
		if (!PostQuantumKem.isSupported()) return;
		KeyPair keyPair = PostQuantumKem.generateKeyPair();
		assertNotNull(keyPair.getPublic());
		assertNotNull(keyPair.getPrivate());
		assertNotNull(PostQuantumKem.getPublicKeyEncoding(keyPair.getPublic()));
		assertNotNull(PostQuantumKem.getPrivateKeyEncoding(keyPair.getPrivate()));
	}
}
