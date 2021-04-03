package com.luvtas.taseats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.luvtas.taseats.Common.Common;
import com.luvtas.taseats.Model.BraintreeToken;
import com.luvtas.taseats.Model.UserModel;
import com.luvtas.taseats.Remote.ICloudFunctions;
import com.luvtas.taseats.Remote.RetrofitCloudClient;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static int APP_REQUEST_CODE = 7171;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ICloudFunctions cloudFunction;

    private DatabaseReference userRef;
    private List<AuthUI.IdpConfig> providers;

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if(listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {

        Places.initialize(this, getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.FacebookBuilder().build());


        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCES);
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();

        listener = firebaseAuth -> {

//            Dexter.withActivity(this)
//                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
//                    .withListener(new PermissionListener() {
//                        @Override
//                        public void onPermissionGranted(PermissionGrantedResponse response) {
//                            FirebaseUser user = firebaseAuth.getCurrentUser();
//                            if(user != null)
//                            {
//                                // Already login
//                                checkUserFromFirebase(user);
//                            }
//                            else
//                            {
//                                phoneLogin();
//                            }
//                        }
//
//                        @Override
//                        public void onPermissionDenied(PermissionDeniedResponse response) {
//                            Toast.makeText(MainActivity.this,"You must enable this permission to use app", Toast.LENGTH_SHORT).show();
//                        }
//
//                        @Override
//                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
//
//                        }
//                    }).check();

            Dexter.withActivity(this)
                    .withPermissions(
                            Arrays.asList(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.CAMERA)
                    )
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if(report.areAllPermissionsGranted())
                            {
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                    if(user != null)
                                    {
                                        // Already login
                                        checkUserFromFirebase(user);
                                    }
                                    else
                                    {
                                        phoneLogin();
                                    }
                            }
                            else
                            {
                                Toast.makeText(MainActivity.this,"You must enable this permission to use app", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                        }
                    }).check();


        };
    }

    private void checkUserFromFirebase(FirebaseUser user) {
        dialog.show();
        userRef.child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            //Toast.makeText(MainActivity.this,"You already registed", Toast.LENGTH_SHORT).show();
//                            UserModel userModel = dataSnapshot.getValue(UserModel.class);
//                            goToHomeActivity(userModel);

                            // 09/09/2020 mark

//                            compositeDisposable.add(cloudFunction.getToken()
//                            .subscribeOn(Schedulers.io())
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe(braintreeToken -> {
//                                dialog.dismiss();
//                                UserModel userModel = dataSnapshot.getValue(UserModel.class);
//                                goToHomeActivity(userModel, braintreeToken.getToken());
//                            }, throwable -> {
//                                dialog.dismiss();
//                                Toast.makeText(MainActivity.this,""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
//                            }));


                            FirebaseAuth.getInstance().getCurrentUser()
                                    .getIdToken(true)
                                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                    .addOnCompleteListener(tokenResultTask -> {
                                        Log.d("Firebase", "OK");

                                        Common.authorizeKey = tokenResultTask.getResult().getToken();

//                                        Map<String, String> headers = new HashMap<>();
//                                        headers.put("Authorization", Common.buildToken(Common.authorizeKey));

//                                        compositeDisposable.add(cloudFunction.getToken(headers)
//                                        .subscribeOn(Schedulers.io())
//                                        .observeOn(AndroidSchedulers.mainThread())
//                                        .subscribe(braintreeToken -> {
                                            //Log.d("Check Login", "OK");
                                            dialog.dismiss();
                                            UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                            goToHomeActivity(userModel);


                                    });

                        }
                        else
                        {
                            showRegisterDialog(user);
                        }

                        dialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this,""+error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterDialog(FirebaseUser user) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Register");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        TextInputLayout phone_input_layout = (TextInputLayout)itemView.findViewById(R.id.phone_input_layout);
        EditText edt_name = (EditText) itemView.findViewById(R.id.edt_name);
        TextView txt_address_detail = (TextView) itemView.findViewById(R.id.txt_address_detail);
        EditText edt_phone = (EditText) itemView.findViewById(R.id.edt_phone);

        places_fragment = (AutocompleteSupportFragment)getSupportFragmentManager()
        .findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                placeSelected = place;
                txt_address_detail.setText(place.getAddress());
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MainActivity.this,""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Set
        if(user.getPhoneNumber() == null || TextUtils.isEmpty(user.getPhoneNumber()))
        {
            phone_input_layout.setHint("Email");
            edt_phone.setText(user.getEmail());
            edt_name.setText(user.getDisplayName());
        }
        else
            edt_phone.setText(user.getPhoneNumber());



        builder.setView(itemView);
        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton("REGISTER", (dialogInterface, i) -> {
            if(placeSelected != null)
            {
                if (TextUtils.isEmpty(edt_name.getText().toString())) {
                    Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }
//                else if (TextUtils.isEmpty(txt_address_detail.getText().toString())) {
//                    Toast.makeText(this, "Please enter your address", Toast.LENGTH_SHORT).show();
//                    return;
//                }

                UserModel userModel = new UserModel();
                userModel.setUid(user.getUid());
                userModel.setName(edt_name.getText().toString());
                userModel.setAddress(txt_address_detail.getText().toString());
                userModel.setPhone(edt_phone.getText().toString());
                userModel.setLat(placeSelected.getLatLng().latitude);
                userModel.setLng(placeSelected.getLatLng().longitude);

                userRef.child(user.getUid()).setValue(userModel)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    //                                dialogInterface.dismiss();
                                    //                                Toast.makeText(MainActivity.this,"Congratulation ! Register success !", Toast.LENGTH_SHORT).show();
                                    //                                goToHomeActivity(userModel);

                                    // 09/09/2020 mark

                                    //                                compositeDisposable.add(cloudFunction.getToken()
                                    //                                        .subscribeOn(Schedulers.io())
                                    //                                        .observeOn(AndroidSchedulers.mainThread())
                                    //                                        .subscribe(braintreeToken -> {
                                    //                                            dialogInterface.dismiss();
                                    //                                            Toast.makeText(MainActivity.this,"Congratulation ! Register success !", Toast.LENGTH_SHORT).show();
                                    //                                            goToHomeActivity(userModel,braintreeToken.getToken());
                                    //                                        }, throwable -> {
                                    //                                            dialog.dismiss();
                                    //                                            Toast.makeText(MainActivity.this,""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    //                                        }));


                                    FirebaseAuth.getInstance().getCurrentUser()
                                            .getIdToken(true)
                                            .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                            .addOnCompleteListener(tokenResultTask -> {
                                                Common.authorizeKey = tokenResultTask.getResult().getToken();


//                                                Map<String, String> headers = new HashMap<>();
//                                                headers.put("Authorization", Common.buildToken(Common.authorizeKey));


                                                            dialogInterface.dismiss();
                                                            Toast.makeText(MainActivity.this, "Congratulation ! Register success !", Toast.LENGTH_SHORT).show();
                                                            goToHomeActivity(userModel);

                                            });
                                }
                            }
                        });
            }
            else
            {
                Toast.makeText(this,"Please select address", Toast.LENGTH_SHORT).show();
                return;
            }

        });

        builder.setView(itemView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        dialog.setOnDismissListener(dialogInterface -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(places_fragment);
            fragmentTransaction.commit();
        });

        dialog.show();
    }

    private void goToHomeActivity(UserModel userModel) {

        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        Common.currentUser = userModel;
//                        Common.currentToken = token;
                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        finish();
                    }
                }).addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                Common.currentUser = userModel;
//                Common.currentToken = token;
                Common.updateToken(MainActivity.this, task.getResult().getToken());
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                finish();
            }
        });

//        Common.currentUser = userModel;
//        Common.currentToken = token;
//        startActivity(new Intent(this, HomeActivity.class));
//        finish();
    }

    private void phoneLogin() {

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setTheme(R.style.FullscreenTheme)
                .setAvailableProviders(providers)
                .build(), APP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == APP_REQUEST_CODE)
        {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if(resultCode == RESULT_OK)
            {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else
            {
                Toast.makeText(this,""+response.getError().getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }

}
