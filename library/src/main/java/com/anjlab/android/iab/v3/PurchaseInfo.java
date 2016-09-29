/**
 * Copyright 2014 AnjLab and Unic8
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anjlab.android.iab.v3;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * With this PurchaseInfo a developer is able verify
 * a purchase from the google play store on his own
 * server. An example implementation of how to verify
 * a purchase you can find <a href="https://github.com/mgoldsborough/google-play-in-app-billing-verification/blob/master/library/GooglePlay/InAppBilling/GooglePlayResponseValidator.php#L64">here</a>
 */
public class PurchaseInfo implements Parcelable {

    private static final String LOG_TAG = "iabv3.purchaseInfo";

    public enum PurchaseState {
        PurchasedSuccessfully, Canceled, Refunded, SubscriptionExpired;
    }

    public final String responseJson;
    public final ResponseData response;
    public final String signature;

    public PurchaseInfo(String responseData, String signature) {
        this.responseJson = responseData;
        this.response = parseResponseData(responseData);
        this.signature = signature;
    }

    public static class ResponseData implements Parcelable {

        public String orderId;
        public String packageName;
        public String productId;
        public Date purchaseTime;
        public PurchaseState purchaseState;
        public String developerPayload;
        public String purchaseToken;
        public boolean autoRenewing;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.orderId);
            dest.writeString(this.packageName);
            dest.writeString(this.productId);
            dest.writeLong(purchaseTime != null ? purchaseTime.getTime() : -1);
            dest.writeInt(this.purchaseState == null ? -1 : this.purchaseState.ordinal());
            dest.writeString(this.developerPayload);
            dest.writeString(this.purchaseToken);
            dest.writeByte(autoRenewing ? (byte) 1 : (byte) 0);
        }

        public ResponseData() {
        }

        protected ResponseData(Parcel in) {
            this.orderId = in.readString();
            this.packageName = in.readString();
            this.productId = in.readString();
            long tmpPurchaseTime = in.readLong();
            this.purchaseTime = tmpPurchaseTime == -1 ? null : new Date(tmpPurchaseTime);
            int tmpPurchaseState = in.readInt();
            this.purchaseState = tmpPurchaseState == -1 ? null : PurchaseState.values()[tmpPurchaseState];
            this.developerPayload = in.readString();
            this.purchaseToken = in.readString();
            this.autoRenewing = in.readByte() != 0;
        }

        public static final Parcelable.Creator<ResponseData> CREATOR = new Parcelable.Creator<ResponseData>() {
            public ResponseData createFromParcel(Parcel source) {
                return new ResponseData(source);
            }

            public ResponseData[] newArray(int size) {
                return new ResponseData[size];
            }
        };
    }

    private static PurchaseState getPurchaseState(int state) {
        switch (state) {
            case 0:
                return PurchaseState.PurchasedSuccessfully;
            case 1:
                return PurchaseState.Canceled;
            case 2:
                return PurchaseState.Refunded;
            case 3:
                return PurchaseState.SubscriptionExpired;
            default:
                return PurchaseState.Canceled;
        }
    }

    private ResponseData parseResponseData(String responseData) {
        try {
            JSONObject json = new JSONObject(responseData);
            ResponseData data = new ResponseData();
            data.orderId = json.optString(Constants.RESPONSE_ORDER_ID);
            data.packageName = json.optString(Constants.RESPONSE_PACKAGE_NAME);
            data.productId = json.optString(Constants.RESPONSE_PRODUCT_ID);
            long purchaseTimeMillis = json.optLong(Constants.RESPONSE_PURCHASE_TIME, 0);
            data.purchaseTime = purchaseTimeMillis != 0 ? new Date(purchaseTimeMillis) : null;
            data.purchaseState = getPurchaseState(json.optInt(Constants.RESPONSE_PURCHASE_STATE, 1));
            data.developerPayload = json.optString(Constants.RESPONSE_DEVELOPER_PAYLOAD);
            data.purchaseToken = json.getString(Constants.RESPONSE_PURCHASE_TOKEN);
            data.autoRenewing = json.optBoolean(Constants.RESPONSE_AUTO_RENEWING);
            return data;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Failed to parse response data", e);
            return null;
        }
    }

    public boolean isPurchased() {
        return response.purchaseState == PurchaseState.PurchasedSuccessfully;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.responseJson);
        dest.writeString(this.signature);
    }

    protected PurchaseInfo(Parcel in) {
        this.responseJson = in.readString();
        this.response = parseResponseData(responseJson);
        this.signature = in.readString();
    }

    public static final Parcelable.Creator<PurchaseInfo> CREATOR = new Parcelable.Creator<PurchaseInfo>() {
        public PurchaseInfo createFromParcel(Parcel source) {
            return new PurchaseInfo(source);
        }

        public PurchaseInfo[] newArray(int size) {
            return new PurchaseInfo[size];
        }
    };
}
