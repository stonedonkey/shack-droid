package com.stonedonkey.shackdroid;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StrikethroughSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.stonedonkey.shackdroid.ShackGestureListener.ShackGestureEvent;
import com.stonedonkey.shackdroid.ShackPopup.ShackPopupEvent;

/**
 * @author markh
 * 
 */
public class ActivityThreadedView extends ListActivity implements Runnable, ShackGestureEvent, ShackPopupEvent {

	private static final int POST_REPLY = 0;
	private ArrayList<ShackPost> posts;
	private String storyID;
	private String postID;
	private String errorText = "";
	private int currentPosition = 0;
	private Boolean threadLoaded = true;
	private Boolean isNWS = false;

	private SharedPreferences prefs;// = PreferenceManager.getDefaultSharedPreferences(this);
	private String login;// = prefs.getString("shackLogin", "");

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		Helper.SetWindowState(getWindow(), this);

		boolean screenOn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keepScreenOn", false);
		if (screenOn) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		ShackGestureListener listener = Helper.setGestureEnabledContentView(R.layout.thread, this);
		if (listener != null) {
			listener.addListener(this);
		}

		this.setTitle("ShackDroid - View Thread");

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		login = prefs.getString("shackLogin", "");

		if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
			final Uri uri = getIntent().getData();

