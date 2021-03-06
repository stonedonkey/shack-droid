package com.stonedonkey.shackdroid;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AdapterMessages extends BaseAdapter {

	private Context context;
	private ArrayList<ShackMessage> messages;
	private int rowResouceID;
	static Typeface face;
	
	public AdapterMessages(Context context, ArrayList<ShackMessage> items, int rowResouceID)
	{
		this.context = context;
		this.messages = items;
		this.rowResouceID = rowResouceID;
		face = Typeface.createFromAsset(context.getAssets(), "fonts/arial.ttf");
	}
	
	
	@Override
	public int getCount() {
		return messages.size();
	}

	@Override
	public Object getItem(int position) {
		return messages.get(position);
	}

	@Override
	public long getItemId(int position) {
		ShackMessage msg = messages.get(position);
		return  Long.parseLong(msg.getMsgID());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ShackMessage msg = messages.get(position);
		LayoutInflater inflate = LayoutInflater.from(context);
		
		View v = inflate.inflate(rowResouceID,parent,false);

		TextView auth = (TextView)v.findViewById(R.id.TextViewMessageAuthor);
		auth.setText(msg.getName());
		auth.setTypeface(face);
		
		TextView dp =  (TextView)v.findViewById(R.id.TextViewMessageDatePosted);
		dp.setText(msg.getMsgDate());
		dp.setTypeface(face);
		
		TextView sub = (TextView)v.findViewById(R.id.TextViewMessageSubject);
		String result = msg.getMsgSubject();
		result = result.replaceAll("(\r\n|\r|\n|\n\r)", "");
		sub.setText(result);
		sub.setTypeface(face);
				
		return v;
	}

}
