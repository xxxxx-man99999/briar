package org.briarproject.briar.android.conversation;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.load.Transformation;

import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.R;
import org.briarproject.briar.android.attachment.AttachmentItem;
import org.briarproject.briar.android.conversation.glide.BriarImageTransformation;
import org.briarproject.briar.android.conversation.glide.GlideApp;
import org.briarproject.briar.android.conversation.glide.Radii;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.annotation.DrawableRes;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.ImageView.ScaleType.CENTER_CROP;
import static android.widget.ImageView.ScaleType.FIT_CENTER;
import static com.bumptech.glide.load.engine.DiskCacheStrategy.NONE;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static org.briarproject.briar.android.attachment.AttachmentItem.State.AVAILABLE;
import static org.briarproject.briar.android.attachment.AttachmentItem.State.ERROR;

@NotNullByDefault
class ImageViewHolder extends ViewHolder {

	@DrawableRes
	private static final int ERROR_RES = R.drawable.ic_image_broken;

	protected final ImageView imageView;
	private final LinearLayout voiceContainer;
	private final ImageButton voicePlayButton;
	private final int imageSize;
	private final MessageId conversationItemId;

	ImageViewHolder(View v, int imageSize, MessageId conversationItemId) {
		super(v);
		imageView = v.findViewById(R.id.imageView);
		voiceContainer = v.findViewById(R.id.voiceContainer);
		voicePlayButton = v.findViewById(R.id.voicePlayButton);
		this.imageSize = imageSize;
		this.conversationItemId = conversationItemId;
	}

	void bind(AttachmentItem attachment, Radii r, boolean single,
			boolean needsStretch) {
		boolean voice = attachment.getMimeType().startsWith("audio/");
		setImageViewDimensions(attachment, single, needsStretch, voice);
		if (attachment.getState() != AVAILABLE) {
			voiceContainer.setVisibility(GONE);
			imageView.setVisibility(VISIBLE);
			GlideApp.with(imageView).clear(imageView);
			imageView.setImageResource(attachment.getState() == ERROR ?
					ERROR_RES : R.drawable.ic_image_missing);
			imageView.setScaleType(FIT_CENTER);
		} else if (voice) {
			GlideApp.with(imageView).clear(imageView);
			imageView.setVisibility(GONE);
			voiceContainer.setVisibility(VISIBLE);
			voicePlayButton.setImageResource(android.R.drawable.ic_media_play);
			voicePlayButton.setContentDescription("Play voice message");
		} else {
			voiceContainer.setVisibility(GONE);
			imageView.setVisibility(VISIBLE);
			loadImage(attachment, r);
			imageView.setScaleType(CENTER_CROP);
		}
		imageView.setTransitionName(
				attachment.getTransitionName(conversationItemId));
	}

	private void setImageViewDimensions(AttachmentItem a, boolean single,
			boolean needsStretch, boolean voice) {
		LayoutParams params = (LayoutParams) itemView.getLayoutParams();
		if (voice) {
			params.width = itemView.getResources()
					.getDimensionPixelSize(R.dimen.message_bubble_voice_width);
			params.height = itemView.getResources()
					.getDimensionPixelSize(R.dimen.message_bubble_voice_height);
			params.setFullSpan(true);
		} else {
			int width = needsStretch ? imageSize * 2 : imageSize;
			params.width = single ? a.getThumbnailWidth() : width;
			params.height = single ? a.getThumbnailHeight() : imageSize;
			params.setFullSpan(!single && needsStretch);
		}
		itemView.setLayoutParams(params);
		ViewGroup.LayoutParams imageParams = imageView.getLayoutParams();
		imageParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		imageParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
		imageView.setLayoutParams(imageParams);
	}

	private void loadImage(AttachmentItem a, Radii r) {
		Transformation<Bitmap> transformation = new BriarImageTransformation(r);
		GlideApp.with(imageView)
				.load(a.getHeader())
				.diskCacheStrategy(NONE)
				.error(ERROR_RES)
				.transform(transformation)
				.transition(withCrossFade())
				.into(imageView)
				.waitForLayout();
	}

}
