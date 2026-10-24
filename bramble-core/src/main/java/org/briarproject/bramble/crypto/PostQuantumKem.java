package org.briarproject.bramble.crypto;

import org.bouncycastle.jcajce.spec.MLKEMParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

import javax.annotation.concurrent.Immutable;

/**
 * ML-KEM-768 support for the post-quantum portion of a hybrid handshake.
 *
 * This class deliberately does not replace Briar's existing Curve25519
 * agreement. Callers should combine both shared secrets with a domain-separated
 * KDF before using the result as a session key.
 */
@Immutable
final class PostQuantumKem {

	private static final String PROVIDER = "BC";
	private static final String ALGORITHM = "ML-KEM";

	private PostQuantumKem() {
	}

	static KeyPair generateKeyPair() throws Exception {
		ensureProvider();
		KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM,
				PROVIDER);
		generator.initialize(MLKEMParameterSpec.ml_kem_768);
		return generator.generateKeyPair();
	}

	static boolean isSupported() {
		try {
			ensureProvider();
			KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	static byte[] getPublicKeyEncoding(PublicKey publicKey) {
		return publicKey.getEncoded();
	}

	static byte[] getPrivateKeyEncoding(PrivateKey privateKey) {
		return privateKey.getEncoded();
	}

	private static void ensureProvider() {
		if (Security.getProvider(PROVIDER) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}
}
