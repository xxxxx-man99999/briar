package org.briarproject.bramble.crypto;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.junit.Assume;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Integration-level coverage for the PQ side of the hybrid key primitive. */
public class HybridKeyAgreementIntegrationTest {

	@Test
	public void pqPeersDeriveTheSameHybridSecret() throws Exception {
		Assume.assumeTrue(HybridKeyAgreement.isSupported());
		AsymmetricCipherKeyPair bob = HybridKeyAgreement.generateKemKeyPair();
		MLKEMPublicKeyParameters bobPublic =
				(MLKEMPublicKeyParameters) bob.getPublic();
		MLKEMPrivateKeyParameters bobPrivate =
				(MLKEMPrivateKeyParameters) bob.getPrivate();

		HybridKeyAgreement.Encapsulation encapsulation =
				HybridKeyAgreement.encapsulate(bobPublic);
		byte[] bobKemSecret = HybridKeyAgreement.decapsulate(
				bobPrivate, encapsulation.getCiphertext());

		byte[] classical = new byte[32];
		Arrays.fill(classical, (byte) 0x42);
		byte[] context = new byte[] {1, 2, 3, 4};
		byte[] aliceHybrid = HybridKeyAgreement.combine(
				classical, encapsulation.getSecret(), context);
		byte[] bobHybrid = HybridKeyAgreement.combine(
				classical, bobKemSecret, context);
		assertArrayEquals(aliceHybrid, bobHybrid);
	}

	@Test
	public void hybridSecretIsBoundToKemSecret() throws GeneralSecurityException {
		Assume.assumeTrue(HybridKeyAgreement.isSupported());
		byte[] classical = new byte[32];
		byte[] kemA = new byte[32];
		byte[] kemB = new byte[32];
		Arrays.fill(kemA, (byte) 0x01);
		Arrays.fill(kemB, (byte) 0x02);

		byte[] first = HybridKeyAgreement.combine(classical, kemA);
		byte[] second = HybridKeyAgreement.combine(classical, kemB);
		assertFalse(Arrays.equals(first, second));
	}

	@Test
	public void hybridSecretIsBoundToClassicalSecret() throws GeneralSecurityException {
		byte[] classicalA = new byte[32];
		byte[] classicalB = new byte[32];
		byte[] kem = new byte[32];
		Arrays.fill(classicalA, (byte) 0x01);
		Arrays.fill(classicalB, (byte) 0x02);
		byte[] first = HybridKeyAgreement.combine(classicalA, kem);
		byte[] second = HybridKeyAgreement.combine(classicalB, kem);
		assertFalse(Arrays.equals(first, second));
	}

	@Test
	public void hybridSecretIsBoundToTranscriptContext() throws GeneralSecurityException {
		byte[] classical = new byte[32];
		byte[] kem = new byte[32];
		byte[] contextA = new byte[] {1, 2, 3};
		byte[] contextB = new byte[] {1, 2, 4};
		byte[] first = HybridKeyAgreement.combine(classical, kem, contextA);
		byte[] second = HybridKeyAgreement.combine(classical, kem, contextB);
		assertFalse(Arrays.equals(first, second));
	}

	@Test
	public void lengthPrefixPreventsAmbiguousContextInputs()
			throws GeneralSecurityException {
		byte[] classical = new byte[] {1};
		byte[] kem = new byte[] {2};
		byte[] first = HybridKeyAgreement.combine(classical, kem, new byte[] {3, 4});
		byte[] second = HybridKeyAgreement.combine(classical, kem, new byte[] {3, 4, 5});
		assertFalse(Arrays.equals(first, second));
	}
}
