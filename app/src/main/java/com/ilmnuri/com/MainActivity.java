package com.ilmnuri.com;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;
import com.ilmnuri.com.adapter.DesignDemoRecyclerAdapter;
import com.ilmnuri.com.api.IlmApi;
import com.ilmnuri.com.application.IlmApplication;
import com.ilmnuri.com.model.AlbumModel;
import com.ilmnuri.com.model.Audio;
import com.ilmnuri.com.model.Category;
import com.ilmnuri.com.model.Global;
import com.ilmnuri.com.model.ListAlbumResult;
import com.ilmnuri.com.utility.CacheUtils;
import com.ilmnuri.com.utility.Utils;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainActivity extends BaseActivity {
    private DesignDemoPagerAdapter adapter;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    private boolean isReceiverRegistered;
    private DrawerLayout mDrawerLayout;

    static String searchKey;
    Gson mGson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGson = new Gson();
        searchKey = "";

        // Checks permission for marshmallow (android 6.x)
        checkPermission();

        // Register work with GCM
        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
                    mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };
        mInformationTextView = (TextView) findViewById(R.id.informationTextView);

        // Registering BroadcastReceiver
        registerReceiver();

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem menuItem) {
                    menuItem.setChecked(true);
                    mDrawerLayout.closeDrawers();
                    Toast.makeText(MainActivity.this, menuItem.getTitle(), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                    startActivity(intent);
                    return true;
                }

            });
        }


        adapter = new DesignDemoPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        if (viewPager != null) {
            viewPager.setAdapter(adapter);
        }
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        if (tabLayout != null) {
            tabLayout.setupWithViewPager(viewPager);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 123: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Utils.showToast(MainActivity.this, "Diskga yozishga izn berildi!");

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Intent intent = new Intent(this, PermDenied.class);
                    startActivity(intent);

                }
            }
        }
    }

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Intent intent = new Intent(this, PermDenied.class);
                startActivity(intent);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        123);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    private void registerReceiver() {
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    public static ArrayList<AlbumModel> filter(ArrayList<AlbumModel> albumModels, String key) {
        if (TextUtils.isEmpty(key)) {
            return albumModels;
        } else {
            ArrayList<AlbumModel> arrayList = new ArrayList<>();
            for (AlbumModel albumModel : albumModels) {
                for (Audio track : albumModel.getAudios()) {

                    if (track.getTrackName().contains(key) || track.getTrackName().contains(Utils.capitalize(key))) {
                        arrayList.add(albumModel);
                        break;
                    }
                }
            }
            return arrayList;
        }

    }

    public static String getSearchKey() {
        return searchKey;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_second, menu);


        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // perform query here
                searchKey = query;
                adapter.notifyDataSetChanged();
                // workaround to avoid issues with some emulators and keyboard devices firing twice if a keyboard enter is used
                // see https://code.google.com/p/android/issues/detail?id=24599
                searchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // perform query here
                if (newText.length() == 0) {
                    searchKey = newText;
                    adapter.notifyDataSetChanged();
                }

                return true;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
//                searchKey = "";
//                adapter.notifyDataSetChanged();
                return true;
            }
        });
        searchItem.setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.about_us:
                startActivity(new Intent(MainActivity.this, AboutUsActivity.class));
                return true;
            case R.id.action_search:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class DesignDemoFragment extends Fragment {
        private static final String TAB_POSITION = "tab_position";
        String[] category;

        @Bind(R.id.recycler_view)
        RecyclerView mRecyclerView;
        @Bind(R.id.swipe_refresh_layout)
        SwipeRefreshLayout mSwipeRefreshLayout;

        @Inject
        IlmApi mApi;

        @Inject
        CacheUtils mCacheUtils;
        private ArrayList<AlbumModel> albumModels;

        public DesignDemoFragment() {
            category = new String[3];
            category[0] = Category.category1;
            category[1] = Category.category2;
            category[2] = Category.category3;

        }

        public static DesignDemoFragment newInstance(int tabPosition) {
            DesignDemoFragment fragment = new DesignDemoFragment();
            Bundle args = new Bundle();
            args.putInt(TAB_POSITION, tabPosition);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            ((IlmApplication) getActivity().getApplication()).inject(this);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);
            View v = inflater.inflate(R.layout.fragment_list_view, container, false);
            ButterKnife.bind(this, v);

            String searchKey = MainActivity.getSearchKey();
            ArrayList<AlbumModel> arrayList = Global.getInstance().getAlbums(category[tabPosition]);
            arrayList = MainActivity.filter(arrayList, searchKey);

            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.setAdapter(new DesignDemoRecyclerAdapter(arrayList, listener));

            mSwipeRefreshLayout.requestDisallowInterceptTouchEvent(true);
            mSwipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.primary_dark);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    reloadContent();
                }
            });
            return v;
        }

        private void reloadContent() {
            albumModels = new ArrayList<>();
            mApi.getAlbums(new Callback<ListAlbumResult>() {
                @Override
                public void success(ListAlbumResult listAlbumResult, Response response) {
                    if (listAlbumResult != null) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        albumModels.addAll(listAlbumResult.getAlbumModels());
                        Global.getInstance().setArrayList(albumModels);
                        Bundle args = getArguments();
                        int tabPosition = args.getInt(TAB_POSITION);
                        ArrayList<AlbumModel> arrayList = Global.getInstance().getAlbums(category[tabPosition]);
                        arrayList = MainActivity.filter(arrayList, searchKey);

                        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                        mRecyclerView.setAdapter(new DesignDemoRecyclerAdapter(arrayList, listener));
                        mCacheUtils.saveToCache(albumModels);
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    if (error.getKind() == RetrofitError.Kind.NETWORK) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        Utils.showToast(getActivity(), "Internetga ulangan emassiz yoki Tarmoqda biron bir xatolik bo'ldi!");
                        Global.getInstance().setArrayList(mCacheUtils.getAlbums());
                    }
                }

            });


        }


        DesignDemoRecyclerAdapter.OnItemClickListener listener = new DesignDemoRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AlbumModel item) {
                Intent intent = new Intent(getContext(), AlbumActivity.class);
                Gson gson = new Gson();
                intent.putExtra("album", gson.toJson(item));
                startActivity(intent);
            }
        };
    }

    static class DesignDemoPagerAdapter extends FragmentStatePagerAdapter {

        public DesignDemoPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return DesignDemoFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return "Abdulloh";

            } else if (position == 1) {
                return "AbuNur";

            } else {
                return "Ayyubxon";

            }

        }


        @Override
        public int getItemPosition(Object object) {///add this method to refresh adapter 2016.3.7 hic
            return POSITION_NONE;
        }
    }

}
