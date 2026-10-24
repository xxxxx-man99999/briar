package org.briarproject.briar.android.attachment;

import org.briarproject.bramble.api.db.DatabaseExecutor;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.NoSuchMessageException;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.android.attachment.AttachmentItem.State;
import org.briarproject.briar.android.attachment.media.ImageHelper;
import org.briarproject.briar.android.attachment.media.ImageSizeCalculator;
import org.briarproject.briar.android.attachment.media.Size;
import org.briarproject.briar.api.attachment.Attachment;
import org.briarproject.briar.api.attachment.AttachmentHeader;
import org.briarproject.briar.api.attachment.AttachmentReader;
import org.briarproject.briar.api.messaging.PrivateMessageHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.util.AndroidUtils.getSupportedImageContentTypes;
import static org.briarproject.bramble.util.IoUtils.tryToClose;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.briar.android.attachment.AttachmentItem.State.AVAILABLE;
import static org.briarproject.briar.android.attachment.AttachmentItem.State.ERROR;
import static org.briarproject.briar.android.attachment.AttachmentItem.State.LOADING;
import static org.briarproject.briar.android.attachment.AttachmentItem.State.MISSING;

@NotNullByDefault
class AttachmentRetrieverImpl implements AttachmentRetriever {

	private static final String VOICE_MIME_TYPE = "audio/mp4";
	private static final String VOICE_EXTENSION = "m4a";
	private static final Logger LOG =
			getLogger(AttachmentRetrieverImpl.class.getName());

	@DatabaseExecutor
	private final Executor dbExecutor;
	private final AttachmentReader attachmentReader;
	private final ImageHelper imageHelper;
	private final ImageSizeCalculator imageSizeCalculator;
	private final int defaultSize;
	private final int minWidth, maxWidth;
	private final int minHeight, maxHeight;

	private final ConcurrentMap<MessageId, MutableLiveData<AttachmentItem>>
			itemsWithSize = new ConcurrentHashMap<>();
	private final ConcurrentMap<MessageId, MutableLiveData<AttachmentItem>>
			itemsWithoutSize = new ConcurrentHashMap<>();

	@Inject
	AttachmentRetrieverImpl(@DatabaseExecutor Executor dbExecutor,
			AttachmentReader attachmentReader, AttachmentDimensions dimensions,
			ImageHelper imageHelper, ImageSizeCalculator imageSizeCalculator) {
		this.dbExecutor = dbExecutor;
		this.attachmentReader = attachmentReader;
		this.imageHelper = imageHelper;
		this.imageSizeCalculator = imageSizeCalculator;
		defaultSize = dimensions.defaultSize;
		minWidth = dimensions.minWidth;
		maxWidth = dimensions.maxWidth;
		minHeight = dimensions.minHeight;
		maxHeight = dimensions.maxHeight;
	}

	@Override
	@DatabaseExecutor
	public Attachment getMessageAttachment(AttachmentHeader h)
			throws DbException {
		return attachmentReader.getAttachment(h);
	}

	@Override
	public List<LiveData<AttachmentItem>> getAttachmentItems(
			PrivateMessageHeader messageHeader) {
		List<AttachmentHeader> headers = messageHeader.getAttachmentHeaders();
		List<LiveData<AttachmentItem>> items = new ArrayList<>(headers.size());
		boolean needsSize = headers.size() == 1 &&
				isImageContentType(headers.get(0).getContentType());
		for (AttachmentHeader h : headers) {
			if (!isSupportedContentType(h.getContentType())) {
				if (LOG.isLoggable(INFO)) {
					LOG.info("Unsupported content type " + h.getContentType());
				}
				AttachmentItem item = new AttachmentItem(h, "", ERROR);
				items.add(new MutableLiveData<>(item));
				continue;
			}

			MutableLiveData<AttachmentItem> liveData =
					itemsWithSize.get(h.getMessageId());
			if (!needsSize && liveData == null) {
				liveData = itemsWithoutSize.get(h.getMessageId());
			}

			if (liveData == null) {
				AttachmentItem item = new AttachmentItem(h,
						defaultSize, defaultSize, LOADING);
				liveData = new MutableLiveData<>(item);
				MutableLiveData<AttachmentItem> oldLiveData;
				if (needsSize) {
					oldLiveData = itemsWithSize.putIfAbsent(h.getMessageId(),
							liveData);
				} else {
					oldLiveData = itemsWithoutSize.putIfAbsent(h.getMessageId(),
							liveData);
				}
				if (oldLiveData == null) {
					MutableLiveData<AttachmentItem> finalLiveData = liveData;
					dbExecutor.execute(() ->
							loadAttachmentItem(h, needsSize, finalLiveData));
				} else {
					liveData = oldLiveData;
				}
			}
			items.add(liveData);
		}
		return items;
	}