			postID = uri.getQueryParameter("id");
			storyID = null; // TODO: Actually get this some how

		}
		else {
			final Bundle extras = this.getIntent().getExtras();
			postID = extras.getString("postID");
			storyID = extras.getString("storyID");
			isNWS = extras.getBoolean("isNWS");
		}
		if (savedInstanceState == null) {
			try {
				fillSaxData(postID);
			}
			catch (Exception ex) {
				new AlertDialog.Builder(this).setTitle("Error").setPositiveButton("OK", null).setMessage("There was an error or could not connect to the API.").show();
			}
		}

		pop = new ShackPopup();
		pop.addListener(this);
		w = pop.Init(this, w);

	}

	ShackPopup pop;
	PopupWindow w;

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		setWindowSizes();

	}

	private void setWindowSizes() {

		final Boolean navBar = prefs.getBoolean("showNavigationBars", true);
		final ImageView prev = (ImageView) findViewById(R.id.ImageViewPreviousPost);
		final ImageView next = (ImageView) findViewById(R.id.ImageViewNextPost);
		final ImageView reload = (ImageView) findViewById(R.id.ImageViewReload);
		final RelativeLayout rl = (RelativeLayout) findViewById(R.id.RelativeLayoutThreadNav);

		if (navBar) {
			prev.setVisibility(View.VISIBLE);
			next.setVisibility(View.VISIBLE);
			reload.setVisibility(View.VISIBLE);
			rl.setVisibility(View.VISIBLE);
		}
		else {
			prev.setVisibility(View.GONE);
			next.setVisibility(View.GONE);
			reload.setVisibility(View.GONE);
			rl.setVisibility(View.GONE);
		}

		// Adjust the scroll view based on the size of the screen
		// this doesn't account for the titlebar or the statusbar
		// no methods appear to be available to determine them
		final ScrollView sv = (ScrollView) findViewById(R.id.textAreaScroller);
		final TextView tv = (TextView) findViewById(R.id.TextViewThreadAuthor);

		final int statusTitleBar = 0; // TODO: really would like to not hardcode this

		final int offset = tv.getTotalPaddingTop() + tv.getHeight() + sv.getTop();
		final int height = getWindowManager().getDefaultDisplay().getHeight();

		sv.getLayoutParams().height = ((height - offset - statusTitleBar) / 2) - rl.getHeight();
		sv.requestLayout();

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

		try {
			dismissDialog(1);
		}
		catch (Exception ex) {
			// dialog could not be killed for some reason
		}

		savedInstanceState.putSerializable("posts", posts);
		savedInstanceState.putString("storyID", storyID);
		savedInstanceState.putString("postID", postID);
		savedInstanceState.putInt("currentPosition", currentPosition);
		savedInstanceState.putBoolean("threadLoaded", threadLoaded);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		posts = (ArrayList<ShackPost>) savedInstanceState.getSerializable("posts");
		storyID = savedInstanceState.getString("storyID");
		postID = savedInstanceState.getString("postID");
		currentPosition = savedInstanceState.getInt("currentPosition");
		threadLoaded = savedInstanceState.getBoolean("threadLoaded");

		if (threadLoaded == true && posts != null) {
			ShowData();
			final ListView lv = getListView();
			lv.setSelection(currentPosition);
		}
		else {
			fillSaxData(postID);
		}

		savedInstanceState.clear();

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id)
		{
		case 1:
		{
			final ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage("loading, please wait...");
			dialog.setTitle(null);
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);

			dialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					finish();
				}
			});

			return dialog;
		}
		}
		return null;

	}

	private void fillSaxData(String postID) {

		if (postID == null)
			return;
		
		// show a progress dialog
		showDialog(1);


		// use the class run() method to do work
		final Thread thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {

		final Comparator<ShackPost> byPostID = new Helper.SortByPostIDComparator();
		final Comparator<ShackPost> byOrderID = new Helper.SortByOrderIDComparator();
		threadLoaded = false;
		try {

			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String feedURL = prefs.getString("shackFeedURL", getString(R.string.default_api));

			// TODO: Once Squeegy updates his api to work with NWS
			// we can remove this.
			if (isNWS)
				feedURL = "http://shackapi.stonedonkey.com";

			final URL url = new URL(feedURL + "/thread/" + postID + ".xml");

			// Get a SAXParser from the SAXPArserFactory.
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			final SAXParser sp = spf.newSAXParser();

			// Get the XMLReader of the SAXParser we created.
			final XMLReader xr = sp.getXMLReader();

			// Create a new ContentHandler and apply it to the XML-Reader
			final SaxHandlerTopicView saxHandler = new SaxHandlerTopicView(this, "threaded");
			xr.setContentHandler(saxHandler);

			// Parse the xml-data from our URL.
			xr.parse(new InputSource(HttpHelper.HttpRequestWithGzip(url.toString(), this)));

			// Our ExampleHandler now provides the parsed data to us.
			posts = saxHandler.GetParsedPosts();

			// from various portions of the app you can get here without a
			// story id, we retrieve this in the sax call, so mide as well
			// set it here if it's missing
			if (storyID == null)
				storyID = saxHandler.getStoryID();

			// the following sorts are what are used to highlight the last ten posts. We add an index to the
			// array by sorting them by post id, then adding the index, then sorting them back
			// sort our posts by PostID
			Collections.sort(posts, byPostID);

			// set the index on them based on order
			ShackPost tempPost = null;
			final int postsSize = posts.size();
			for (int x = 0; x < postsSize; x++) {
				tempPost = posts.get(x);
				tempPost.setPostIndex(x);
			}

			// set the order back to the original sort
			Collections.sort(posts, byOrderID);

		}
		catch (Exception ex) {
			Log.e("ShackDroid", "Unable to parse story " + this.postID);
			errorText = "An error occurred connecting to API.";
		}
		threadLoaded = true;

		progressBarHandler.sendEmptyMessage(0);
	}

	private Handler progressBarHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			try {
				dismissDialog(1);
			}
			catch (Exception ex) {
				// dialog could not be killed for some reason
			}

			// if we are provided a postID that is not the same as the first
			// item we need to find it and setit
			if (posts != null && posts.size() > 0 && postID != null)
				if (postID.equalsIgnoreCase(posts.get(0).getPostID()) == false)
					for (int x = 0; x < posts.size(); x++)
						if (posts.get(x).getPostID().equalsIgnoreCase(postID)) {
							currentPosition = x;
							break;
						}

			ShowData();
			UpdateWatchedPosts();

		}
	};

	@SuppressWarnings("unchecked")
	private void UpdateWatchedPosts() {
		// TODO: the opening and saving of the cache file needs to be moved to a func
		ArrayList<ShackPost> watchCache = null;
		if (getFileStreamPath("watch.cache").exists()) {

			try {
				synchronized (Helper.dataLock) {
					final FileInputStream fileIn = openFileInput("watch.cache");
					final ObjectInputStream in = new ObjectInputStream(fileIn);
					watchCache = (ArrayList<ShackPost>) in.readObject();
					in.close();
					fileIn.close();
				}
			}
			catch (Exception ex) {
				Log.e("ShackDroid", "Thread Error Loading watch.cache");
			}
		}

		try {
			if (watchCache != null && watchCache.size() > 0 && posts != null) {
				// TODO: Not sure a loop is necessary here
				for (int counter = 0; counter < watchCache.size(); counter++) {
					if (watchCache.get(counter).getPostID().equals(postID)) {
						final int replyCount = posts.size() - 1;
						watchCache.get(counter).setOriginalReplyCount(replyCount);
						watchCache.get(counter).setReplyCount(replyCount);
						break;
					}
				}
			}
		}
		catch (Exception ex) {
			Log.e("ShackDroid", "Error UpdateWatchedPosts() in ActivityThreadedView.java");
		}

		try {
			synchronized (Helper.dataLock) {
				final FileOutputStream fos = openFileOutput("watch.cache", MODE_PRIVATE);
				final ObjectOutputStream os = new ObjectOutputStream(fos);
				os.writeObject(watchCache);
				os.close();
				fos.close();
			}
		}
		catch (Exception e) {
		}

	}

	private void UpdatePostText(int position, Boolean addSpoilerMarkers) {
		if (posts == null || posts.size() < position - 1) // can't bind an empty list of posts
			return;

		final TextView tv = (TextView) findViewById(R.id.TextViewPost);
		final String postText = Helper.ParseShackText(posts.get(position).getPostText(), addSpoilerMarkers);

		final Spanned parsedText = Html.fromHtml(postText, imgGetter, new TagHandler() {
			int startPos = 0;

			@Override
			public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {

				if (tag.equals("s")) {
					if (opening) {
						startPos = output.length();
					}
					else {
						final StrikethroughSpan strike = new StrikethroughSpan();
						/*
						 * // This doesn't work and the colour is really dark normally :( TextPaint p = new TextPaint(); p.setStrokeWidth(2); p.setColor(Color.RED); p.setFlags(TextPaint.STRIKE_THRU_TEXT_FLAG); strike.updateDrawState(p);
						 */
						output.setSpan(strike, startPos, output.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}

			}
		});

		ScrollView sv = (ScrollView) findViewById(R.id.textAreaScroller);
		if (sv != null) {
			sv.scrollTo(0, 0);
		}

		if (tv != null) {
			tv.setText(parsedText, BufferType.NORMAL);
		}
		// tv.setText(Html.fromHtml(postText),BufferType.SPANNABLE);

		if (addSpoilerMarkers == true)
			SpoilerTextView();

		// TODO: This was causing links to break for some reason, for instance
		// http://pancake_humper.shackspace.com/
		// it removes the pancake_ and goes to http://humper.shackspace... bug in linkify maybe??
		Linkify.addLinks(tv, Linkify.ALL); // make all hyperlinks clickable
		tv.setClickable(false);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		// tv.scrollTo(0,0);
		tv.requestLayout();

		/*
		 * Log.i("height", String.valueOf(tv.getHeight())); tv.forceLayout(); findViewById(R.id.textAreaScroller).forceLayout(); Log.i("height", String.valueOf(tv.getHeight()));
		 */
		final ShackPost post = posts.get(position);
		final TextView posterName = (TextView) findViewById(R.id.TextViewThreadAuthor);
		posterName.setText(post.getPosterName());

		if (login.equalsIgnoreCase(post.getPosterName()))
			posterName.setTextColor(Color.parseColor("#00BFF3"));
		else
			posterName.setTextColor(Color.parseColor("#FFBA00"));

		final TextView postDate = (TextView) findViewById(R.id.TextViewThreadViewPostDate);
		postDate.setText(Helper.FormatShackDate(post.getPostDate()));

		final String postCat = post.getPostCategory();
		setPostCategoryIcon(postCat);

		postID = post.getPostID();

		ShackDroidStats.AddPostsViewed(this);
	}

	private void ShowData() {

		if (posts != null) {
			// this is where we bind our fancy ArrayList of posts
			final AdapterThreadedView tva = new AdapterThreadedView(this, R.layout.thread_row, posts, currentPosition);
			setListAdapter(tva);

			if (posts.size() > 0) {
				final ImageView threadTime = (ImageView) findViewById(R.id.ImageViewThreadTimer);
				threadTime.setImageResource(Helper.GetTimeLeftDrawable(posts.get(0).getPostDate()));
			}

			UpdatePostText(currentPosition, true);

			final ListView lv = getListView();
			lv.setSelection(currentPosition);

			lv.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
				@Override
				public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
					menu.setHeaderTitle("Options");
					menu.add(0, 2, 0, "Copy Post Url to Clipboard");
					menu.add(0, 4, 0, "Shacker's Chatty Profile");
					// menu.add(0, -1, 0, "Cancel");
				}
			});

			// set the post background color to be more "shack" like
			final LinearLayout layout = (LinearLayout) findViewById(R.id.RelativeLayoutThread);
			layout.setBackgroundColor(Color.parseColor("#222222"));

			// add a listener for removing spoilers and maybe adding "copy" functionality later
			final TextView tvpost = (TextView) findViewById(R.id.TextViewPost);
			tvpost.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
				@Override
				public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
					menu.setHeaderTitle("Post Options");
					menu.add(0, 1, 0, "Copy Post Url to Clipboard");
					menu.add(0, 3, 0, "Thread Expires In?");
					menu.add(0, 4, 0, "Shacker's Chatty Profile");
					// menu.add(0, -1, 0, "Cancel"); //unnecessary? with back button and click outside of options..
				}
			});

		}
		else {
			try {
				if (errorText.length() > 0) {
					new AlertDialog.Builder(this).setTitle("Error").setPositiveButton("OK", null).setMessage(errorText).show();
				}
			}
			catch (Exception ex) {
				// problem throwing up alert

			}
		}

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		w.dismiss();

		TextView threadPreview = null;
		View vi = (View) l.getChildAt(currentPosition - l.getFirstVisiblePosition());

		if (vi != null)
			threadPreview = (TextView) vi.findViewById(R.id.TextViewThreadPreview);
		if (threadPreview != null)
			threadPreview.setBackgroundColor(Color.TRANSPARENT);

		vi = (View) l.getChildAt(position - l.getFirstVisiblePosition());
		if (vi != null)
			threadPreview = (TextView) vi.findViewById(R.id.TextViewThreadPreview);
		if (threadPreview != null)
			threadPreview.setBackgroundColor(Color.parseColor("#274FD3"));

		// tell our adapter what the current row is, this is used to rehighlight
		// the current topic during scrolling
		final AdapterThreadedView tva = (AdapterThreadedView) getListAdapter();
		tva.setSelectedRow(position);

		currentPosition = position;
		l.setFocusableInTouchMode(true);

		UpdatePostText(position, true);
	}

	private void setListItemPosition(int direction) {
		w.dismiss();

		if ((currentPosition + direction) < 0)
			return;

		ListView l = getListView();

		if (currentPosition + direction >= l.getCount())
			return;

		TextView threadPreview = null;

		View vi = (View) l.getChildAt(currentPosition - l.getFirstVisiblePosition());
		if (vi != null)
			threadPreview = (TextView) vi.findViewById(R.id.TextViewThreadPreview);
		if (threadPreview != null)
			threadPreview.setBackgroundColor(Color.TRANSPARENT);
		else {

		}

		int position = currentPosition;

		final int rows = l.getLastVisiblePosition() - l.getFirstVisiblePosition();

		l.setFocusableInTouchMode(true);
		l.setSelection(position + direction + -(rows / 2));

		l.refreshDrawableState();

		vi = null;
		threadPreview = null; // clear the last thread selection
		vi = (View) l.getChildAt((position + direction) - l.getFirstVisiblePosition());
		if (vi != null)
			threadPreview = (TextView) vi.findViewById(R.id.TextViewThreadPreview);
		if (threadPreview != null)
			threadPreview.setBackgroundColor(Color.parseColor("#274FD3"));

		// tell our adapter what the current row is, this is used to rehighlight
		// the current topic during scrolling
		final AdapterThreadedView tva = (AdapterThreadedView) getListAdapter();
		tva.setSelectedRow(position + direction);

		currentPosition = position + direction;

		UpdatePostText(position + direction, true);

	}

	private void SpoilerTextView() {
		// We have to use the Spannable interface to handle spoilering text
		// not the best but works.
		final TextView tv = (TextView) findViewById(R.id.TextViewPost);

		final String text = tv.getText().toString();

		// early out if there are no spoilers in post
		if (text.indexOf("!!-") == -1)
			return;

		// need to allow clicks on this TextView
		tv.setMovementMethod(LinkMovementMethod.getInstance());

		// avoid annoying orange flicker when clicking on spoiler
		tv.setHighlightColor(Color.parseColor("#222222"));

		// replace end marker with start marker so we can split on only one of the markers
		String components[] = text.replaceAll("-!!", "!!-").split("!!-");

		StringBuilder cleanText = new StringBuilder();
		ArrayList<int[]> spoilerSections = new ArrayList<int[]>();

		// build up a list of spoiler sections and a clean version of the post text (no !!- or -!! markers)
		for (int i = 0; i < components.length; i++) {
			if (i % 2 == 1) // odd strings in the list are the spoilers
			{
				// store start and length of spoiler section
				spoilerSections.add(new int[] { cleanText.length(), components[i].length() });
			}

			cleanText.append(components[i]);
		}

		tv.setText(cleanText);

		// add clickable spans to each spoiler section
		Spannable str = (Spannable) tv.getText();
		for (int[] section : spoilerSections)
			str.setSpan(new SpoilerSpan(tv), section[0], section[0] + section[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	private void setPostCategoryIcon(String postCat) {
		final ImageView img = (ImageView) findViewById(R.id.ImageViewCatTopic);
		// TODO: clean this up a little / also duplicated in Topic View Adapter ick
		if (postCat.equals("offtopic"))
			img.setImageResource(R.drawable.offtopic);
		else if (postCat.equals("nws"))
			img.setImageResource(R.drawable.nws);
		else if (postCat.equals("political"))
			img.setImageResource(R.drawable.political);
		else if (postCat.equals("stupid"))
			img.setImageResource(R.drawable.stupid);
		else if (postCat.equals("informative"))
			img.setImageResource(R.drawable.interesting);
		else {
			// chazums, commented out as throwing an exception
			// img.setImageResource(-1);
			img.setImageDrawable(null);
		}
	}

	// menu creation
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(1, 0, 4, "Reply").setIcon(R.drawable.menu_reply);
		// menu.add(1, 1, 1, "Settings").setIcon(R.drawable.menu_settings);
		menu.add(1, 2, 1, "Back").setIcon(R.drawable.menu_back);
		menu.add(1, 3, 2, "Refresh").setIcon(R.drawable.menu_reload);

		SubMenu sub = menu.addSubMenu(1, 4, 3, "LOL/INF/UNF/TAG").setIcon(R.drawable.menu_lolinf);
		sub.add(0, 8, 0, "LOL Post");
		sub.add(0, 9, 1, "INF Post");
		sub.add(0, 11, 1, "UNF Post");
		sub.add(0, 12, 1, "TAG Post");
		sub.add(0, 10, 2, "Cancel");

		// sub = menu.addSubMenu(1, 5, 5, "ShackMarks").setIcon(R.drawable.menu_mark);
		// sub.add(0,6,0,"View saved ShackMarks");
		// sub.add(0,7,1, "Save to ShackMarks").setIcon(R.drawable.menu_shacktags);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		w.dismiss();
		Intent intent;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String login = "";
		switch (item.getItemId())
		{
		case 0: // Launch post form
			doReply();
			return true;
		case 1:
			// show settings dialog
			intent = new Intent();
			intent.setClass(this, ActivityPreferences.class);
			startActivity(intent);
			return true;
		case 2:
			finish();
			return true;
		case 3:
			this.currentPosition = 0;
			this.fillSaxData(postID);
			return true;
		case 6:
			LaunchNotesIntent();
			return true;
		case 7: // sub menu for ShackMarks
			HandlerExtendedSites.AddRemoveShackMark(this, postID, false);
			return true;
		case 8:
			// lol post
			login = prefs.getString("shackLogin", "");
			HandlerExtendedSites.INFLOLPost(this, login, postID, "LOL");
			return true;
		case 9:
			// inf post
			login = prefs.getString("shackLogin", "");
			HandlerExtendedSites.INFLOLPost(this, login, postID, "INF");
			return true;
		case 10: // cancel lol/unf/tag/ing
			return true;
		case 11:
			// unf post
			login = prefs.getString("shackLogin", "");
			HandlerExtendedSites.INFLOLPost(this, login, postID, "UNF");
			return true;
		case 12:
			// tag post
			login = prefs.getString("shackLogin", "");
			HandlerExtendedSites.INFLOLPost(this, login, postID, "TAG");
			return true;

		}
		return false;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		w.dismiss();
		switch (item.getItemId())
		{
		case 1:
		{
			// http://www.shacknews.com/chatty?id=25445895
			final String url = "http://www.shacknews.com/chatty?id=" + postID;
			final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(url);
			Toast.makeText(this, "Link to post copied to clipboard.", Toast.LENGTH_SHORT).show();
		}
		case 2:
		{

			String linkID = posts.get(currentPosition).getPostID();

			// http://www.shacknews.com/laryn.x?id=23005222#itemanchor_23005222
			final String url = "http://www.shacknews.com/chatty?id=" + linkID + "#itemanchor_" + linkID;
			final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(url);
			Toast.makeText(this, "Link to post copied to clipboard.", Toast.LENGTH_SHORT).show();
		}
		case 3:
		{
			String datePosted = posts.get(currentPosition).getPostDate();

			new AlertDialog.Builder(this).setTitle("Thread Will Expire In:").setMessage(Helper.GetTimeLeftString(datePosted)).setNegativeButton("OK", null).show();

			return true;
		}
		case 4:
		{
			String shackname = posts.get(currentPosition).getPosterName();

			Intent intent = new Intent();
			intent.putExtra("shackname", shackname);
			intent.setClass(this, ActivityProfile.class);
			startActivity(intent);

			return true;
		}
		}

		return false;

	}

	private void doReply() {
		Intent intent = new Intent();
		intent.setClass(this, ActivityPost.class);
		intent.putExtra("storyID", storyID);
		intent.putExtra("postID", postID);
		startActivityForResult(intent, POST_REPLY);
	}

	private void LaunchNotesIntent() {
		final Intent intent = new Intent();
		intent.setClass(this, ActivityShackMarks.class);
		startActivity(intent);
	}

	private ImageGetter imgGetter = new Html.ImageGetter() {
		@Override
		public Drawable getDrawable(String source) {
			Drawable drawable = null;

			try {

				InputStream is = (InputStream) new URL(source).getContent();
				drawable = Drawable.createFromStream(is, "youtubes");
				// Important
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth() * 2, drawable.getIntrinsicHeight() * 2);

			}
			catch (Exception ex) {
				// String exc = ex.getMessage();
				// int i = 1;
			}
			return drawable;
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode)
		{
		case POST_REPLY:
			if (resultCode == RESULT_OK) // only refresh on posts
				fillSaxData(postID);

			break;
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (!prefs.getBoolean("enableVolumeNav", false))
			return super.dispatchKeyEvent(event);

		int action = event.getAction();
		int keyCode = event.getKeyCode();

		switch (keyCode)
		{
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (action == KeyEvent.ACTION_DOWN)
				setListItemPosition(-1);

			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN)
				setListItemPosition(1);

			return true;
		default:
			return super.dispatchKeyEvent(event);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		/*
		 * ListView lv = getListView();
		 * 
		 * 
		 * 
		 * if (keyCode == 54) { if (lv.getCount() >= currentPosition+2 ){ currentPosition++; this.UpdatePostText(currentPosition, true); lv.setSelection(currentPosition); }
		 * 
		 * }
		 */
		if (keyCode == KeyEvent.KEYCODE_BACK && w.isShowing()) {
			w.dismiss();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void eventRaised(int eventType) {
		switch (eventType)
		{
		case ShackGestureListener.BACKWARD:
			finish();
			break;
		case ShackGestureListener.REFRESH:
			fillSaxData(postID);
			break;
		}

	}

	@Override
	public void PopupEventRaised(int eventType) {

		switch (eventType)
		{
		case ShackPopup.MESSAGE:
			ShackPost post = posts.get(currentPosition);// .getPostText();
			Intent intent = new Intent();
			intent.putExtra("postto", post.getPosterName());
			intent.setClass(this, ActivityPostMessage.class);
			startActivity(intent);
			break;
		case ShackPopup.REFRESH:
			fillSaxData(postID);
			break;
		case ShackPopup.REPLY:
			doReply();
			break;
		}
		w.dismiss();
	}
}
/**
 * Used to sort the post array based on the ID
 */

