package com.stonedonkey.shackdroid;

import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AdapterLimerifficTopic extends BaseAdapter {

	// private Context context;
	private List<ShackPost> topicList;
	private final int rowResouceID;
	private final String shackLogin;
	private final Typeface face;
	private final Hashtable<String, String> postCache;
	private final String showAuthor;
	private final Resources r;
	private int totalNewPosts = 0;

	LayoutInflater inflate;// = LayoutInflater.from(context);

	public AdapterLimerifficTopic(Context context, int rowResouceID, List<ShackPost> topicList, String shackLogin, Hashtable<String, String> postCache) {
		this.topicList = topicList;
		this.rowResouceID = rowResouceID;
		this.shackLogin = shackLogin;
		this.postCache = postCache;
		this.r = context.getResources();

		face = Typeface.createFromAsset(context.getAssets(), "fonts/arial.ttf");

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		showAuthor = prefs.getString("showAuthor", "count");

		inflate = LayoutInflater.from(context);
	}
	public void SetPosts(List<ShackPost> posts)
	{
		topicList = posts;
	}
	@Override
	public int getCount() {
		return topicList.size();
	}

	@Override
	public Object getItem(int position) {
		return topicList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// return position;
		final ShackPost post = topicList.get(position);
		return Long.parseLong(post.getPostID());
	}

	static class ViewHolder {
		TextView posterName;
		TextView datePosted;
		TextView replyCount;
		TextView newPosts;
		TextView postText;
		TextView viewCat;
		RelativeLayout topicRow;
		ImageView postTimer;
	}
	

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		// TextView tmp;
		// final View v;
		ViewHolder holder;
		
		if (position > topicList.size())
			return convertView;
		
		 ShackPost post = topicList.get(position);

		if (convertView == null) {
			convertView = inflate.inflate(rowResouceID, parent, false);
			holder = new ViewHolder();
			holder.posterName = (TextView) convertView.findViewById(R.id.TextViewLimeAuthor);
			holder.datePosted = (TextView) convertView.findViewById(R.id.TextViewLimePostDate);
			holder.replyCount = (TextView) convertView.findViewById(R.id.TextViewLimePosts);
			holder.newPosts = (TextView) convertView.findViewById(R.id.TextViewLimeNewPosts);
			holder.postText = (TextView) convertView.findViewById(R.id.TextViewLimePostText);
			holder.viewCat = (TextView) convertView.findViewById(R.id.TextViewLimeModTag);
			holder.topicRow = (RelativeLayout) convertView.findViewById(R.id.LimeTopicRow);
//			holder.postTimer = (ImageView) convertView.findViewById(R.id.ImageViewTopicTimer);

//			holder.posterName.setTypeface(face);
//			holder.datePosted.setTypeface(face);
//			holder.replyCount.setTypeface(face);
//			holder.newPosts.setTypeface(face);
//			holder.postText.setTypeface(face);

			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}

//		holder.postTimer.setImageResource(Helper.GetTimeLeftDrawable(post.getPostDate()));
//
		holder.posterName.setText(post.getPosterName());
//
//		if (shackLogin.equalsIgnoreCase(post.getPosterName()))
//			holder.posterName.setTextColor(Color.parseColor("#00BFF3"));
//		else
//			holder.posterName.setTextColor(Color.parseColor("#ffba00"));
//
		holder.datePosted.setText(Helper.FormatShackDateToTimePassed(post.getPostDate()));
		
		holder.replyCount.setText(post.getReplyCount());
//
//		if (showAuthor.equalsIgnoreCase("count") && post.getIsAuthorInThread())
//			holder.replyCount.setTextColor(Color.parseColor("#0099CC"));
//		else
//			holder.replyCount.setTextColor(Color.parseColor("#FFFFFF"));

		if (post.getOrginalReplyCount() != null) // we are showing watched
													// messages
		{
			Integer newPosts = 0;

			if (postCache != null && postCache.get(post.getPostID()) != null) {
				final String cacheposts = postCache.get(post.getPostID());
				newPosts = Integer.parseInt(cacheposts) - post.getOrginalReplyCount();
			}

			if (newPosts > 0) {
				totalNewPosts = totalNewPosts + newPosts;
				holder.newPosts.setText("+" + newPosts.toString());
				holder.newPosts.setVisibility(View.VISIBLE);
			}
			else {
				holder.newPosts.setText(null);
				holder.newPosts.setVisibility(View.INVISIBLE);
			}

		}
		else // show the number of new posts since the last refresh
		{
			if (postCache != null && postCache.get(post.getPostID()) != null) {

				final String cacheposts = postCache.get(post.getPostID());
				final Integer newPosts = Integer.parseInt(post.getReplyCount()) - Integer.parseInt(cacheposts);

				if (newPosts > 0 && Integer.parseInt(post.getReplyCount()) > 0) {
					holder.newPosts.setText("+" + newPosts.toString());
					holder.newPosts.setVisibility(View.VISIBLE);
				}
				else {
					holder.newPosts.setText(null);
					holder.newPosts.setVisibility(View.INVISIBLE);
				}

			}
			else // we don't have a cached version of this in the post cache
					// reset the view
			{
				holder.newPosts.setVisibility(View.INVISIBLE);
				holder.newPosts.setText(null);
			}
		}

		String preview = post.getPostPreview();
		//if (preview.length() > 99)
		//	preview = preview.substring(0, 99);

		holder.postText.setText(preview);


		if (showAuthor.equalsIgnoreCase("topic") && post.getIsAuthorInThread()) {
			final Drawable d = r.getDrawable(R.drawable.background_gradient_blue);
			holder.topicRow.setBackgroundDrawable(d);
		}
		else
			holder.topicRow.setBackgroundDrawable(null);
		
		
		// TODO: clean this up a little / also replicated in ShackDroidThread ick
		final String postCat = post.getPostCategory();
		holder.viewCat.setVisibility(View.VISIBLE);
		
		if (postCat.equals("offtopic"))  {
			holder.viewCat.setText("offtopic");
			holder.viewCat.setBackgroundColor(Color.parseColor("#444444"));
		}
		else if (postCat.equals("nws")) {
			holder.viewCat.setText("nws");
			holder.viewCat.setBackgroundColor(Color.parseColor("#CC0000"));
		}
		else if (postCat.equals("political")) {
			holder.viewCat.setText("political");
			holder.viewCat.setBackgroundColor(Color.parseColor("#FF8800"));
		}
		else if (postCat.equals("stupid")) {
			holder.viewCat.setText("stupid");
			holder.viewCat.setBackgroundColor(Color.parseColor("#669900"));
		}
		else if (postCat.equals("informative")) {
			holder.viewCat.setText("interesting");
			holder.viewCat.setBackgroundColor(Color.parseColor("#0099CC"));
		}
		else 
			holder.viewCat.setVisibility(View.GONE);		
		

		return convertView;
	}

	public int getTotalNewPosts() {
		return totalNewPosts;
	}

}
