package com.luvtas.taseats.Adapter;



import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.asksira.loopingviewpager.LoopingPagerAdapter;

import com.bumptech.glide.Glide;
import com.luvtas.taseats.EventBus.BestDealItemClick;
import com.luvtas.taseats.Model.BestDealModel;
import com.luvtas.taseats.R;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyBestDealsAdapter extends LoopingPagerAdapter<BestDealModel> {

    @BindView(R.id.img_best_deal)
    ImageView img_best_deal;
    @BindView(R.id.txt_best_deal)
    TextView txt_best_deal;

    Unbinder unbinder;

    public MyBestDealsAdapter(Context context, List<? extends BestDealModel> itemList, boolean isInfinite) {
        super(context, itemList, isInfinite);
    }

    @Override
    protected void bindView(View convertView, int listPosition, int viewType) {
        unbinder = ButterKnife.bind(this,convertView);
        Glide.with(convertView).load(getItemList().get(listPosition).getImage()).into(img_best_deal);
        txt_best_deal.setText(getItemList().get(listPosition).getName());

        convertView.setOnClickListener(view -> {
            EventBus.getDefault().postSticky(new BestDealItemClick(getItemList().get(listPosition)));
        });

    }

    @Override
    protected View inflateView(int viewType, ViewGroup container, int listPosition) {
        return LayoutInflater.from(getContext()).inflate(R.layout.layout_best_deal_item, container,false);
    }
}
