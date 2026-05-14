package com.virtual.app.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.virtual.app.R;
import com.virtual.app.VirtualApplication;
import com.virtual.core.VirtualCore;
import com.virtual.util.VirtualLog;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int TAB_COUNT = 2;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private FloatingActionButton fabClone;

    private InstalledAppsFragment installedFragment;
    private ClonedAppsFragment clonedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!VirtualCore.get().isSupportVersion()) {
            VirtualLog.w(TAG, "Unsupported Android version: " + android.os.Build.VERSION.SDK_INT);
        }

        initViews();
        setupToolbar();
        setupViewPager();
        setupTabs();
        setupFab();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        fabClone = findViewById(R.id.fabClone);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    private void setupViewPager() {
        installedFragment = new InstalledAppsFragment();
        clonedFragment = new ClonedAppsFragment();

        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return TAB_COUNT;
            }

            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return installedFragment;
                    case 1:
                        return clonedFragment;
                    default:
                        return installedFragment;
                }
            }
        };

        androidx.viewpager2.widget.ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);
    }

    private void setupTabs() {
        new TabLayoutMediator(tabLayout, findViewById(R.id.viewPager),
            (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText(R.string.title_installed_apps);
                        break;
                    case 1:
                        tab.setText(R.string.title_cloned_apps);
                        break;
                }
            }
        ).attach();
    }

    private void setupFab() {
        fabClone.setOnClickListener(v -> {
            openCloneSelectActivity();
        });
    }

    private void openCloneSelectActivity() {
        android.content.Intent intent = new android.content.Intent(this, CloneSelectActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterApps(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterApps(newText);
                    return true;
                }
            });
        }

        return true;
    }

    private void filterApps(String query) {
        Fragment currentFragment = getSupportFragmentManager()
            .findFragmentByTag("f" + ((androidx.viewpager2.widget.ViewPager2) findViewById(R.id.viewPager)).getCurrentItem());
        if (currentFragment instanceof InstalledAppsFragment) {
            ((InstalledAppsFragment) currentFragment).filter(query);
        } else if (currentFragment instanceof ClonedAppsFragment) {
            ((ClonedAppsFragment) currentFragment).filter(query);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            refreshCurrentFragment();
            return true;
        } else if (id == R.id.action_settings) {
            openSettings();
            return true;
        } else if (id == R.id.action_about) {
            openAbout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshCurrentFragment() {
        int currentItem = ((androidx.viewpager2.widget.ViewPager2) findViewById(R.id.viewPager)).getCurrentItem();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + currentItem);
        if (fragment instanceof androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener) {
            ((androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener) fragment).onRefresh();
        }
    }

    private void openSettings() {
        android.content.Intent intent = new android.content.Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void openAbout() {
        android.content.Intent intent = new android.content.Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (clonedFragment != null) {
            clonedFragment.refresh();
        }
    }
}
