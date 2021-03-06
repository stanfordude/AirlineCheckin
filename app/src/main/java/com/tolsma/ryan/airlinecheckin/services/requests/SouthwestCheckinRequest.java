package com.tolsma.ryan.airlinecheckin.services.requests;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;
import com.squareup.okhttp.ResponseBody;
import com.squareup.otto.Bus;
import com.tolsma.ryan.airlinecheckin.CleanupApplication;
import com.tolsma.ryan.airlinecheckin.MainActivity;
import com.tolsma.ryan.airlinecheckin.components.AppComponent;
import com.tolsma.ryan.airlinecheckin.model.events.NotificationEvent;
import com.tolsma.ryan.airlinecheckin.utils.ConstantsConfig;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

import hugo.weaving.DebugLog;
import retrofit.Call;

/**
 * Created by ryan on 1/14/16.
 */
public class SouthwestCheckinRequest implements Runnable {
    public final static String CHECKIN_REQUEST = "CheckinRequest"; //literally here for nothing

    public SouthwestCheckinRequest() {
    }

    @DebugLog
    public static boolean isCheckedIn() {
        AppComponent dap = CleanupApplication.getAppComponent();
        Context ctx = dap.context();
        NotificationEvent ne = new NotificationEvent();
        Bus eventBus = dap.eventBus();
        Document htmlDoc;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class), PendingIntent.FLAG_ONE_SHOT);
        Call<ResponseBody> responseBodyCall;
        String response = "";

        ne.setId(CHECKIN_REQUEST.hashCode());
        ne.setIntent(pi);

        responseBodyCall = dap.southwestAPI().sendCheckinButton(
                "true",
                ConstantsConfig.SOUTHWEST_CHECKIN_BUTTON
        );


        try {
            response = responseBodyCall.execute().body().string();
            //Shouldn't have to actually do anything, just send request and load next page

            htmlDoc = Jsoup.parse(response);
            Elements elements = htmlDoc.select("span." + ConstantsConfig.SOUTHWEST_BOARDING_INFO_CLASS);
            if (elements == null) {
                //Could possibly occur, if previous checkins were unsuccessful
                throw new IOException("Parsing of Southwest page is null");
            } else if (elements.first() == null || elements.last() == null) {
                throw new IOException("Southwest page elements not loaded");
            }
            String boardingInfo = elements.first().text() + " : " + elements.last().text();

            ne.setTitle("Boarding Pass Seat " + boardingInfo);
            ne.setPriority(10);
            eventBus.post(ne);
            eventBus.post(ne.getTitleToastEvent());

            return true;
        } catch (IOException e) {
            Crashlytics.logException(e);
            Crashlytics.logException(new Throwable(response));
            ne.setTitle("Southwest Check In Error: " + e.getMessage());
            ne.setMessage("There has been an unknown error in checking in to " +
                    "the Southwest website. Please manually check in using the Southwest website at" +
                    "this time. We apologize for this inconvenience.");
            eventBus.post(ne);
            eventBus.post(ne.getTitleToastEvent());
            return false;
        }


    }

    @Override
    public void run() {
        isCheckedIn();
    }

}
