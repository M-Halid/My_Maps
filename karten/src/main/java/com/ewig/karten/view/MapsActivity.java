package com.ewig.karten.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ewig.karten.R;
import com.ewig.karten.model.Place;
import com.ewig.karten.roomdb.PlaceDao;
import com.ewig.karten.roomdb.PlaceDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private com.ewig.karten.databinding.ActivityMapsBinding binding;
    LocationManager locationManager;
    LocationListener locationListener;
    ActivityResultLauncher permissionLauncher;
    PlaceDatabase db;
    PlaceDao placeDao;
    double selectedLatitude;
    double selectedLongitude;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    Place selectedPlace;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = com.ewig.karten.databinding.ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        registerLauncher();

        db= Room.databaseBuilder(getApplicationContext(), PlaceDatabase.class, "Places")
                //.allowMainThreadQueries()
                .build();
        placeDao=db.placeDao();
        selectedLatitude = 0.0;
        selectedLongitude = 0.0;

        binding.save.setEnabled(false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        Intent intent = getIntent();
        String intentInfo=intent.getStringExtra("info");
        if (intentInfo.equals("new")) {
            binding.save.setVisibility(View.VISIBLE);
            binding.delete.setVisibility(View.GONE);

            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    LatLng newLoc = new LatLng(location.getLatitude(),location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLoc,16));
                    //mMap.addMarker(new MarkerOptions().position(newLoc).title("Current Location"));
                }

            };
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Snackbar.make(binding.getRoot(), "Permission needed for maps", Snackbar.LENGTH_INDEFINITE).setAction(
                            "Give Permission", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //requestPermission
                                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                                }
                            }).show();
                } else {
                    //requestPermission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, locationListener);
                Location lastLocation= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation!=null){
                    LatLng lastUserLoc= new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLoc,16));
                }
                mMap.setMyLocationEnabled(true);

            }


        }else {
            mMap.clear();
            selectedPlace= (Place) intent.getSerializableExtra("place");
            LatLng latLng= new LatLng(selectedPlace.latitude,selectedPlace.longitude);
            mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            binding.placeNameText.setText(selectedPlace.name);
            binding.save.setVisibility(View.GONE);
            binding.delete.setVisibility(View.VISIBLE);

        }


        // Add a marker in Berlin and move the camera
     /*   LatLng berlin = new LatLng(52.509352, 13.375739);
        mMap.addMarker(new MarkerOptions().position(berlin).title("Potsdamer Platz"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(berlin, 16));*/
    }

    private void registerLauncher() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    //permissionGranted
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat
                            .checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, locationListener);
                        Location lastLocation= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastLocation!=null){
                            LatLng lastUserLoc= new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLoc,16));
                        }
                    }


                }else {
                    //permissionDenied
                    Toast.makeText(MapsActivity.this,"Permission for maps needed",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().title("Marked Place").position(latLng));
        selectedLatitude=latLng.latitude;
        selectedLongitude=latLng.longitude;
        binding.save.setEnabled(true);
    }
    public void save(View view){
       Place place = new Place(binding.placeNameText.getText().toString(),selectedLatitude,selectedLongitude);
        //placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();
        compositeDisposable.add((placeDao.insert(place))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse)
        );
    }
    private void handleResponse(){
        Intent intent =new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    public void delete(View view){
        if (selectedPlace!=null){
        compositeDisposable.add(placeDao.delete(selectedPlace)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse));
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        compositeDisposable.clear();
    }
}