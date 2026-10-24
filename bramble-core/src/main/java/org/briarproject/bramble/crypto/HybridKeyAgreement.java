package org.briarproject.bramble.crypto;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.briarproject.nullsafety.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Hybrid classical + post-quantum key agreement primitive.
 *
 * The existing Briar agreement remains the authentication mechanism. This
 * class provides the ML-KEM-768 half and a domain-separated combiner.
 */
@NotNullByDefault
public final class HybridKeyAgreement {

	private static final byte[] LABEL =
			"Briar-Hybrid-KEM-v1".getBytes(StandardCharsets.US_ASCII);

	private HybridKeyAgreement() {}

	public static boolean isSupported() {
		try {
			MLKEMGenerator generator = new MLKEMGenerator(new SecureRandom());
			generator.generateEncapsulated(new MLKEMPublicKeyParameters(
					MLKEMParameters.ml_kem_768, new byte[1184]));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static AsymmetricCipherKeyPair generateKemKeyPair() {
		org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator generator =
				new org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator();
		generator.init(new org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters(
				new SecureRandom(), MLKEMParameters.ml_kem_768));
		return generator.generateKeyPair();
	}

	public static Encapsulation encapsulate(MLKEMPublicKeyParameters publicKey) {
		SecretWithEncapsulation result = new MLKEMGenerator(new SecureRandom())
				.generateEncapsulated(publicKey);
		return new Encapsulation(result.getEncapsulation(), result.getSecret());
	}

	public static byte[] decapsulate(MLKEMPrivateKeyParameters privateKey,
			byte[] encapsulation) {
		return new MLKEMExtractor(privateKey).extractSecret(encapsulation);
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
