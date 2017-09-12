package com.softmarshmallow.foodle.Views.StoreDetail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Geocoder;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.ms.square.android.expandabletextview.ExpandableTextView;
import com.r0adkll.slidr.Slidr;
import com.softmarshmallow.foodle.Models.Menus.MenuModel;
import com.softmarshmallow.foodle.Models.ReviewModel;
import com.softmarshmallow.foodle.Models.Store.StoreModel;
import com.softmarshmallow.foodle.Models.Store.StoreReviewModel;
import com.softmarshmallow.foodle.R;
import com.softmarshmallow.foodle.Services.MenuService;
import com.softmarshmallow.foodle.Services.StoreReviewService;
import com.softmarshmallow.foodle.Services.StoreService;
import com.softmarshmallow.foodle.Views.RequestCatering.RequestCateringActivity;
import com.softmarshmallow.foodle.Views.Shared.GridSpacingItemDecoration;
import com.softmarshmallow.foodle.Views.StoreDetail.StoreReview.StoreReviewCreaterActivity;
import com.yalantis.taurus.PullToRefreshView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.functions.Consumer;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import android.widget.Toast;

public class StoreDetailViewActivity extends AppCompatActivity implements OnMapReadyCallback, AppBarLayout.OnOffsetChangedListener
{
        
        
        private static final String TAG = "StoreDetailViewActivity";
        //region views
        @BindView(R.id.appbar)
        AppBarLayout appBarLayout;
        
        @BindView(R.id.pull_to_refresh)
        PullToRefreshView pullToRefreshView;
        
        @BindView(R.id.toolbar)
        Toolbar toolbar;
        
        @BindView(R.id.storeFeatureGraphicImageView)
        ImageView storeFeatureGraphicImageView;
        
        @BindView(R.id.storeNameTextView)
        TextView storeNameTextView;
        
        @BindView(R.id.storeShortDescriptionTextView)
        TextView storeShortDescriptionTextView;
        
        @BindView(R.id.storeFullDescriptionExpandableTextView)
        ExpandableTextView storeFullDescriptionExpandableTextView;
        
        @BindView(R.id.storeDetailMenusRecyclerView)
        RecyclerView menusRecyclerView;
        
        @BindView(R.id.storeReviewsRecyclerView)
        RecyclerView reviewsRecyclerView;
        
        
        // Store Location
        @BindView(R.id.storeAddressTextView)
        TextView storeAddressTextView;
        
        @BindView(R.id.storeMapDescriptionAddressTextView)
        TextView storeMapDescriptionAddressTextView;
        
        @BindView(R.id.storeLocationMapView)
        MapView storeLocationMapView;
        
        //endregion
        
        
        Geocoder geoCoder;
        
        MenusAdapter menusAdapter;
        ReviewsAdapter reviewsAdapter;
        
        static StoreModel storeDataToDisplay;
        
