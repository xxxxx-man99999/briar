package org.briarproject.bramble.keyagreement;

import org.briarproject.bramble.api.crypto.CryptoComponent;
import org.briarproject.bramble.api.crypto.KeyAgreementCrypto;
import org.briarproject.bramble.api.crypto.KeyPair;
import org.briarproject.bramble.api.crypto.PostQuantumCapabilities;
import org.briarproject.bramble.api.keyagreement.Payload;
import org.briarproject.bramble.api.keyagreement.PayloadEncoder;
import org.briarproject.bramble.test.BrambleTestCase;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.briarproject.bramble.test.TestUtils.getAgreementPrivateKey;
import static org.briarproject.bramble.test.TestUtils.getAgreementPublicKey;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeyAgreementProtocolPqcTest extends BrambleTestCase {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Mock CryptoComponent crypto;
    @Mock KeyAgreementCrypto keyAgreementCrypto;
    @Mock PayloadEncoder payloadEncoder;
    @Mock KeyAgreementTransport transport;
    @Mock KeyAgreementProtocol.Callbacks callbacks;

    @Test(expected = AbortException.class)
    public void classicalOnlyPeerIsRejectedBeforeKeyExchange() throws Exception {
        Payload theirPayload = new Payload(new byte[32], emptyList());
        Payload ourPayload = new Payload(new byte[32], emptyList());
        KeyPair ourKeyPair = new KeyPair(getAgreementPublicKey(), getAgreementPrivateKey());

        KeyAgreementProtocol protocol = new KeyAgreementProtocol(
                callbacks, crypto, keyAgreementCrypto, payloadEncoder, transport,
                theirPayload, ourPayload, ourKeyPair, true);

        context.checking(new Expectations() {{
            oneOf(transport).sendPqCapabilities(with(any(PostQuantumCapabilities.class)));
            oneOf(transport).receivePqCapabilities();
            will(returnValue(PostQuantumCapabilities.local(false)));
            never(transport).sendKey(with(any(byte[].class)));
            never(transport).receiveKey();
        }});

        protocol.perform();
    }

    @Test
    public void capabilityContractRejectsMissingReceiveSupport() {
        PostQuantumCapabilities local = PostQuantumCapabilities.local(true);
        PostQuantumCapabilities peer = new PostQuantumCapabilities(true, true, true, false);
        assertFalse(local.canUseHybridWith(peer));
    }

    @Test
    public void capabilityContractAcceptsFullHybridSupport() {
        PostQuantumCapabilities local = PostQuantumCapabilities.local(true);
        PostQuantumCapabilities peer = PostQuantumCapabilities.local(true);
        assertTrue(local.canUseHybridWith(peer));
    }
}
