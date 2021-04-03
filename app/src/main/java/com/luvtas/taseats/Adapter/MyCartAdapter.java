package com.luvtas.taseats.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.luvtas.taseats.Common.Common;
import com.luvtas.taseats.Database.CartItem;
import com.luvtas.taseats.EventBus.UpdateItemInCart;
import com.luvtas.taseats.Model.AddonModel;
import com.luvtas.taseats.Model.SizeModel;
import com.luvtas.taseats.R;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Type;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyCartAdapter extends RecyclerView.Adapter<MyCartAdapter.MyViewHolder> {

    Context context;
    List<CartItem> cartItemList;
    Gson gson;

    public MyCartAdapter(Context context, List<CartItem> cartItemList) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.gson = new Gson();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_cart_item, parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(cartItemList.get(position).getFoodImage()).into(holder.img_cart);
        holder.txt_food_name.setText(new StringBuilder(cartItemList.get(position).getFoodName()));
        holder.txt_food_price.setText(new StringBuilder("$ ").append(cartItemList.get(position).getFoodPrice() + cartItemList.get(position).getFoodExtraPrice()));
        holder.numberButton.setNumber(String.valueOf(cartItemList.get(position).getFoodQuantity()));

        if(cartItemList.get(position).getFoodSize() != null)
        {
            if(cartItemList.get(position).getFoodSize().equals("Default"))
                holder.txt_food_size.setText(new StringBuilder("Size: ").append("Default"));
            else
            {
                SizeModel sizeModel = gson.fromJson(cartItemList.get(position).getFoodSize(), new TypeToken<SizeModel>(){}.getType());
                holder.txt_food_size.setText(new StringBuilder("Size: ").append(sizeModel.getName()));
            }
        }

        if(cartItemList.get(position).getFoodAddon() != null)
        {
            if(cartItemList.get(position).getFoodAddon().equals("Default"))
                holder.txt_food_addon.setText(new StringBuilder("Addon: ").append("Default"));
            else
            {
                List<AddonModel> addonModels = gson.fromJson(cartItemList.get(position).getFoodAddon(), new TypeToken<List<AddonModel>>(){}.getType());
                if(addonModels.size() != 0)
                    holder.txt_food_addon.setText(new StringBuilder("Addon: ").append(Common.getListAddon(addonModels)));
                else
                    holder.txt_food_addon.setText(new StringBuilder("Addon: ").append("Default"));
            }
        }

        // Event
        holder.numberButton.setOnValueChangeListener((view, oldValue, newValue) -> {
            // when user click this button, we will update database
            cartItemList.get(position).setFoodQuantity(newValue);
            EventBus.getDefault().postSticky(new UpdateItemInCart(cartItemList.get(position)));
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public CartItem getItemAtPosition(int pos) {
        return cartItemList.get(pos);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private Unbinder unbinder;

        @BindView(R.id.img_cart)
        ImageView img_cart;
        @BindView(R.id.txt_food_price)
        TextView txt_food_price;
        @BindView(R.id.txt_food_name)
        TextView txt_food_name;
        @BindView(R.id.number_button)
        ElegantNumberButton numberButton;
        @BindView(R.id.txt_food_size)
        TextView txt_food_size;
        @BindView(R.id.txt_food_addon)
        TextView txt_food_addon;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);

        }
    }
}