        public static void ShowStoreDetailWithData(Context context, StoreModel storeDataToDisplay) {
                StoreDetailViewActivity.storeDataToDisplay = storeDataToDisplay;
                context.startActivity(new Intent(context, StoreDetailViewActivity.class));
        }
        
        
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_store_detail_view);
                ButterKnife.bind(this);

                //pushable activity
                Slidr.attach(this);


                toolbar.setTitleTextColor(Color.RED);

                //region Init
                // map
                storeLocationMapView.onCreate(savedInstanceState);
                SetUpMap();
                
                setSupportActionBar(toolbar);
                
                // Init CollapsingToolbar
                InitCollapsingToolbar();
                // InitPullToRefresh
                InitPullToRefresh();
                //
                InitLikeButton();
                
                
                // init GeoCoder
                this.geoCoder = new Geocoder(this, Locale.getDefault());
                //endregion
                
        
                
                LoadStoreData();
                
                
        }
        
        
        public void LoadStoreData() {
                
                Thread t = new Thread(new Runnable()
                {
                        @Override
                        public void run() {
                                LoadStoreDataInBackground();
                        }
                });
                t.run();
        }
        
        public void LoadStoreDataInBackground() {
                
                Gson gson = new Gson();
                // Load StoreData
                if (storeDataToDisplay == null){
                        Log.e(TAG, "STORE DATA CANNOT BE NULL, this may be caused by using old intend pattern");
                        this.storeDataToDisplay = gson.fromJson(getIntent().getStringExtra(
                                (StoreModel.class.getName())), StoreModel.class);
                }
                
                
                
                runOnUiThread(new Runnable()
                {
                        @Override
                        public void run() {
                                
                                SetMenus();
                                
                                SetStoreReviews();
                                
                                
                                // Store MainImage
                                Glide.with(StoreDetailViewActivity.this)
                                        .load(storeDataToDisplay.GetMainStorePhotoUrl())
                                        .into(storeFeatureGraphicImageView);
                                // StoreName
                                storeNameTextView.setText(storeDataToDisplay.StoreName);
                                // StoreShortDescription
                                storeShortDescriptionTextView.setText(
                                        storeDataToDisplay.StoreShortDescription);
                                // StoreFullDescription
                                storeFullDescriptionExpandableTextView.setText(
                                        storeDataToDisplay.StoreFullDescription);
                                
                                
                                // region Store Location ==
                                storeAddressTextView.setText(storeDataToDisplay.StoreAddress);
                                
                                // Store address with LatLng Data
                               /* String addressByGeoCoder = "";
                                try {
                                        List<Address> listAddresses = geoCoder.getFromLocation(
                                                storeDataToDisplay.GetStoreLocation().first,
                                                storeDataToDisplay.GetStoreLocation().second, 1);
                                        Log.d(TAG, "listAddresses.size : " + listAddresses.size());
                                        Address address = listAddresses.get(0);
                                        
                                        for (int addressLine = 0; addressLine < 3; addressLine++) {
                                                addressByGeoCoder += address.getAddressLine(
                                                        addressLine);
                                        }
                                }
                                catch (Exception ex) {
                                        addressByGeoCoder = "No Data..";
                                }
                                storeMapDescriptionAddressTextView.setText(addressByGeoCoder);
*/

                                // set map with storeLocation
                                // set google map to storeLocation
                                storeLocationMapView.getMapAsync(new OnMapReadyCallback()
                                {
                                        @Override
                                        public void onMapReady(GoogleMap googleMap) {
                                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                                        new LatLng(
                                                                storeDataToDisplay.GetStoreLocation().first,
                                                                storeDataToDisplay.GetStoreLocation().second),
                                                        18.5f));
                                        }
                                });
                                //endregion
                                
                        }
                });
                
        }
        
        //region Menus
        void SetMenus() {
                
                // Menus
                RecyclerView.LayoutManager menusLayoutManager = new GridLayoutManager(this, 2);
                
                menusAdapter = new MenusAdapter(this);
                menusRecyclerView.setLayoutManager(menusLayoutManager);
                menusRecyclerView.addItemDecoration(
                        new GridSpacingItemDecoration(2,
                                GridSpacingItemDecoration.ConvertPixelsToDp(10), true));
                menusRecyclerView.setItemAnimator(new DefaultItemAnimator());
                menusRecyclerView.setAdapter(menusAdapter);


                storeDataToDisplay.Menus = new ArrayList<>();
                for (String keyToMenuId : storeDataToDisplay.MenusIds.keySet()){
                        String menuId = storeDataToDisplay.MenusIds.get(keyToMenuId);
                        MenuService.GetMenu(
                                menuId,
                                new Consumer<MenuModel>()
                                {
                                        @Override
                                        public void accept(MenuModel menuModel) throws Exception {
                                                storeDataToDisplay.Menus.add(menuModel);
                                                menusAdapter.UpdateDatas(storeDataToDisplay.Menus);
                                        }
                                }, new Consumer<String>()
                                {
                                        @Override
                                        public void accept(String databaseError) throws Exception {
                                                Log.e(TAG, databaseError);
                                        }
                                });
                }


                
        }
        
        
        //endregion
        
        
        //region Reviews

        
        void SetStoreReviews() {
                reviewsAdapter = new ReviewsAdapter(this);

                // Reviews
                RecyclerView.LayoutManager reviewsLayoutManager = new LinearLayoutManager(this);

                reviewsRecyclerView.setLayoutManager(reviewsLayoutManager);
                reviewsRecyclerView.addItemDecoration(
                        new GridSpacingItemDecoration(2,
                                GridSpacingItemDecoration.ConvertPixelsToDp(10), true));
                reviewsRecyclerView.setItemAnimator(new DefaultItemAnimator());
                reviewsRecyclerView.setAdapter(reviewsAdapter);


                // Load & Update Review Datas
                storeDataToDisplay.StoreReviews = new ArrayList<>();
                for (String storeReviewsIdsHashKey : storeDataToDisplay.StoreReviewsIds.keySet()) {
                        String storeReviewId = storeDataToDisplay.StoreReviewsIds.get(storeReviewsIdsHashKey);
                        StoreReviewService.GetStoreReview(
                                storeReviewId,
                                new Consumer<StoreReviewModel>()
                                {
                                        @Override
                                        public void accept(StoreReviewModel storeReviewModel) throws Exception {
                                                storeDataToDisplay.StoreReviews.add(storeReviewModel);
                                                List<? extends ReviewModel> reviews = storeDataToDisplay.StoreReviews;
                                                reviewsAdapter.UpdateReviews((List<ReviewModel>) reviews);
                                        }
                                }, new Consumer<String>()
                                {
                                        @Override
                                        public void accept(String databaseError) throws Exception {
                                                Log.e(TAG, databaseError);
                                        }
                                });
                }



        }
        //endregion
        
        void InitPullToRefresh(){
                pullToRefreshView.setOnRefreshListener(new PullToRefreshView.OnRefreshListener()
                {
                        @Override
                        public void onRefresh() {
                                Log.d(TAG, "onRefresh");


                                StoreService.GetStore(
                                        storeDataToDisplay.Id,
                                        new Consumer<StoreModel>()
                                        {
                                                @Override
                                                public void accept(StoreModel storeModel) throws Exception {
                                                        pullToRefreshView.setRefreshing(false);
                                                        StoreDetailViewActivity.ShowStoreDetailWithData(StoreDetailViewActivity.this, storeModel);
                                                        finish();
                                                }
                                        }, new Consumer<String>()
                                        {
                                                @Override
                                                public void accept(String databaseError) throws Exception {
                                                        pullToRefreshView.setRefreshing(false);
                                                        new SweetAlertDialog(StoreDetailViewActivity.this, SweetAlertDialog.ERROR_TYPE)
                                                                .setTitleText("업데이트 오류")
                                                                .setContentText(databaseError)
                                                                .show();
                                                }
                                        });



                        }
                });
        }
        
        

        
        
        //region map
        GoogleMap googleMap;
        
        void SetUpMap() {
                if (googleMap == null) {
                        storeLocationMapView.getMapAsync(this);
                }
        }
        
        
        @Override
        public void onMapReady(GoogleMap googleMap) {
                this.googleMap = googleMap;
//                this.googleMap.UiSettings.CompassEnabled = false;
//                this.googleMap.UiSettings.MyLocationButtonEnabled = false;
//                this.googleMap.UiSettings.MapToolbarEnabled = false;
        }
        //endregion
        
        
        void InitCollapsingToolbar() {
                final CollapsingToolbarLayout collapsingToolbar =
                        (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
                collapsingToolbar.setTitle(" ");
                AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
                appBarLayout.setExpanded(true);
                
                // hiding & showing the title when toolbar expanded & collapsed
                
                
                appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener()
                {
                        boolean isShow = false;
                        int scrollRange = -1;
                        
                        @Override
                        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                                if (scrollRange == -1) {
                                        scrollRange = appBarLayout.getTotalScrollRange();
                                }
                                if (scrollRange + verticalOffset == 0) {
                                        collapsingToolbar.setTitle(storeDataToDisplay.StoreName);
                                        this.isShow = true;
                                } else if (isShow) {
                                        collapsingToolbar.setTitle("");
                                        isShow = false;
                                }
                        }
                });
                
        }
        
        
        void InitLikeButton(){
                LikeButton likeButton = findViewById(R.id.likeButton);
                likeButton.setOnLikeListener(new OnLikeListener() {
                        @Override
                        public void liked(LikeButton likeButton) {
                                new SweetAlertDialog(StoreDetailViewActivity.this)
                                        .setTitleText("Liked!")
                                        .show();

                        }
                
                        @Override
                        public void unLiked(LikeButton likeButton) {
                                new SweetAlertDialog(StoreDetailViewActivity.this)
                                        .setTitleText("UnLiked!")
                                        .show();

                        }
                });
        }
        
        
        @OnClick(R.id.cateringRequestOptionContainer)
        void OnRequestCateringOptionClick() {
                Intent intent = new Intent(this, RequestCateringActivity.class);
                startActivity(intent);
        }
        
        @OnClick(R.id.shareOptionContainer)
        void OnShareOptionClick() {
                Toast.makeText(this, "Share!", Toast.LENGTH_SHORT)
                        .show();
        }
        
        @OnClick(R.id.writeReviewOptionContainer)
        void OnWriteReviewOptionClick() {
                StoreReviewCreaterActivity.StartCreateStoreReviewActivity(storeDataToDisplay.Id, this);
        }
        
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                //The Refresh must be only active when the offset is zero :
                pullToRefreshView.setEnabled(verticalOffset == 0);
        
        }
        
        @Override
        protected void onResume() {
                super.onResume();
                appBarLayout.addOnOffsetChangedListener(this);
        }
        
        @Override
        protected void onPause() {
                super.onPause();
                appBarLayout.removeOnOffsetChangedListener(this);
        }
        
        //endregion

        @Override
        protected void attachBaseContext(Context newBase) {
                super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
        }
        
        
}
