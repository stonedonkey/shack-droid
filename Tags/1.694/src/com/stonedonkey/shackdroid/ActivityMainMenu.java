package com.stonedonkey.shackdroid;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ActivityMainMenu extends ListActivity  {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Helper.SetWindowState(getWindow(),this);
		
	
		setContentView(R.layout.mainmenu);

		
		//JSONTopicView jv = new JSONTopicView(this, null, null);
		//jv.GetParsedPosts();

		ArrayList<ShackMenuItem> menu = new ArrayList<ShackMenuItem>();

		menu.add(new ShackMenuItem("Latest Chatty","It gets you chicks, and diseases.",R.drawable.menu2_latestchatty));
		menu.add(new ShackMenuItem("Latest Stories", "The \"Mos Eisley\" of chatties.",R.drawable.menu2_rss));
		menu.add(new ShackMenuItem("Shack Search","For all your vanity needs.",R.drawable.menu2_search));
		menu.add(new ShackMenuItem("Shack Messages","Stuff too shocking for even the Shack.",R.drawable.menu2_shackmessages2));
		menu.add(new ShackMenuItem("Shack LOLs","You are not as popular as these people.",R.drawable.menu2_lol2));//
		//menu.add(new ShackMenuItem("Shack Marks","Your mobile tranny porn Stash.",R.drawable.menu2_shackmarks2));
		menu.add(new ShackMenuItem("Settings","Hay guys, am I doing this right?",R.drawable.menu2_settings));
		//menu.add(new ShackMenuItem("Version Check","stonedonkey finally did something new!?!",R.drawable.menu2_vercheck));
		//menu.add(new ShackMenuItem("What's New","How we most recently broke this thing.",R.drawable.menu2_cone));
		//menu.add(new ShackMenuItem("Stats","Keeping score, it's how you know you're better.",R.drawable.menu2_stats));
		//menu.add(new ShackMenuItem("Tester","Testing stuff before you can play with it!.",R.drawable.menu2_stats));

		AdapterMainMenu mm = new AdapterMainMenu(this,R.layout.mainmenu_row, menu);

		//getListView().setDivider("#333333");

		String[] urls = getResources().getStringArray(R.array.titles);
		int titles =urls.length;
		Random r = new Random();
		setTitle("ShackDroid - " + urls[r.nextInt(titles)]);

		ColorDrawable cd = new ColorDrawable(Color.parseColor("#333333"));
		getListView().setDivider(cd);
		getListView().setDividerHeight(0);

		
		setListAdapter(mm);

		CheckForUpdate(false);

	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		//chazums 'fix' for something in the xml crashing on 1.5
		if (hasFocus  && Integer.parseInt(android.os.Build.VERSION.SDK) > 3)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			boolean allowMenuAnimations = prefs.getBoolean("allowMenuAnimations", true);
			
			final ImageView imageBackground = (ImageView) findViewById(R.id.ImageViewMainMenuBackground);
			if (allowMenuAnimations) 
			{
				imageBackground.setBackgroundResource(R.drawable.shackcrest);
				Animation anim = AnimationUtils.loadAnimation(getBaseContext(), R.anim.move_rotate);
				imageBackground.setAnimation(anim);
				anim = null;
			}
			else
			{
				imageBackground.setVisibility(View.GONE);
			}
		}
	}

	// TODO: this is copied in ActivityPreferences, need to move to it's own class
	private void CheckForUpdate(boolean force) {
		// have we seen this update?
		try {
			String vc = null;
			vc = getString(R.string.version_id);
			final SharedPreferences settings=getPreferences(0);

			// NOTE: debugging resets value
			//SharedPreferences.Editor editor = settings.edit();
			//editor.putBoolean("hasSeenUpdatedVersion" + vc, false);
			//editor.commit(); 
			boolean hasSeenUpdatedVersion = settings.getBoolean("hasSeenUpdatedVersion" + vc, false);

			if (!hasSeenUpdatedVersion || force)
			{
				final String result = HandlerExtendedSites.WhatsNew(getResources().getString(R.string.version_id),this);

				Context mContext = getApplicationContext();
				LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
				View layout = inflater.inflate(R.layout.whats_new_dialog,
				                               (ViewGroup) findViewById(R.id.layout_root));

				TextView text = (TextView) layout.findViewById(R.id.TextViewWhatsNewDialogText);
				text.setText(Html.fromHtml(result));
					
				new AlertDialog.Builder(this)
				.setTitle("What's New " + vc)
				.setPositiveButton("OK",null)
				.setIcon(R.drawable.icon)
				.setView(layout).show();
				

				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("hasSeenUpdatedVersion" + vc, true);
				editor.commit(); 

			}
		}
		catch (Exception ex)
		{
			// do nothing
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Intent intent = new Intent();;
		switch (position) {
		case 0: // LatestChatty
		{
			ShackDroidStats.AddViewedChatty(this);
			intent.setClass(this, ActivityTopicView.class);
			break;
		}
		case 1: // RSS
		{
			ShackDroidStats.AddViewedRssFeed(this);
			intent.setClass(this,ActivityRSS.class);
			break;
		}
		case 2: // Shack Search 
		{
			intent.setClass(this,ActivitySearchTabs.class);
			break;

		}
		case 3: // shack messages
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			boolean allowSMs = prefs.getBoolean("allowShackMessages", false);

			if (allowSMs)
			{
				ShackDroidStats.AddViewShackMessage(this);
				intent.setClass(this,ActivityMessages.class);
				startActivity(intent);
				return;
			}
			else
			{
				new AlertDialog.Builder(this).setTitle("Information")
				.setPositiveButton("OK", null).setMessage(
						"Shack Messages posts your credentials to the API " +
						"instead of directly ShackNews.\n\n If you agree with this " +
				"you can enable this feature under \"Settings\"." )
				.show();

				return;
			}
		}
		case 4:  // shackLOL
		{
			ShackDroidStats.AddViewedShackLOLs(this);
			intent.setClass(this,ActivityLOLTabs.class);
			break;
		}
		case 5: // settings
		{
			intent.setClass(this,ActivityPreferences.class);
			break;
		}
		case 6: // tester
		{
			
			try {
			final URL url= new URL("http://shackapi.stonedonkey.com/thread/23141803.json");
			
			final InputStream json= HttpHelper.HttpRequestWithGzip(url.toString(),this);
			String jsonResult = convertStreamToString(json);
			

			JSONTopicView jv = new JSONTopicView(this, null, null);
			jv.GetParsedPostsAndroid(jsonResult);
			jv.GetParsedPostsJson(jsonResult);
			
			
			}
			catch (Exception ex)
			{
				
			}
			
			return;
			
		}
		}	

		
		startActivity(intent);

		
	}
	private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
	@Override
	protected void onResume() {
		super.onResume();
		//CheckForNewShackDroid(this);
		new CheckForNewShackDroidAsyncTask(this).execute();
	}	
}
class CheckForNewShackDroidAsyncTask extends AsyncTask<Void,Void,Integer>{

