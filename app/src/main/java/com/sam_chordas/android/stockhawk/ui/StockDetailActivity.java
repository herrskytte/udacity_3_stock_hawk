package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;

public class StockDetailActivity extends AppCompatActivity {

    public static final String STOCK_ID_EXTRA = "stockId";

    LineChartView mLineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLineChart = (LineChartView) findViewById(R.id.linechart);


        LineSet dataSet = new LineSet();
        dataSet.addPoint("12.01.16", 100);
        dataSet.addPoint("13.01.16", 99);
        dataSet.addPoint("14.01.16", 104);
        dataSet.addPoint("15.01.16", 109);
        dataSet.addPoint("16.01.16", 80);

        mLineChart.addData(dataSet);
        mLineChart.show();
    }

}
