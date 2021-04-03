package com.luvtas.taseats.Remote;

import com.luvtas.taseats.Model.BraintreeToken;
import com.luvtas.taseats.Model.BraintreeTransaction;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ICloudFunctions {
    @GET("token")
    Observable<BraintreeToken> getToken(@HeaderMap Map<String, String> headers);

    @POST("checkout")
    @FormUrlEncoded
    Observable<BraintreeTransaction> submitPayment(
            @HeaderMap Map<String, String> headers,
            @Field("amount") double amount,
            @Field("payment_method_nonce") String nonce);
}