	private ActivityMainMenu context;
	public CheckForNewShackDroidAsyncTask(ActivityMainMenu context)
	{
		this.context = context; 

	}
	@Override
	protected Integer doInBackground(Void... arg0) {

		// LOL at MONSTER FUNCTIONSSLKJDSJDLKJ!!KLJ OMG Refactor much!
		
		boolean checkForUpdate = false;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean allowCheck = prefs.getBoolean("allowCheckForNewVersion", true);
		
		if (!allowCheck)
			return null;
		
		Calendar currentDate = Calendar.getInstance();
		Calendar lastMessageDate;
		try {
			if (context.getFileStreamPath("versioncheck.cache").exists()) {

				FileInputStream fileIn = context.openFileInput("versioncheck.cache");
				ObjectInputStream in = new ObjectInputStream(fileIn);
				lastMessageDate =  (Calendar) in.readObject();
				in.close();
				fileIn.close();

				// 8640000 = 1 day
				if (currentDate.getTimeInMillis() - lastMessageDate.getTimeInMillis() > 14400000 )
					checkForUpdate = true;
			}
			else 
			{ 	checkForUpdate = true;
			}
		}
		catch (Exception ex) { 
			Log.e("ShackDroid", "Error Loading Last Update Date: " + ex.getMessage());
			checkForUpdate = true;
		}
		//checkForUpdate = true;  // DEBUGGING SWITCH
		if (checkForUpdate)
		{
			final String result = HandlerExtendedSites.VersionCheck(context);
			if (result != null && result != "*fail*") 
			{
				// if we find a new version bake some toast

				String ns = Context.NOTIFICATION_SERVICE;
				NotificationManager nm = (NotificationManager)context.getSystemService(ns);

				int icon = R.drawable.shack_logo;
				CharSequence tickerText = "ShackDroid Update Available";

				Notification note = new Notification(icon,tickerText,0);

				CharSequence contentTitle = "ShackDroid Update Available";
				CharSequence contentText = "Touch here to download!";

				//Intent notificationIntent = new Intent("android.intent.action.VIEW", Uri.parse(result));
				Intent notificationIntent = new Intent("android.intent.action.VIEW", Uri.parse("market://search?q=pname:com.stonedonkey.shackdroid"));
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
				note.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

				note.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
				note.ledARGB = 0xFF800080;
				note.ledOnMS = 100;
				note.ledOffMS = 100;
				//note.defaults = Notification.DEFAULT_SOUND;
				note.sound = Uri.parse("android.resource://com.stonedonkey.shackdroid/" + R.raw.alert1);
				nm.notify(1,note);


				// update our cache file
				try {
					FileOutputStream fos = context.openFileOutput("versioncheck.cache",Context.MODE_PRIVATE);
					ObjectOutputStream os = new ObjectOutputStream(fos);
					os.writeObject(currentDate);
					os.close();
					fos.close();
				}
				catch (Exception ex)
				{
					Log.e("ShackDroid", "Error Saving Last New Version Check: " + ex.getMessage());
				}				
			}

		}
		return null;
	}
}

