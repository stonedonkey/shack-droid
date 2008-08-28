package com.stonedonkey.shackdroid;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TopicViewAdapter extends BaseAdapter {

	private Context context;
	private List<ShackPost> topicList;
	private int rowResouceID;
	
	public TopicViewAdapter(Context context,int rowResouceID, List<ShackPost> topicList ){
		this.context = context;
		this.topicList = topicList;
		this.rowResouceID = rowResouceID;
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
		//return position;
		ShackPost post = topicList.get(position);
		return Long.parseLong(post.getPostID());
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ShackPost post = topicList.get(position);
		LayoutInflater inflate = LayoutInflater.from(context);
		
		View v = inflate.inflate(rowResouceID,parent,false);
		
		
		String postCat = post.getPostCategory();
		
		// Returns the view which leaves a space.. this should really be
		// part of the SaxHandler, just need away to access the context
		
/*		// we only add posts that past the filters
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Boolean allowNWS = prefs.getBoolean("allowNWS", true);
		Boolean allowPolitical = prefs.getBoolean("allowPolitical", true);
		Boolean allowStupid = prefs.getBoolean("allowStupid", true);
		Boolean allowInteresting = prefs.getBoolean("allowInteresting", true);
		Boolean allowOffTopic = prefs.getBoolean("allowOffTopic", true);
		
		if (postCat.equals("offtopic") && allowOffTopic != true) 
			return v;
		else if (postCat.equals("nws") && allowNWS != true)
			return v;
		else if (postCat.equals("political") && allowPolitical != true) 
			return v;
		else if (postCat.equals("stupid") && allowStupid != true) 
			return v;
		else if (postCat.equals("informative") && allowInteresting != true)
			return v;*/

		
		// bind the TextViews to the items in our datasource
		TextView posterName = (TextView)v.findViewById(R.id.TextViewPosterName);
		if (posterName != null)
			posterName.setText(post.getPosterName());
		
		TextView postDate = (TextView)v.findViewById(R.id.TextViewDatePosted);
		if (postDate != null)
			postDate.setText(post.getPostDate());
		
		TextView postReplyCount = (TextView)v.findViewById(R.id.TextViewReplyCount);
		if (postReplyCount !=null)
			postReplyCount.setText(post.getReplyCount());
		
		TextView postText = (TextView)v.findViewById(R.id.TextViewPostText);
		if (postText != null)
			postText.setText(post.getPostPreview());
		
		
		ImageView img = (ImageView)v.findViewById(R.id.ImageViewCat);

		
		
		// TODO: clean this up a little / also replicated in ShackDroidThread ick
		if (postCat.equals("offtopic"))  {
			img.setImageResource(R.drawable.offtopic);
			//tr.setBackgroundColor(Color.parseColor("#081407"));
		}
		else if (postCat.equals("nws"))
			img.setImageResource(R.drawable.nws);
		else if (postCat.equals("political")) {
			img.setImageResource(R.drawable.political);
			//tr.setBackgroundColor(Color.parseColor("#211D1A"));
		}
		else if (postCat.equals("stupid")) {
			img.setImageResource(R.drawable.stupid);
			//tr.setBackgroundColor(Color.GREEN);
		}
		else if (postCat.equals("informative"))
			img.setImageResource(R.drawable.interesting);		
		
		
		return v;
		
	}
	
}
