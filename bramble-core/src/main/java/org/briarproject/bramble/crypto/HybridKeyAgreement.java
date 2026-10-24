package org.briarproject.bramble.crypto;

import org.briarproject.nullsafety.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.NamedParameterSpec;

import javax.crypto.KEM;

/**
 * Hybrid classical + post-quantum key agreement primitive.
 *
 * The existing Briar agreement remains the authentication mechanism. This
 * class provides the ML-KEM half and a domain-separated combiner for the
 * negotiated hybrid secret.
 */
@NotNullByDefault
public final class HybridKeyAgreement {

	private static final String KEM_ALGORITHM = "ML-KEM";
	private static final String KEM_PARAMETER = "ML-KEM-768";
	private static final byte[] LABEL =
			"Briar-Hybrid-KEM-v1".getBytes(StandardCharsets.US_ASCII);

	private HybridKeyAgreement() {}

	public static boolean isSupported() {
		try {
			KEM.getInstance(KEM_ALGORITHM);
			return true;
		} catch (GeneralSecurityException e) {
			return false;
		}
	}

	public static KeyPair generateKemKeyPair() throws GeneralSecurityException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance(KEM_ALGORITHM);
		generator.initialize(new NamedParameterSpec(KEM_PARAMETER), new SecureRandom());
		return generator.generateKeyPair();
	}

	public static Encapsulation encapsulate(PublicKey publicKey)
			throws GeneralSecurityException {
		KEM kem = KEM.getInstance(KEM_ALGORITHM);
		KEM.Encapsulated encapsulated = kem.newEncapsulator(publicKey).encapsulate();
		return new Encapsulation(encapsulated.encapsulation(),
				encapsulated.key().getEncoded());
	}

	public static byte[] decapsulate(PrivateKey privateKey, byte[] encapsulation)
			throws GeneralSecurityException {
		KEM kem = KEM.getInstance(KEM_ALGORITHM);
		return kem.newDecapsulator(privateKey).decapsulate(encapsulation).getEncoded();
	}

	/** Combines the existing classical secret and the ML-KEM secret with SHA-256. */
	public static byte[] combine(byte[] classicalSecret, byte[] kemSecret)
			throws GeneralSecurityException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(LABEL);
		digest.update((byte) (classicalSecret.length >>> 24));
		digest.update((byte) (classicalSecret.length >>> 16));
		digest.update((byte) (classicalSecret.length >>> 8));
		digest.update((byte) classicalSecret.length);
		digest.update(classicalSecret);
		digest.update(kemSecret);
		return digest.digest();
	}

	public static final class Encapsulation {
		private final byte[] ciphertext;
		private final byte[] secret;

		private Encapsulation(byte[] ciphertext, byte[] secret) {
			this.ciphertext = ciphertext.clone();
			this.secret = secret.clone();
		}

		public byte[] getCiphertext() { return ciphertext.clone(); }
		public byte[] getSecret() { return secret.clone(); }
	}
}
