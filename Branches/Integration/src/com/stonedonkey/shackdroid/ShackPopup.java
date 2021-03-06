package com.stonedonkey.shackdroid;

import java.util.ArrayList;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.PopupWindow;

public class ShackPopup {
	
	//PopupWindow w;
	public static final int REPLY = 0;
	public static final int WATCH = 1;
	public static final int REFRESH = 2;
	public static final int MESSAGE = 3;
	public static final int SPOIL = 4;
	
	public boolean isSmallScreen = false;
	private ArrayList<ShackPopupEvent> mListeners = new ArrayList<ShackPopupEvent>();
	public ShackPopup(){

	}
	
	public PopupWindow Init(Activity ctx, PopupWindow w){
		View v = ctx.getLayoutInflater().inflate(R.layout.popup, null, false);
		
		
		if ((ctx.getWindowManager().getDefaultDisplay().getHeight() == 480 &&
				ctx.getWindowManager().getDefaultDisplay().getWidth() == 320) ||
				(ctx.getWindowManager().getDefaultDisplay().getHeight() == 320 &&
						ctx.getWindowManager().getDefaultDisplay().getWidth() == 480)){
			w = new PopupWindow(v, 240, 64);
			isSmallScreen = true;
		}
		else{
			w = new PopupWindow(v, 480, 128);
		}
		w.setBackgroundDrawable(ctx.getResources().getDrawable(R.drawable.box));

		setupButton(v.findViewById(R.id.ivPopupReply), REPLY);
		setupButton(v.findViewById(R.id.ivPopupMessage), MESSAGE);
		setupButton(v.findViewById(R.id.ivPopupWatch), WATCH);
		setupButton(v.findViewById(R.id.ivPopupRefresh), REFRESH);
		setupButton(v.findViewById(R.id.ivPopupSpoil), SPOIL);
		
		return w;
	}
	
	public void addListener(ShackPopupEvent l){
		mListeners.add(l);
	}
	public void removeListener(ShackPopupEvent l){
		mListeners.remove(l);
	}
	
	private void setupButton(View v, final int event){
		v.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				raiseEvent(event);
			}});		
	}
	
	private void raiseEvent(int event){
		for(ShackPopupEvent e : mListeners){
			e.PopupEventRaised(event);
		}
	}
	public interface ShackPopupEvent{
		public void PopupEventRaised(int eventType);
	}
}
