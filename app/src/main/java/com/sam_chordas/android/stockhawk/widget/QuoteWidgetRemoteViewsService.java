package com.sam_chordas.android.stockhawk.widget;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by fsk on 11.03.2016.
 */
public class QuoteWidgetRemoteViewsService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
