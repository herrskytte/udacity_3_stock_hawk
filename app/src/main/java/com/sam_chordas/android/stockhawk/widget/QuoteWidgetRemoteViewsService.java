package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.StockDetailActivity;

public class QuoteWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor cursor = null;

            @Override
            public void onCreate() {
                // Nothing to do
                Log.e("QuoteRemoteViewsService", "onCreate");
            }

            @Override
            public void onDataSetChanged() {
                Log.e("QuoteRemoteViewsService", "onDataSetChanged");
                if (cursor != null) {
                    cursor.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                cursor = getContentResolver().query(
                        QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{ QuoteColumns._ID,
                                      QuoteColumns.HASDATA,
                                      QuoteColumns.SYMBOL,
                                      QuoteColumns.BIDPRICE,
                                      QuoteColumns.PERCENT_CHANGE,
                                      QuoteColumns.CHANGE,
                                      QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + "=? AND " + QuoteColumns.HASDATA + "=?",
                        new String[]{"1", "1"},
                        null);

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                Log.e("QuoteRemoteViewsService", "onDestroy");

                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }

            @Override
            public int getCount() {
                return cursor == null ? 0 : cursor.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                Log.e("QuoteRemoteViewsService", "getViewAt " + position);
                if (position == AdapterView.INVALID_POSITION ||
                        cursor == null || !cursor.moveToPosition(position)) {
                    return null;
                }

                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_collection_item);

                views.setTextViewText(R.id.stock_symbol, cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL)));
                if (Utils.showPercent){
                    views.setTextViewText(R.id.change, cursor.getString(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
                } else{
                    views.setTextViewText(R.id.change, cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE)));
                }

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(StockDetailActivity.STOCK_ID_EXTRA, cursor.getLong(cursor.getColumnIndex(QuoteColumns._ID)));
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (cursor.moveToPosition(position))
                    return cursor.getLong(cursor.getColumnIndex(QuoteColumns._ID));
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