	@Override
	@DatabaseExecutor
	public void cacheAttachmentItemWithSize(MessageId conversationMessageId,
			AttachmentHeader h) throws DbException {
		if (itemsWithSize.containsKey(h.getMessageId())) return;
		try {
			Attachment a = attachmentReader.getAttachment(h);
			AttachmentItem item = createAttachmentItem(a, true);
			MutableLiveData<AttachmentItem> liveData =
					new MutableLiveData<>(item);
			itemsWithSize.putIfAbsent(h.getMessageId(), liveData);
		} catch (NoSuchMessageException e) {
			LOG.info("Attachment not received yet");
		}
	}

	@Override
	@DatabaseExecutor
	public void loadAttachmentItem(MessageId attachmentId) {
		MutableLiveData<AttachmentItem> liveData;
		boolean needsSize = true;
		liveData = itemsWithSize.get(attachmentId);
		if (liveData == null) {
			needsSize = false;
			liveData = itemsWithoutSize.get(attachmentId);
		}
		if (liveData == null) return;
		AttachmentHeader h = requireNonNull(liveData.getValue()).getHeader();
		loadAttachmentItem(h, needsSize, liveData);
	}

	@DatabaseExecutor
	private void loadAttachmentItem(AttachmentHeader h, boolean needsSize,
			MutableLiveData<AttachmentItem> liveData) {
		Attachment a;
		AttachmentItem item;
		try {
			a = attachmentReader.getAttachment(h);
			item = createAttachmentItem(a, needsSize);
		} catch (NoSuchMessageException e) {
			LOG.info("Attachment not received yet");
			item = new AttachmentItem(h, defaultSize, defaultSize, MISSING);
		} catch (DbException e) {
			logException(LOG, WARNING, e);
			item = new AttachmentItem(h, "", ERROR);
		}
		liveData.postValue(item);
	}

	@Override
	public AttachmentItem createAttachmentItem(Attachment a,
			boolean needsSize) {
		AttachmentHeader h = a.getHeader();
		if (VOICE_MIME_TYPE.equals(h.getContentType()) ||
				"audio/x-m4a".equals(h.getContentType())) {
			return new AttachmentItem(h, defaultSize, defaultSize,
					VOICE_EXTENSION, defaultSize, defaultSize, AVAILABLE);
		}
		AttachmentItem item;
		if (needsSize) {
			InputStream is = new BufferedInputStream(a.getStream());
			Size size = imageSizeCalculator.getSize(is, h.getContentType());
			tryToClose(is, LOG, WARNING);
			item = createAttachmentItem(h, size);
		} else {
			String extension = imageHelper.getExtensionFromMimeType(h.getContentType());
			State state = AVAILABLE;
			if (extension == null) {
				extension = "";
				state = ERROR;
			}
			item = new AttachmentItem(h, extension, state);
		}
		return item;
	}

	private boolean isSupportedContentType(String mimeType) {
		return isImageContentType(mimeType) || VOICE_MIME_TYPE.equals(mimeType)
				|| "audio/x-m4a".equals(mimeType);
	}

	private boolean isImageContentType(String mimeType) {
		return asList(getSupportedImageContentTypes()).contains(mimeType);
	}

	private AttachmentItem createAttachmentItem(AttachmentHeader h, Size size) {
		Size thumbnailSize = new Size(defaultSize, defaultSize, size.getMimeType());
		if (!size.hasError()) {
			thumbnailSize = getThumbnailSize(size.getWidth(), size.getHeight(),
					size.getMimeType());
		}
		String extension = imageHelper.getExtensionFromMimeType(size.getMimeType());
		boolean hasError = extension == null || size.hasError();
		if (!h.getContentType().equals(size.getMimeType())) {
			if (LOG.isLoggable(WARNING)) {
				LOG.warning("Header has different mime type (" +
						h.getContentType() + ") than image (" +
						size.getMimeType() + ").");
			}
			hasError = true;
		}
		if (extension == null) extension = "";
		State state = hasError ? ERROR : AVAILABLE;
		return new AttachmentItem(h, size.getWidth(), size.getHeight(),
				extension, thumbnailSize.getWidth(), thumbnailSize.getHeight(), state);
	}

	private int[] unused() { return new int[] {minWidth, maxWidth, minHeight, maxHeight}; }

	private Size getThumbnailSize(int width, int height, String mimeType) {
		float widthPercentage = maxWidth / (float) width;
		float heightPercentage = maxHeight / (float) height;
		float scaleFactor = Math.min(widthPercentage, heightPercentage);
		if (scaleFactor > 1) scaleFactor = 1f;
		int thumbnailWidth = (int) (width * scaleFactor);
		int thumbnailHeight = (int) (height * scaleFactor);
		if (thumbnailWidth < minWidth || thumbnailHeight < minHeight) {
			widthPercentage = minWidth / (float) width;
			heightPercentage = minHeight / (float) height;
			scaleFactor = Math.max(widthPercentage, heightPercentage);
			thumbnailWidth = (int) (width * scaleFactor);
			thumbnailHeight = (int) (height * scaleFactor);
			if (thumbnailWidth > maxWidth) thumbnailWidth = maxWidth;
			if (thumbnailHeight > maxHeight) thumbnailHeight = maxHeight;
		}
		return new Size(thumbnailWidth, thumbnailHeight, mimeType);
	}
}
