package org.briarproject.bramble.keyagreement;

import org.briarproject.bramble.api.crypto.PostQuantumCapabilities;
import org.briarproject.bramble.api.crypto.PostQuantumCapabilitiesCodec;
import org.briarproject.bramble.api.keyagreement.KeyAgreementConnection;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.briarproject.bramble.api.record.Record;
import org.briarproject.bramble.api.record.RecordReader;
import org.briarproject.bramble.api.record.RecordReader.RecordPredicate;
import org.briarproject.bramble.api.record.RecordReaderFactory;
import org.briarproject.bramble.api.record.RecordWriter;
import org.briarproject.bramble.api.record.RecordWriterFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static org.briarproject.bramble.api.keyagreement.KeyAgreementConstants.PROTOCOL_VERSION;
import static org.briarproject.bramble.api.keyagreement.RecordTypes.ABORT;
import static org.briarproject.bramble.api.keyagreement.RecordTypes.CONFIRM;
import static org.briarproject.bramble.api.keyagreement.RecordTypes.KEY;
import static org.briarproject.bramble.api.keyagreement.RecordTypes.PQ_CAPABILITIES;
import static org.briarproject.bramble.api.keyagreement.RecordTypes.PQ_CIPHERTEXT;
import static org.briarproject.bramble.api.keyagreement.RecordTypes.PQ_PUBLIC_KEY;
import static org.briarproject.bramble.util.LogUtils.logException;

/**
 * Handles the sending and receiving of BQP records.
 *
 * <p>PQ capability negotiation is optional. A capability receiver buffers a
 * legacy KEY record when the peer does not understand the new record type,
 * allowing the existing classical BQP flow to continue unchanged.</p>
 */
@NotNullByDefault
class KeyAgreementTransport {

	private static final Logger LOG =
			Logger.getLogger(KeyAgreementTransport.class.getName());

	private static final RecordPredicate ACCEPT = r ->
			r.getProtocolVersion() == PROTOCOL_VERSION &&
					isKnownRecordType(r.getRecordType());

	private static final RecordPredicate IGNORE = r ->
			r.getProtocolVersion() == PROTOCOL_VERSION &&
					!isKnownRecordType(r.getRecordType());

	private static boolean isKnownRecordType(byte type) {
		return type == KEY || type == CONFIRM || type == ABORT
				|| type == PQ_CAPABILITIES || type == PQ_PUBLIC_KEY
				|| type == PQ_CIPHERTEXT;
	}

	private final KeyAgreementConnection kac;
	private final RecordReader reader;
	private final RecordWriter writer;
	private Record pendingRecord;

	KeyAgreementTransport(RecordReaderFactory recordReaderFactory,
			RecordWriterFactory recordWriterFactory, KeyAgreementConnection kac)
			throws IOException {
		this.kac = kac;
		InputStream in = kac.getConnection().getReader().getInputStream();
		reader = recordReaderFactory.createRecordReader(in);
		OutputStream out = kac.getConnection().getWriter().getOutputStream();
		writer = recordWriterFactory.createRecordWriter(out);
	}

	public DuplexTransportConnection getConnection() {
		return kac.getConnection();
	}

	public TransportId getTransportId() {
		return kac.getTransportId();
	}

	void sendKey(byte[] key) throws IOException {
		writeRecord(KEY, key);
	}

	byte[] receiveKey() throws AbortException {
		return readRecord(KEY);
	}

	void sendPqCapabilities(PostQuantumCapabilities capabilities) throws IOException {
		writeRecord(PQ_CAPABILITIES, PostQuantumCapabilitiesCodec.encode(capabilities));
	}

	PostQuantumCapabilities receivePqCapabilities() throws AbortException {
		try {
			Record record = readNextRecord();
			byte type = record.getRecordType();
			if (type == ABORT) throw new AbortException(true);
			if (type == PQ_CAPABILITIES)
				return PostQuantumCapabilitiesCodec.decode(record.getPayload());
			if (type == KEY) {
				pendingRecord = record;
				return PostQuantumCapabilities.local(false);
			}
			throw new AbortException(false);
		} catch (IOException e) {
			throw new AbortException(e);
		}
	}

	void sendPqPublicKey(byte[] publicKey) throws IOException {
		writeRecord(PQ_PUBLIC_KEY, publicKey);
	}

	byte[] receivePqPublicKey() throws AbortException {
		return readRecord(PQ_PUBLIC_KEY);
	}

	void sendPqCiphertext(byte[] ciphertext) throws IOException {
		writeRecord(PQ_CIPHERTEXT, ciphertext);
	}

	byte[] receivePqCiphertext() throws AbortException {
		return readRecord(PQ_CIPHERTEXT);
	}

	void sendConfirm(byte[] confirm) throws IOException {
		writeRecord(CONFIRM, confirm);
	}

	byte[] receiveConfirm() throws AbortException {
		return readRecord(CONFIRM);
	}

	void sendAbort(boolean exception) {
		try {
			writeRecord(ABORT, new byte[0]);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
			exception = true;
		}
		tryToClose(exception);
	}

	private void tryToClose(boolean exception) {
		try {
			kac.getConnection().getReader().dispose(exception, true);
			kac.getConnection().getWriter().dispose(exception);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
		}
	}

	private void writeRecord(byte type, byte[] payload) throws IOException {
		writer.writeRecord(new Record(PROTOCOL_VERSION, type, payload));
		writer.flush();
	}

	private Record readNextRecord() throws IOException {
		if (pendingRecord != null) {
			Record record = pendingRecord;
			pendingRecord = null;
			return record;
		}
		Record record = reader.readRecord(ACCEPT, IGNORE);
		if (record == null) throw new EOFException();
		return record;
	}

	private byte[] readRecord(byte expectedType) throws AbortException {
		try {
			Record record = readNextRecord();
			byte type = record.getRecordType();
			if (type == ABORT) throw new AbortException(true);
			if (type != expectedType) throw new AbortException(false);
			return record.getPayload();
		} catch (IOException e) {
			throw new AbortException(e);
		}
	}
}
