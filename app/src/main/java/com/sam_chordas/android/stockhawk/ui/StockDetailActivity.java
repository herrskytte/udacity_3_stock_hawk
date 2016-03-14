package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class StockDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    public static final String STOCK_ID_EXTRA = "stockId";
    private static final int CURSOR_LOADER_ID = 1;

    LineChartView mLineChart;
    TextView symbol;
    TextView bidPrice;
    TextView change;

    long mCurrentQuoteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCurrentQuoteId = getIntent().getLongExtra(STOCK_ID_EXTRA, -1);

        mLineChart = (LineChartView) findViewById(R.id.linechart);
        symbol = (TextView) findViewById(R.id.stock_symbol);
        bidPrice = (TextView) findViewById(R.id.bid_price);
        change = (TextView) findViewById(R.id.change);

        mLineChart.setYLabels(AxisController.LabelPosition.OUTSIDE)
                .setLabelsColor(Color.WHITE);

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.HASDATA, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns._ID + " = ?",
                new String[]{Long.toString(mCurrentQuoteId)},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor){
        if(cursor.moveToNext()){
            String symb = cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL));
            symbol.setText(symb);
            bidPrice.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.BIDPRICE)));
            int sdk = Build.VERSION.SDK_INT;
            if (cursor.getInt(cursor.getColumnIndex(QuoteColumns.ISUP)) == 1){
                if (sdk < Build.VERSION_CODES.JELLY_BEAN){
                    change.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.percent_change_pill_green));
                }else {
                    change.setBackground(
                            getResources().getDrawable(R.drawable.percent_change_pill_green));
                }
            } else{
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    change.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.percent_change_pill_red));
                } else{
                    change.setBackground(
                            getResources().getDrawable(R.drawable.percent_change_pill_red));
                }
            }
            if (Utils.showPercent){
                change.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
            } else{
                change.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE)));
            }

            new GetHistoricDataTask().execute(symb);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
    }

    private class GetHistoricDataTask extends AsyncTask<String, Void, ArrayList<Point>>{

        @Override
        protected ArrayList<Point> doInBackground(String... params) {
            StringBuilder urlStringBuilder = new StringBuilder();
            try{
                Calendar c = Calendar.getInstance();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                String today = df.format(c.getTime());
                c.add(Calendar.DATE, -30);
                String lastMont = df.format(c.getTime());

                // Base URL for the Yahoo query
                urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol "
                        + "=", "UTF-8"));
                urlStringBuilder.append(
                        URLEncoder.encode("\"" + params[0] + "\" " +
                                "and startDate=\"" + lastMont + "\" " +
                                "and endDate=\"" + today + "\"", "UTF-8"));
                urlStringBuilder.append("&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String urlString = urlStringBuilder.toString();
            ArrayList<Point> pointList = null;

            try{
                Request request = new Request.Builder()
                        .url(urlString)
                        .build();
                OkHttpClient client = new OkHttpClient();
                Response response = client.newCall(request).execute();
                String getResponse = response.body().string();


                pointList = Utils.quoteJsonToPoints(getResponse);


            } catch (IOException e){
                e.printStackTrace();
            }

            return pointList;
        }

        @Override
        protected void onPostExecute(ArrayList<Point> pointList) {
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            LineSet dataSet = new LineSet();
            for (Point p : pointList) {
                min = Math.min(min, p.getValue());
                max = Math.max(max, p.getValue());
                dataSet.addPoint(p);
            }
            mLineChart.setAxisBorderValues((int) min,  (int) max);
            dataSet.setColor(getColor(R.color.material_blue_500));
            mLineChart.addData(dataSet);
            mLineChart.show();
        }
    }

}
