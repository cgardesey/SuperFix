package com.service.provision.adapter;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.activity.RiderHomeActivity;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmBanner;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmChat;
import com.service.provision.realm.RealmProduct;
import com.service.provision.realm.RealmProductImage;
import com.service.provision.realm.RealmProvider;
import com.service.provision.realm.RealmService;
import com.service.provision.realm.RealmServiceCategory;
import com.service.provision.realm.RealmServiceImage;
import com.service.provision.util.RealmUtility;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmList;

public class ChooseRiderAccountAdapter extends RecyclerView.Adapter<ChooseRiderAccountAdapter.ViewHolder> implements Filterable {
    ChooseRiderAdapterInterface chooseRiderAdapterInterface;
    RealmList<RealmProvider> realmProviders;
    private Context mContext;

    public ChooseRiderAccountAdapter(ChooseRiderAdapterInterface chooseRiderAdapterInterface, RealmList<RealmProvider> realmProviders) {
        this.chooseRiderAdapterInterface = chooseRiderAdapterInterface;
        this.realmProviders = realmProviders;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.recycle_choose_provider_account, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        RealmProvider realmProvider = realmProviders.get(position);
        holder.name.setText(realmProvider.getTitle() != null && !realmProvider.getTitle().equals("") ? StringUtils.normalizeSpace((realmProvider.getTitle() + " " + realmProvider.getFirst_name() + " " + realmProvider.getOther_name() + " " + realmProvider.getLast_name()).replace("null", "")) : realmProvider.getProvider_name());
        holder.account_type.setText(realmProvider.getVehicle_type());
        holder.location.setText(realmProvider.getStreet_address());
        holder.cardview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog mProgress = new ProgressDialog(mContext);
                mProgress.setMessage(mContext.getString(R.string.pls_wait));
                mProgress.show();
                String provider_id = realmProvider.getProvider_id();

                StringRequest stringRequest = new StringRequest(
                        com.android.volley.Request.Method.POST,
                        API_URL + "provider-home-data",
                        response -> {
                            if (response != null) {
                                mProgress.dismiss();
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    Realm.init(mContext);
                                    Realm.getInstance(RealmUtility.getDefaultConfig(mContext)).executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            try {
                                                realm.where(RealmBanner.class).findAll().deleteAllFromRealm();
                                                realm.where(RealmServiceImage.class).findAll().deleteAllFromRealm();
                                                realm.where(RealmProduct.class).findAll().deleteAllFromRealm();
                                                realm.where(RealmService.class).findAll().deleteAllFromRealm();
                                                realm.where(RealmProductImage.class).findAll().deleteAllFromRealm();
                                                realm.where(RealmServiceImage.class).findAll().deleteAllFromRealm();
                                                realm.where(RealmCart.class).findAll().deleteAllFromRealm();
                                                realm.where(RealmChat.class).findAll().deleteAllFromRealm();

                                                realm.createOrUpdateAllFromJson(RealmBanner.class, jsonObject.getJSONArray("banners"));
                                                realm.createOrUpdateAllFromJson(RealmServiceCategory.class, jsonObject.getJSONArray("service_categories"));
                                                realm.createOrUpdateAllFromJson(RealmProduct.class, jsonObject.getJSONArray("products"));
                                                realm.createOrUpdateAllFromJson(RealmService.class, jsonObject.getJSONArray("services"));
                                                realm.createOrUpdateAllFromJson(RealmProductImage.class, jsonObject.getJSONArray("product_images"));
                                                realm.createOrUpdateAllFromJson(RealmServiceImage.class, jsonObject.getJSONArray("service_images"));
                                                realm.createOrUpdateAllFromJson(RealmCart.class, jsonObject.getJSONArray("scoped_carts"));
                                                realm.createOrUpdateAllFromJson(RealmChat.class, jsonObject.getJSONArray("chats"));

                                                if (jsonObject.has("ride_history")) {
                                                    JSONObject ride_historyJson = jsonObject.getJSONObject("ride_history");
                                                    JSONObject customer_json = jsonObject.getJSONObject("customer");

                                                    PreferenceManager
                                                            .getDefaultSharedPreferences(mContext)
                                                            .edit()
                                                            .putString("RIDE_HISTORY_ID", ride_historyJson.getString("ride_history_id"))
                                                            .putString("PICKUP_LAT", String.valueOf(ride_historyJson.getDouble("pickup_latitude")))
                                                            .putString("PICKUP_LONG", String.valueOf(ride_historyJson.getDouble("pickup_longitude")))
                                                            .putString("DESTINATION_LAT", String.valueOf(ride_historyJson.getDouble("destination_latitude")))
                                                            .putString("DESTINATION_LONG", String.valueOf(ride_historyJson.getDouble("destination_longitude")))
                                                            .putString("CUSTOMER_NAME", customer_json.getString("name"))
                                                            .putString("CUSTOMER_PROFILE_IMAGE_URL", customer_json.getString("profile_image_url"))
                                                            .putString("PICKUP_ADDRESS", ride_historyJson.getString("pickup_address"))
                                                            .putString("DESTINATION_ADDRESS", ride_historyJson.getString("destination_address"))
                                                            .putString("SERVICE_ID", ride_historyJson.getString("service_id"))
                                                            .putString("CUSTOMER_PRIMARY_CONTACT", customer_json.getString("primary_contact"))
                                                            .putString("CUSTOMER_CONFIRMATION_TOKEN", customer_json.getString("confirmation_token"))
                                                            .putString("PROVIDER_ID", provider_id)
                                                            .putString("CUSTOMER_ID", customer_json.getString("customer_id"))
                                                            .apply();


                                                    if (ride_historyJson.isNull("start_time") || ride_historyJson.getString("start_time").equals("null")) {
                                                        PreferenceManager
                                                                .getDefaultSharedPreferences(mContext)
                                                                .edit()
                                                                .putBoolean("DRIVING_TO_PICKUP", true)
                                                                .putBoolean("DRIVING_TO_DESTINATION", false)
                                                                .apply();
                                                    }
                                                    else {
                                                        PreferenceManager
                                                                .getDefaultSharedPreferences(mContext)
                                                                .edit()
                                                                .putBoolean("DRIVING_TO_PICKUP", false)
                                                                .putBoolean("DRIVING_TO_DESTINATION", true)
                                                                .apply();
                                                    }
                                                }
                                                else {
                                                    PreferenceManager
                                                            .getDefaultSharedPreferences(mContext)
                                                            .edit()
                                                            .putBoolean("DRIVING_TO_PICKUP", false)
                                                            .putBoolean("DRIVING_TO_DESTINATION", false)
                                                            .apply();
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            PreferenceManager
                                                    .getDefaultSharedPreferences(mContext)
                                                    .edit()
                                                    .putString("ROLE", "PROVIDER")
                                                    .putString("PROVIDER_ID", realmProvider.getProvider_id())
                                                    .apply();
                                            mContext.startActivity(new Intent(mContext, RiderHomeActivity.class));
                                            ((Activity)mContext).finish();
                                        }
                                    });

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            myVolleyError(mContext, error);
                            mProgress.dismiss();
                            Log.d("Cyrilll", error.toString());
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params  = new HashMap<>();
                        params.put("provider_id", provider_id);
                        return params;
                    }

                    @Override
                    public Map getHeaders() throws AuthFailureError {
                        HashMap headers = new HashMap();
                        headers.put("accept", "application/json");
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(mContext).getString(APITOKEN, ""));
                        return headers;
                    }
                };
                stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                        0,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                InitApplication.getInstance().addToRequestQueue(stringRequest);
            }
        });
    }

    public interface ChooseRiderAdapterInterface {
        void onListItemClick(ArrayList<RealmProvider> files, int position, ViewHolder holder);
    }

    @Override
    public int getItemCount() {
        return realmProviders.size();
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    public void reload(RealmList<RealmProvider> realmProviders) {
        this.realmProviders = realmProviders;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public TextView location, name, account_type;
        public CardView cardview;

        public ViewHolder(View view) {
            super(view);
            location = view.findViewById(R.id.google_location);
            name = view.findViewById(R.id.provider_name);
            account_type = view.findViewById(R.id.account_type);
            cardview = view.findViewById(R.id.cardview);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

        }

        @Override
        public boolean onLongClick(View view) {
            return false;
        }
    }
}

