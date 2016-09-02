package com.cilenco.featurediscovery;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.cilenco.discoveryview.DiscoveryView;

public class MainActivity extends AppCompatActivity implements DiscoveryView.OnDiscoveryViewClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final View v = findViewById(R.id.info);
                DiscoveryView infoView = new DiscoveryView.Builder(MainActivity.this, v)
                        .setPrimaryText(R.string.infoHeader)
                        .setSecondaryText(R.string.infoDescription)
                        .setOnClickListener(MainActivity.this)
                        .usePrimaryColorAsFilter(true)
                        .build();

                infoView.show();
            }
        });

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:

                DiscoveryView infoView = new DiscoveryView.Builder(this, findViewById(R.id.info))
                        .setPrimaryText(R.string.infoHeader)
                        .setSecondaryText(R.string.infoDescription)
                        .setOnClickListener(this)
                        .usePrimaryColorAsFilter(true)
                        .build();

                infoView.show();
                return true;

            case R.id.star:

                DiscoveryView starView = new DiscoveryView.Builder(this, findViewById(R.id.star))
                        .setPrimaryText(R.string.starHeader)
                        .setSecondaryText(R.string.starDescription)
                        .setOnClickListener(this)
                        .usePrimaryColorAsFilter(true)
                        .setPrimaryTextTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))
                        .setSecondaryTextTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC))
                        .build();

                starView.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onFabClicked(View v) {
        DiscoveryView view = new DiscoveryView.Builder(this, v)
                .setPrimaryText(R.string.fabHeader)
                .setSecondaryText(R.string.fabDescription)
                .setOnClickListener(this)
                .build();

        view.show();
    }

    @Override
    public void onDiscoveryViewClicked(DiscoveryView discoveryView) {
        discoveryView.dismiss();
    }

    @Override
    public void onDiscoveryViewDismissed(DiscoveryView discoveryView) {

    }
}
