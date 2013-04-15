package com.example.jimbraypull;

import java.util.Arrays;
import java.util.LinkedList;

import com.example.jimbraypull.PullView.RefreshListener;

import android.os.Bundle;
import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class JimbrayPullActivity extends Activity {
	private LinkedList<String> mLisItems;
	
	private String[] mStrings = {"Test-0", "Test-1", "Test-2",
            "Test-3", "Test-4", "Test-5", "Test-6", "Test-7",
            "Test-8", "Test-9", "Test-10", "Test-11",
            "Test-12"};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jimbray_pull);
		
		mLisItems = new LinkedList<String>();
		mLisItems.addAll(Arrays.asList(mStrings));
		
		
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_1, mLisItems);
		ListView listView = (ListView) findViewById(R.id.list_view);
		listView.setAdapter(adapter);
		
		final PullView pullView = (PullView) findViewById(R.id.pull_view);
		pullView.setEnablePull(true);
		pullView.setRefreshListener(new RefreshListener() {
			
			@Override
			public void onRefresh() {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						JimbrayPullActivity.this.runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								pullView.refreshFinished();
							}
						});
					}
				}).start();
			}
		});
	}

}
