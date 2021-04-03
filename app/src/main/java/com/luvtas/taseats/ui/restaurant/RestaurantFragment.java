package com.luvtas.taseats.ui.restaurant;

import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import com.luvtas.taseats.Adapter.MyRestaurantAdapter;
import com.luvtas.taseats.EventBus.CounterCartEvent;
import com.luvtas.taseats.EventBus.HideFABCart;
import com.luvtas.taseats.EventBus.MenuInflateEvent;
import com.luvtas.taseats.R;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RestaurantFragment extends Fragment {

    private RestaurantViewModel mViewModel;
    Unbinder unbinder;
    AlertDialog dialog;
    MyRestaurantAdapter adapter;
    LayoutAnimationController layoutAnimationController;

    @BindView(R.id.recycler_restaurant)
    RecyclerView recycler_restaurant;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mViewModel = ViewModelProviders.of(this).get(RestaurantViewModel.class);

        View root =  inflater.inflate(R.layout.fragment_restaurant, container, false);
        unbinder = ButterKnife.bind(this,root);
        initViews();

        mViewModel.getMessageError().observe(getViewLifecycleOwner(), message ->{
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
        mViewModel.getRestaurantListMutable().observe(getViewLifecycleOwner(), restaurantModels -> {
            dialog.dismiss();
            adapter = new MyRestaurantAdapter(getContext(),restaurantModels);
            recycler_restaurant.setAdapter(adapter);
        });

        return root;
    }

    private void initViews() {
        EventBus.getDefault().postSticky(new HideFABCart(true));  // hide when user back to this fragment
        setHasOptionsMenu(true);
        dialog = new AlertDialog.Builder(getContext()).setCancelable(false).setMessage("Please wait...").create();
        dialog.show();

        //layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(),R.anim.layout_item_from_left);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recycler_restaurant.setLayoutManager(linearLayoutManager);
        recycler_restaurant.addItemDecoration(new DividerItemDecoration(getContext(), linearLayoutManager.getOrientation()));

    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().postSticky(new CounterCartEvent(true));
        EventBus.getDefault().postSticky(new MenuInflateEvent(false));
    }
}
