package com.luvtas.taseats.ui.cart;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.luvtas.taseats.Adapter.MyCartAdapter;
import com.luvtas.taseats.Callback.ILoadTimeFromFirebaseListener;
import com.luvtas.taseats.Callback.ISearchCategoryCallbackListener;
import com.luvtas.taseats.Common.Common;
import com.luvtas.taseats.Common.MySwipeHelper;
import com.luvtas.taseats.Database.CartDataSource;
import com.luvtas.taseats.Database.CartDatabase;
import com.luvtas.taseats.Database.CartItem;
import com.luvtas.taseats.Database.LocalCartDataSource;
import com.luvtas.taseats.EventBus.CounterCartEvent;
import com.luvtas.taseats.EventBus.HideFABCart;
import com.luvtas.taseats.EventBus.MenuItemBack;
import com.luvtas.taseats.EventBus.UpdateItemInCart;
import com.luvtas.taseats.Model.AddonModel;
import com.luvtas.taseats.Model.BraintreeTransaction;
import com.luvtas.taseats.Model.CategoryModel;
import com.luvtas.taseats.Model.FCMSendData;
import com.luvtas.taseats.Model.FoodModel;
import com.luvtas.taseats.Model.Order;
import com.luvtas.taseats.Model.SizeModel;
import com.luvtas.taseats.R;
import com.luvtas.taseats.Remote.ICloudFunctions;
import com.luvtas.taseats.Remote.IFCMService;
import com.luvtas.taseats.Remote.RetrofitCloudClient;
import com.luvtas.taseats.Remote.RetrofitFCMClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CartFragment extends Fragment implements ILoadTimeFromFirebaseListener, ISearchCategoryCallbackListener, TextWatcher {

    private static final int REQUEST_BRAINTREE_CODE = 7777;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ISearchCategoryCallbackListener searchFoodCallbackListener;

    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;

    private CartViewModel cartViewModel;
    private Unbinder unbinder;
    private MyCartAdapter adapter;

    private BottomSheetDialog addonBottomSheetDialog;
    private ChipGroup chip_group_addon, chip_group_user_selected_addon;
    private EditText edt_search;

    String address, comment;

    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;

    ICloudFunctions cloudFunctions;
    IFCMService ifcmService;
    ILoadTimeFromFirebaseListener listener;

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;

    @OnClick(R.id.btn_place_order)
    void onPlaceOrderClick(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("One more step");

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_order, null);



        EditText edt_comment = (EditText)view.findViewById(R.id.edt_comment);
        TextView txt_address = (TextView)view.findViewById(R.id.txt_address_detail);
        RadioButton rdi_home = (RadioButton)view.findViewById(R.id.rdi_home_address);
        RadioButton rdi_other_address = (RadioButton)view.findViewById(R.id.rdi_other_address);
        RadioButton rdi_ship_to_this = (RadioButton)view.findViewById(R.id.rdi_ship_this_address);
        RadioButton rdi_cod = (RadioButton)view.findViewById(R.id.rdi_cod);
        RadioButton rdi_braintree = (RadioButton)view.findViewById(R.id.rdi_braintree);


        places_fragment = (AutocompleteSupportFragment)getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                placeSelected = place;
                txt_address.setText(place.getAddress());
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(getContext(),""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Date
        txt_address.setText(Common.currentUser.getAddress()); // By default we select home address, so user's address will display

        // Event
        rdi_home.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b)
            {
                txt_address.setText(Common.currentUser.getAddress());
                txt_address.setVisibility(View.VISIBLE);
                places_fragment.setHint(Common.currentUser.getAddress());
            }
        });

        rdi_other_address.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b)
            {
                txt_address.setVisibility(View.VISIBLE);
            }
        });

        rdi_ship_to_this.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b)
            {
                //Toast.makeText(getContext(),"Implement late with Google API", Toast.LENGTH_SHORT).show();
                fusedLocationProviderClient.getLastLocation()
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                txt_address.setVisibility(View.GONE);
                            }
                        })
                        .addOnCompleteListener(new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                String coordinates = new StringBuilder()
                                        .append(task.getResult().getLatitude())
                                        .append("/")
                                        .append(task.getResult().getLongitude()).toString();

                                Single<String> singleAddress = Single.just(getAddressFromLatLng(task.getResult().getLatitude(),task.getResult().getLongitude()));

                                Disposable disposable = singleAddress.subscribeWith(new DisposableSingleObserver<String>() {
                                    @Override
                                    public void onSuccess(String s) {
                                        txt_address.setText(s);
                                        txt_address.setVisibility(View.VISIBLE);
                                        places_fragment.setHint(s);
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        txt_address.setText(e.getMessage());
                                        txt_address.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        });
            }
        });

        builder.setView(view);
        builder.setNegativeButton("NO", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        }).setPositiveButton("YES", (dialogInterface, i) -> {
            //Toast.makeText(getContext(),"Implement late", Toast.LENGTH_LONG).show();
            if(rdi_cod.isChecked())
                paymentCOD(txt_address.getText().toString(), edt_comment.getText().toString());
            else if(rdi_braintree.isChecked())
            {
                address = txt_address.getText().toString();
                comment = edt_comment.getText().toString();
                if(!TextUtils.isEmpty(Common.currentToken))
                {
                    DropInRequest dropInRequest = new DropInRequest().clientToken(Common.currentToken);
                    startActivityForResult(dropInRequest.getIntent(getContext()),REQUEST_BRAINTREE_CODE);
                }
            }
        });
        AlertDialog dialog = builder.create();
        // Avoid second open fragment crash
        dialog.setOnDismissListener(dialogInterface -> {
            FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(places_fragment);
            fragmentTransaction.commit();
        });

        dialog.show();
    }

    private void paymentCOD(String address, String comment) {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid(),
                Common.currentRestaurant.getUid())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(cartItems -> {
            // When we have all cartItems, we will get total price
            cartDataSource.sumPriceInCart(Common.currentUser.getUid(),
                    Common.currentRestaurant.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Double>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Double totalPrice) {
                            double finalPrice = totalPrice; //We will modify this formula for discount late
                            Order order = new Order();
                            order.setUserId(Common.currentUser.getUid());
                            order.setUserName(Common.currentUser.getName());
                            order.setUserPhone(Common.currentUser.getPhone());
                            order.setShippingAddress(address);
                            order.setComment(comment);

                            if(currentLocation != null)
                            {
                                order.setLat(currentLocation.getLatitude());
                                order.setLng(currentLocation.getLongitude());
                            }
                            else
                            {
                                order.setLat(-0.1f);
                                order.setLng(-0.1f);
                            }

                            order.setCartItemList(cartItems);
                            order.setTotalPayment(totalPrice);
                            order.setDiscount(0); // Modify with discount late
                            order.setFinalPayment(finalPrice);
                            order.setCod(true);
                            order.setTransactionId("Cash On Delivery");

                            // Submit this order object to Firebase
                            //writeOrderToFirebase(order);
                            syncLocalTimeWithGlobalTime(order);
                        }

                        @Override
                        public void onError(Throwable e) {
                            if(!e.getMessage().contains("Query returned empty result set"))
                                Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }, throwable -> {
            Toast.makeText(getContext(),""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
        }));
    }

    private void syncLocalTimeWithGlobalTime(Order order) {
        final DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long offset = dataSnapshot.getValue(Long.class);
                long estimatedServerTimeMS = System.currentTimeMillis()+offset; // offset is missing between your local time and server time
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                Date resultDate = new Date(estimatedServerTimeMS);
                Log.d("TEST_DATE", ""+sdf.format(resultDate));

                listener.onLoadTimeSuccess(order,estimatedServerTimeMS);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onLoadTimeFailed(databaseError.getMessage());
                //Toast.makeText(getContext(),""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void writeOrderToFirebase(Order order) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentRestaurant.getUid())
                .child(Common.ORDER_REF)
                .child(Common.createOrderNumber()) // Create order number with only digit
                .setValue(order)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                cartDataSource.cleanCart(Common.currentUser.getUid(),
                        Common.currentRestaurant.getUid())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<Integer>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onSuccess(Integer integer) {

                                Map<String, String> notiData = new HashMap<>();
                                notiData.put(Common.NOTI_TITLE, "New Order");
                                notiData.put(Common.NOTI_CONTENT,"You have new order from "+Common.currentUser.getPhone());

                                FCMSendData sendData = new FCMSendData(Common.createTopicOrder(), notiData);
                                compositeDisposable.add(ifcmService.sendNotification(sendData)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(fcmResponse -> {
                                            Toast.makeText(getContext(),"Order Success", Toast.LENGTH_SHORT).show();
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                        }, throwable -> {
                                                Toast.makeText(getContext(),"Order was sent but failure to send notification", Toast.LENGTH_SHORT).show();
                                                EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                        }));


                            }

                            @Override
                            public void onError(Throwable e) {
                                Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        });
    }

    private String getAddressFromLatLng(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        String result = "";
        try{
            List<Address> addressList = geocoder.getFromLocation(latitude,longitude,1);
            if(addressList != null && addressList.size() > 0)
            {
                Address address = addressList.get(0); // always get first item
                StringBuilder sb = new StringBuilder(address.getAddressLine(0));
                result = sb.toString();
            }
            else
                result = "Address not found";
        } catch (IOException e){
            e.printStackTrace();
            result = e.getMessage();
        }
        return result;
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        cartViewModel = ViewModelProviders.of(this).get(CartViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        cloudFunctions = RetrofitCloudClient.getInstance(Common.currentRestaurant.getPaymentUrl()).create(ICloudFunctions.class);
        listener = this;

        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItem().observe(getViewLifecycleOwner(), new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                if(cartItems == null || cartItems.isEmpty())
                {
                    recycler_cart.setVisibility(View.GONE);
                    group_place_holder.setVisibility(View.GONE);
                    txt_empty_cart.setVisibility(View.VISIBLE);
                }
                else
                {
                    recycler_cart.setVisibility(View.VISIBLE);
                    group_place_holder.setVisibility(View.VISIBLE);
                    txt_empty_cart.setVisibility(View.GONE);

                    adapter = new MyCartAdapter(getContext(),cartItems);
                    recycler_cart.setAdapter(adapter);
                }
            }
        });
        unbinder = ButterKnife.bind(this,root);
        initViews();
        initLocation();
        return root;
    }

    private void initLocation() {
        buildLocationRequest();
        buildLocationCallBack();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    private void initViews() {

        searchFoodCallbackListener = this;
        
        initPlaceClient();

        setHasOptionsMenu(true);

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        EventBus.getDefault().postSticky(new HideFABCart(true));

        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_cart,200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0 , Color.parseColor("#FF3C30"),
                        pos -> {
                                //Toast.makeText(getContext(),"Delete item Click", Toast.LENGTH_SHORT).show();

                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            cartDataSource.deleteCartItem(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(Integer integer) {
                                            adapter.notifyItemRemoved(pos);
                                            sumAllItemInCart(); // update total price
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true)); // Update FAB
                                            Toast.makeText(getContext(),"Delete item from cart successful", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }));

                buf.add(new MyButton(getContext(), "Update", 30, 0 , Color.parseColor("#5D4037"),
                        pos -> {
                                CartItem cartItem = adapter.getItemAtPosition(pos);
                                FirebaseDatabase.getInstance()
                                        .getReference(Common.RESTAURANT_REF)
                                        .child(Common.currentRestaurant.getUid())
                                        .child(Common.CATEGORY_REF)
                                        .child(cartItem.getCategoryId())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.exists())
                                                {
                                                    CategoryModel categoryModel = dataSnapshot.getValue(CategoryModel.class);
                                                    searchFoodCallbackListener.onSearchCategoryFound(categoryModel, cartItem);
                                                }
                                                else
                                                {
                                                    searchFoodCallbackListener.onSearchCategoryNotFound("Food not found");
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                searchFoodCallbackListener.onSearchCategoryNotFound(databaseError.getMessage());
                                            }
                                        });
                        }));
            }
        };

        sumAllItemInCart();

        // Addon
        addonBottomSheetDialog = new BottomSheetDialog(getContext(), R.style.DialogStyle);
        View layout_addon_display = getLayoutInflater().inflate(R.layout.layout_addon_display, null);
        chip_group_addon = (ChipGroup)layout_addon_display.findViewById(R.id.chip_group_addon);
        edt_search = (EditText)layout_addon_display.findViewById(R.id.edt_search);
        addonBottomSheetDialog.setContentView(layout_addon_display);

        addonBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                displayUserSelectedAddon(chip_group_user_selected_addon);
                calculateTotalPrice();
            }
        });
    }

    private void displayUserSelectedAddon(ChipGroup chip_group_user_selected_addon) {
        if(Common.selectedFood.getUserSelectedAddon() != null && Common.selectedFood.getUserSelectedAddon().size() > 0)
        {
            chip_group_user_selected_addon.removeAllViews();
            for(AddonModel addonModel:Common.selectedFood.getUserSelectedAddon())
            {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_chip_with_delete_icon, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+$")
                        .append(addonModel.getPrice()).append(")"));
                chip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(b)
                        {
                            if(Common.selectedFood.getUserSelectedAddon() == null)
                                Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                            Common.selectedFood.getUserSelectedAddon().add(addonModel);
                        }
                    }
                });
                chip_group_user_selected_addon.addView(chip);
            }
        }
        else
            chip_group_user_selected_addon.removeAllViews();
    }

    private void initPlaceClient() {
        Places.initialize(getContext(),getString(R.string.google_maps_key));
        placesClient = Places.createClient(getContext());
    }

    private void sumAllItemInCart() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid(),
                Common.currentRestaurant.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double aDouble) {
                        txt_total_price.setText(new StringBuilder("Total: $ ").append(Common.formatPrice(aDouble)));
                    }

                    @Override
                    public void onError(Throwable e) {
                        if(!e.getMessage().contains("Query returned empty"))
                            Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false); // Hide Home menu already inflate
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.cart_menu,menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_clear_cart)
        {
            cartDataSource.cleanCart(Common.currentUser.getUid(),
                    Common.currentRestaurant.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            Toast.makeText(getContext(),"Clear Cart Success" ,Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().removeAllStickyEvents();
        EventBus.getDefault().postSticky(new HideFABCart(false));
        EventBus.getDefault().postSticky(new CounterCartEvent(false));
        cartViewModel.onStop();
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        if(fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateItemInCartEvent(UpdateItemInCart event)
    {
        if(event.getCartItem() != null)
        {
            // First, save state of recycler view
            recyclerViewState = recycler_cart.getLayoutManager().onSaveInstanceState();
            cartDataSource.upadteCartItems(event.getCartItem())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            calculateTotalPrice();
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState); // Fix error refresh recycle view after update
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(),"[UPDATE CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void calculateTotalPrice() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double price) {
                        txt_total_price.setText(new StringBuilder("Total: $ ").append(Common.formatPrice(price)));
                    }

                    @Override
                    public void onError(Throwable e) {
                        if(!e.getMessage().contains("Query returned empty result set"))
                            Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_BRAINTREE_CODE)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                PaymentMethodNonce nonce = result.getPaymentMethodNonce();

                // Calculate sum cart
                cartDataSource.sumPriceInCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<Double>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onSuccess(Double totalPrice) {
                                // Get all item in cart to create order
                                compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<List<CartItem>>() {
                                    @Override
                                    public void accept(List<CartItem> cartItems) throws Exception {
                                        //Submit payment

                                        Map<String, String> headers = new HashMap<>();
                                        headers.put("Authorization", Common.buildToken(Common.authorizeKey));

                                        compositeDisposable.add(cloudFunctions.submitPayment(
                                                headers,
                                                totalPrice,
                                                nonce.getNonce())
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Consumer<BraintreeTransaction>() {
                                            @Override
                                            public void accept(BraintreeTransaction braintreeTransaction) throws Exception {
                                                if(braintreeTransaction.isSuccess())
                                                {
                                                    double finalPrice = totalPrice; //We will modify this formula for discount late
                                                    Order order = new Order();
                                                    order.setUserId(Common.currentUser.getUid());
                                                    order.setUserName(Common.currentUser.getName());
                                                    order.setUserPhone(Common.currentUser.getPhone());
                                                    order.setShippingAddress(address);
                                                    order.setComment(comment);

                                                    if(currentLocation != null)
                                                    {
                                                        order.setLat(currentLocation.getLatitude());
                                                        order.setLng(currentLocation.getLongitude());
                                                    }
                                                    else
                                                    {
                                                        order.setLat(-0.1f);
                                                        order.setLng(-0.1f);
                                                    }

                                                    order.setCartItemList(cartItems);
                                                    order.setTotalPayment(totalPrice);
                                                    order.setDiscount(0); // Modify with discount late
                                                    order.setFinalPayment(finalPrice);
                                                    order.setCod(false);
                                                    order.setTransactionId(braintreeTransaction.getTransaction().getId());

                                                    // Submit this order object to Firebase
                                                    //writeOrderToFirebase(order);
                                                    syncLocalTimeWithGlobalTime(order);

                                                }
                                            }
                                        }, new Consumer<Throwable>() {
                                            @Override
                                            public void accept(Throwable throwable) throws Exception {
                                                Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }));
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        Toast.makeText(getContext(),""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }));
                            }

                            @Override
                            public void onError(Throwable e) {
                                if(!e.getMessage().contains("Query returned empty result set"))
                                    Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    @Override
    public void onLoadTimeSuccess(Order order, long estimateTimeInMs) {
        order.setCreateDate(estimateTimeInMs);
        order.setOrderStatus(0);
        writeOrderToFirebase(order);
    }

    @Override
    public void onLoadOnlyTimeSuccess(long estimateTimeInMs) {
        // Do nothing
    }

    @Override
    public void onLoadTimeFailed(String message) {
        Toast.makeText(getContext(),""+message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();
    }

    @Override
    public void onSearchCategoryFound(CategoryModel categoryModel, CartItem cartItem) {
        FoodModel foodModel = Common.findFoodInListById(categoryModel,cartItem.getFoodId());
        if(foodModel != null)
        {
            showUpdateDialog(cartItem,foodModel);
        }
        else
            Toast.makeText(getContext(),"Food ID not found", Toast.LENGTH_SHORT).show();
    }

    private void showUpdateDialog(CartItem cartItem, FoodModel foodModel) {
        Common.selectedFood = foodModel;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_dialog_update_cart, null);
        builder.setView(itemView);

        // View
        Button btn_ok = (Button) itemView.findViewById(R.id.btn_ok);
        Button btn_cancel = (Button) itemView.findViewById(R.id.btn_cancel);

        RadioGroup rdi_group_size = (RadioGroup) itemView.findViewById(R.id.rdi_group_size);
        chip_group_user_selected_addon = (ChipGroup)itemView.findViewById(R.id.chip_group_user_selected_addon);
        ImageView img_add_on = (ImageView)itemView.findViewById(R.id.img_add_addon);
        img_add_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(foodModel.getAddon() != null)
                {
                    displayAddonList();
                    addonBottomSheetDialog.show();
                }
            }
        });

        if(foodModel.getSize() != null)
        {
            for(SizeModel sizeModel:foodModel.getSize())
            {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(b)
                            Common.selectedFood.setUserSelectedSize(sizeModel);
                        calculateTotalPrice();
                    }
                });

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,1.0f);
                radioButton.setLayoutParams(params);
                radioButton.setText(sizeModel.getName());
                radioButton.setTag(sizeModel.getPrice());

                rdi_group_size.addView(radioButton);

            }
            if(rdi_group_size.getChildCount() > 0)
            {
                RadioButton radioButton = (RadioButton)rdi_group_size.getChildAt(0);
                radioButton.setChecked(true);
            }

            displayAlreadySelectedAddon(chip_group_user_selected_addon,cartItem);

            AlertDialog dialog = builder.create();
            dialog.show();

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.CENTER);

            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cartDataSource.deleteCartItem(cartItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Integer>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Integer integer) {
                                    if(Common.selectedFood.getUserSelectedAddon() != null)
                                        cartItem.setFoodAddon(new Gson().toJson(Common.selectedFood.getUserSelectedAddon()));
                                    else
                                        cartItem.setFoodAddon("Nothing Addon");
                                    if(Common.selectedFood.getUserSelectedSize() != null)
                                        cartItem.setFoodSize(new Gson().toJson(Common.selectedFood.getUserSelectedSize()));
                                    else
                                        cartItem.setFoodSize("Default");

                                    cartItem.setFoodExtraPrice(Common.calculateExtraPrice(Common.selectedFood.getUserSelectedSize(), Common.selectedFood.getUserSelectedAddon()));

                                    compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(() -> {
                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                        calculateTotalPrice();
                                        dialog.dismiss();
                                        Toast.makeText(getContext(),"Update cart success", Toast.LENGTH_SHORT).show();
                                    }, throwable -> {
                                        Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    }));


                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });

            btn_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

    }

    private void displayAlreadySelectedAddon(ChipGroup chip_group_user_selected_addon, CartItem cartItem) {
        if(cartItem.getFoodAddon() != null && !cartItem.getFoodAddon().equals("Default"))
        {
            List<AddonModel> addonModels = new Gson().fromJson(
                    cartItem.getFoodAddon(), new TypeToken<List<AddonModel>>(){}.getType());

            Common.selectedFood.setUserSelectedAddon(addonModels);

            chip_group_user_selected_addon.removeAllViews();

            for (AddonModel addonModel:addonModels)
            {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_addon_item, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+$")
                        .append(addonModel.getPrice()).append(")"));

                chip.setClickable(false);
                chip.setOnCloseIconClickListener(view -> {
                    chip_group_user_selected_addon.removeView(view);
                    Common.selectedFood.getUserSelectedAddon().remove(addonModel);
                    calculateTotalPrice();
                });

                chip_group_user_selected_addon.addView(chip);
            }
        }
    }

    private void displayAddonList() {
        if(Common.selectedFood.getAddon() != null && Common.selectedFood.getAddon().size() > 0)
        {
            chip_group_addon.clearCheck();
            chip_group_addon.removeAllViews();

            edt_search.addTextChangedListener(this);

            for (AddonModel addonModel:Common.selectedFood.getAddon())
            {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_chip_with_delete_icon, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+$")
                .append(addonModel.getPrice()).append(")"));
                chip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(b)
                        {
                            if(Common.selectedFood.getUserSelectedAddon() == null)
                                Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                            Common.selectedFood.getUserSelectedAddon().add(addonModel);
                        }
                    }
                });
                chip_group_addon.addView(chip);
            }
        }
    }

    @Override
    public void onSearchCategoryNotFound(String message) {
        Toast.makeText(getContext(),message,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        chip_group_addon.clearCheck();
        chip_group_addon.removeAllViews();
        for(AddonModel addonModel:Common.selectedFood.getAddon())
        {
            if(addonModel.getName().toLowerCase().contains(charSequence.toString().toLowerCase()))
            {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_addon_item, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+$")
                        .append(addonModel.getPrice()).append(")"));
                chip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(b)
                        {
                            if(Common.selectedFood.getUserSelectedAddon() == null)
                                Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                            Common.selectedFood.getUserSelectedAddon().add(addonModel);
                        }
                    }
                });
                chip_group_addon.addView(chip);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
