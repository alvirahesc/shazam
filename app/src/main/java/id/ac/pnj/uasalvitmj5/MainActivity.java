package id.ac.pnj.uasalvitmj5;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int FASTEST_INTERVAL = 5;
    public static final int DEFAULT_INTERVAL = 30;
    private static final int PERMISSIONS_FINE_LOCATION = 25; //tidak masalah angka berapa untuk permissionnya
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address; //ADDRESS MERUPAKAN FITUR TAMBAHAN, NANTINYA DIGUNAKAN API GOOGLE YANG TERSEDIA
    Button btn_showMap, btn_aboutMe;
    Switch sw_locationupdates, sw_gps;

    //Variabel yang memberitahukan apakah lokasi sedang direkam atau tidak
    boolean updateOn = false;

    //lokasi saat ini
    Location currentLocation;
    //daftar lokasi tersimpan
    List<Location> markedLocations;

    // File config yang berisi konfigurasi FusedLocationProviderClient
    LocationRequest locationRequest;

    LocationCallback locationCallBack;

    //Google API untuk lokasi, kebanyakan fitur lokasi di app ini bergantung pada class ini (dependen)
    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);

        btn_showMap = findViewById(R.id.btn_showMap);
        btn_aboutMe = findViewById(R.id.btn_aboutMe);

        sw_gps = findViewById(R.id.sw_gps);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);



        //konfigurasi locationRequest

        locationRequest = new LocationRequest();

        locationRequest.setInterval(300 * DEFAULT_INTERVAL);

        locationRequest.setFastestInterval(1000 * FASTEST_INTERVAL);

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //event yang berjalan setelah interval yang sudah dibuat
        locationCallBack = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //menyimpan hasil data lokasi
                updateUIValues(locationResult.getLastLocation());
            }
        };


        btn_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(i);
            }
        });

        btn_aboutMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this,AboutMe.class);
                startActivity(i);
            }
        });

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_gps.isChecked()) {
                    //menggunakan GPS untuk hasil akurat
                    locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Menggunakan sensor GPS");
                } else {
                    locationRequest.setPriority(locationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Menggunakan tower atau WIFI");
                }
            }
        });

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_locationupdates.isChecked()) {
                    //menyalakan fungsi update lokasi di hp
                    startLocationUpdates();
                } else {
                    //mematikan fungsi update lokasi di hp
                    stopLocationUpdates();
                }
            }
        });

        updateGPS();
    } //akhir method onCreate


    private void startLocationUpdates() {
        tv_updates.setText("Lokasi Anda sedang direkam");
        //ini adalah blok kode yang dihasilkan oleh permission check yang diminta rLU
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null); //DOK: pada line ini terjadi error rLU meminta blok kode di atas untuk mengecek permission
        updateGPS();
    }

    private void stopLocationUpdates() {
        tv_updates.setText("N/A");
        tv_lat.setText("N/A");
        tv_lon.setText("N/A");
        tv_address.setText("N/A");
        tv_speed.setText("N/A");
        tv_accuracy.setText("N/A");
        tv_altitude.setText("N/A");
        tv_sensor.setText("N/A");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                } else {
                    Toast.makeText(this, "App ini perlu izin dari Anda agar bekerja dengan baik!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void updateGPS() {
        //fungsi utamanya untuk update GPS (termasuk UI/text view), juga berfungsi meminta restu dari empunya hp/user untuk mengakses GPS

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //user menentukan restunya
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //direstui oleh user
                    updateUIValues(location);
                    currentLocation = location;
                }
            });
        } else {
            //kalau masih belum direstui
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    //fungsi ini dipisah seandainya mau dipakai lagi di tempat lain, sebenarnya bisa diletakkan di updateUIValues di dalam if di atas.
    private void updateUIValues(Location location) {
        //update semua data textview dengan data nilai lokasi terbaru
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        //beberapa hp memiliki keterbatasan dalam mendapatkan data GPS, menggunakan blok berikut hal tersebut dicek
        if (location.hasAltitude()) {
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        } else {
            tv_altitude.setText("Tidak Tersedia");
        }

        if (location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getSpeed()));
        } else {
            tv_speed.setText("Tidak Tersedia");
        }
        //FITUR TAMBAHAN MENGGUNAKAN GEOCODER UNTUK TRANSLASI KOORDINAT KE ALAMAT
        Geocoder geocoder = new Geocoder(MainActivity.this);
        //seandainya tidak bekerja, digunakan try-catch
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
            tv_address.setText(addresses.get(0).getAddressLine(0)); //Address line berarti jalan, sebenarnya bisa yang lain seperti kode pos atau apa saja yang disediakan Geocoder
        }
        catch (Exception e) {
            tv_address.setText("Gagal menemukan alamat");
        }


        }
}