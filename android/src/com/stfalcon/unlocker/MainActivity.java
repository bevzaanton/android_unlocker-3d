package com.stfalcon.unlocker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener {

    public static String MY_PREF = "my_pref";
    public static int LOW_Q = R.id.rb_low;
    public static int MEDIUM_Q = R.id.rb_medium;
    public static int HARD_Q = R.id.rb_hard;
    SensorManager sensorManager = null;
    //for accelerometer values
    TextView outputX;
    TextView outputY;
    TextView outputZ;
    //for orientation values
    TextView outputX2;
    TextView outputY2;
    TextView outputZ2;
    LinearLayout layout;
    public Button record, compar;
    TextView proc;
    GraphViewSeries pitchsaveDataSeries, rollsaveDataSeries;
    GraphView graphView;
    double startTime;
    boolean isSensorOn = false;
    boolean isPressed = false;
    boolean toConfirm = false;
    double[] temp = new double[3];
    ArrayList<Double> masShow = new ArrayList<Double>();
    ArrayList<Double> masSave = new ArrayList<Double>();
    ArrayList<Double> masConfirm = new ArrayList<Double>();
    ArrayList<double[]> accDataList = new ArrayList<double[]>();
    ArrayList<double[]> gyrDataList = new ArrayList<double[]>();
    ArrayList<double[]> filterDataList = new ArrayList<double[]>();
    private TextView tv_time;
    private Switch on_off;
    private TextView tv_new_time;
    private Activity context;
    private RadioGroup rb_quality;
    private int eventType = 333;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        initView();
        initGraph();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        startService(new Intent(this, LockService.class));


        UnlockApp.sPref = getSharedPreferences(MY_PREF, 0);
        boolean isSave = UnlockApp.sPref.getBoolean("isSave", false);
        if (isSave) {
            showSaveGraph();
        }


        //Quality
        int quality = 0;
        if (UnlockApp.sPref.contains("quality")) {
            quality = (UnlockApp.sPref.getInt("quality", R.id.rb_hard));
        } else {
            quality = (UnlockApp.sPref.getInt("quality", R.id.rb_hard));
            SharedPreferences.Editor editor = UnlockApp.sPref.edit();
            editor.putInt("quality", R.id.rb_hard);
            editor.commit();
        }
        Log.v("LOGER", "" + quality);


        rb_quality = (RadioGroup) findViewById(R.id.rg_quality);
        rb_quality.check(quality);
        rb_quality.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = UnlockApp.sPref.edit();
                editor.putInt("quality", checkedId);
                editor.commit();
            }
        });
    }

    public void initView() {
        proc = (TextView) findViewById(R.id.textView6);
        record = (Button) findViewById(R.id.record);
        compar = (Button) findViewById(R.id.compare);
        layout = (LinearLayout) findViewById(R.id.ll_graph);
        outputX = (TextView) findViewById(R.id.textView);
        outputY = (TextView) findViewById(R.id.textView1);
        outputZ = (TextView) findViewById(R.id.textView2);
        outputX2 = (TextView) findViewById(R.id.textView3);
        outputY2 = (TextView) findViewById(R.id.textView4);
        outputZ2 = (TextView) findViewById(R.id.textView5);
        tv_time = (TextView) findViewById(R.id.tv_time);
        tv_new_time = (TextView) findViewById(R.id.tv_new_time);
        record.setOnClickListener(this);
        compar.setOnClickListener(this);
        compar.setEnabled(false);

    }


    public void initMass() {
        masShow.clear();
        for (int i = 0; i < 200; i++) {
            masShow.add(0.0);
        }
    }


    public void initGraph() {
        graphView = new LineGraphView(this, "Saved");
        graphView.setScalable(true);
        graphView.setManualYAxisBounds(1.0, -1.0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record:
                if (!isSensorOn) {
                    StartNewGesture();
                    record.setText("STOP");
                } else if (!toConfirm) {
                    StopRecording();
                    record.setText("Record gesture");
                    compar.setEnabled(true);
                }
                break;
            case R.id.compare:
                if (!isSensorOn) {
                    StartConfirmGesture();
                    compar.setText("STOP");
                } else if (toConfirm) {
                    StopConfirm();
                    compar.setText("Compare gesture");
                }
                break;

        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //init button activated unlocker
        on_off = (Switch) findViewById(R.id.off_on);
        on_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                UnlockApp.getInstance().saveActivState(isChecked);
            }
        });
        if (UnlockApp.prefs.getString(UnlockApp.IS_ON, "false").equals("true")) {
            on_off.performClick();
            Log.i("Loger", "IS_ON = " + UnlockApp.prefs.getString(UnlockApp.IS_ON, "false"));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
    }


    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            {
                if (isSensorOn) {
                    switch (event.sensor.getType()) {
                        case Sensor.TYPE_ACCELEROMETER:
                            outputX.setText("x:" + Float.toString(event.values[0]));
                            outputY.setText("y:" + Float.toString(event.values[1]));
                            outputZ.setText("z:" + Float.toString(event.values[2]));
                            double[] accData = Comparison.lowFilter(event.values[0], event.values[1], event.values[2]);
                            accDataList.add(accData);
                            break;
                        case Sensor.TYPE_GYROSCOPE:
                            outputX2.setText("x:" + Float.toString(event.values[0]));
                            outputY2.setText("y:" + Float.toString(event.values[1]));
                            outputZ2.setText("z:" + Float.toString(event.values[2]));
                            double[] gyrData = Comparison.lowFilter(event.values[0], event.values[1], event.values[2]);
                            gyrDataList.add(gyrData);
                            getPoint(event.values[0], event.values[1], event.values[2], event.sensor.getType());
                            break;
                    }

                }

            }
        }
    }


    public void StartNewGesture() {
        initMass();
        startTime = System.currentTimeMillis();
        accDataList.clear();
        gyrDataList.clear();
        filterDataList.clear();
        isSensorOn = true;
        isPressed = true;
        masConfirm.clear();
        masSave.clear();
    }

    public void StopRecording() {
        isSensorOn = false;
        isPressed = false;
        SharedPreferences.Editor editor = UnlockApp.sPref.edit();
        editor.putBoolean("isSave", false);
        editor.commit();
        Validating();
        masConfirm.addAll(masSave);
        masSave.clear();
    }

    public void StartConfirmGesture() {
        initMass();
        startTime = System.currentTimeMillis();
        accDataList.clear();
        gyrDataList.clear();
        filterDataList.clear();
        isSensorOn = true;
        isPressed = true;
        toConfirm = true;
        layout.removeAllViews();
    }

    public void StopConfirm() {
        isSensorOn = false;
        isPressed = false;
        Validating();
    }

    public void Validating() {
        layout.removeAllViews();

        filterDataList = filterData();
        double[] pArr = new double[filterDataList.size()];
        for (int i = 0; i < filterDataList.size(); i++) {
            pArr[i] = filterDataList.get(i)[0];
        }
        double[] rArr = new double[filterDataList.size()];
        for (int i = 0; i < filterDataList.size(); i++) {
            rArr[i] = filterDataList.get(i)[1];
        }
        List<double[]> pList = Comparison.prepareArrays(pArr, rArr);
        if (pList == null) {
            Toast.makeText(context, getString(R.string.try_again), Toast.LENGTH_SHORT).show();
            return;
        }
        pArr = pList.get(0);
        rArr = pList.get(1);
        GraphView.GraphViewData[] pitchGraphViewData = new GraphView.GraphViewData[pArr.length];
        for (int i = 0; i < pArr.length; i++) {
            pitchGraphViewData[i] = new GraphView.GraphViewData(i, pArr[i]);
        }
        GraphView.GraphViewData[] rollGraphViewData = new GraphView.GraphViewData[rArr.length];
        for (int i = 0; i < rArr.length; i++) {
            rollGraphViewData[i] = new GraphView.GraphViewData(i, rArr[i]);
        }

        GraphViewSeries pitchDataSeries = new GraphViewSeries("pitch", new GraphViewSeries.GraphViewSeriesStyle(Color.BLACK, 4), pitchGraphViewData);
        GraphViewSeries rollDataSeries = new GraphViewSeries("roll", new GraphViewSeries.GraphViewSeriesStyle(Color.RED, 4), rollGraphViewData);

        boolean isSave = UnlockApp.sPref.getBoolean("isSave", false);
        boolean isConfirm = UnlockApp.sPref.getBoolean("isConfirm", false);

        if (isSave) {
            double savePitch[] = UnlockApp.loadArrays().get(0);
            double saveRoll[] = UnlockApp.loadArrays().get(1);
            GraphView.GraphViewData[] pitchGraphViewsaveData = new GraphView.GraphViewData[savePitch.length];
            for (int i = 0; i < savePitch.length; i++) {
                pitchGraphViewsaveData[i] = new GraphView.GraphViewData(i, savePitch[i]);
            }
            GraphView.GraphViewData[] rollGraphViewsaveData = new GraphView.GraphViewData[saveRoll.length];
            for (int i = 0; i < saveRoll.length; i++) {
                rollGraphViewsaveData[i] = new GraphView.GraphViewData(i, saveRoll[i]);
            }
            pitchsaveDataSeries = new GraphViewSeries("pitch1", new GraphViewSeries.GraphViewSeriesStyle(Color.GREEN, 2), pitchGraphViewsaveData);
            rollsaveDataSeries = new GraphViewSeries("roll1", new GraphViewSeries.GraphViewSeriesStyle(Color.YELLOW, 2), rollGraphViewsaveData);
        }

        GraphView graphView = new LineGraphView(
                this // context
                , "New Gesture" // heading
        );
        //graphView.addSeries(pitchDataSeries); // data
        graphView.addSeries(rollDataSeries); // data
        if (isSave) {
            double savePitch[] = UnlockApp.loadArrays().get(0);
            double saveRoll[] = UnlockApp.loadArrays().get(1);
            //graphView.addSeries(pitchsaveDataSeries);
            graphView.addSeries(rollsaveDataSeries);
            double[] x = pArr;
            double[] x1 = savePitch;
            double[] y = rArr;
            double[] y1 = saveRoll;

            double xPirsonKoef = Comparison.pirsonCompare(x, x1);
            double yPirsonKoef = Comparison.pirsonCompare(y, y1);
            UnlockApp.FACTOR factor = UnlockApp.getInstance().getFactors();
            boolean unlock = (xPirsonKoef + yPirsonKoef >= factor.getFactor())
                    && ((xPirsonKoef > factor.getPitchFactor() && yPirsonKoef > factor.getRollFactor())
                    || (yPirsonKoef > factor.getPitchFactor() && xPirsonKoef > factor.getRollFactor()));
            proc.setText("Unlock: " + unlock + " " + "compare Pitch = " +
                    new DecimalFormat("#.##").format(xPirsonKoef) + " " +
                    "Roll = " + new DecimalFormat("#.##").format(yPirsonKoef));
            tv_new_time.setText("Time: " + new DecimalFormat("#.##").format((System.currentTimeMillis() - startTime) / 1000));
            if (toConfirm && unlock) {
                UnlockApp.confArrays(pArr, rArr);
            }
        }
        layout.addView(graphView);
        if (!isSave) {
            UnlockApp.saveArrays(pArr, rArr);
            tv_time.setText("Time: " + new DecimalFormat("#.##").format((System.currentTimeMillis() - startTime) / 1000));
        }
        toConfirm = false;
    }

    public void CheckGesture() {
        showSaveGraph();
    }


    public void getPoint(Float x, Float y, Float z, int type) {

        double acc = x + y + z;
        double point = UnlockApp.lowPassFilterAcc(acc);
        masShow.add(point);
        masSave.add(point);
        if (masShow.size() > 200) {
            masShow.remove(0);

        }
        showOnGraph(masShow);
    }

    private void showOnGraph(ArrayList<Double> mas) {
        layout.removeAllViews();
        GraphView.GraphViewData[] accGraphViewsaveData = new GraphView.GraphViewData[mas.size()];
        for (int i = 0; i < mas.size(); i++) {
            accGraphViewsaveData[i] = new GraphView.GraphViewData(i, mas.get(i));
        }
        GraphViewSeries accGraphViewSeries1 = new GraphViewSeries("acc", new GraphViewSeries.GraphViewSeriesStyle(Color.BLACK, 4), accGraphViewsaveData);
        GraphViewSeries accGraphViewSeries = new GraphViewSeries("acc", new GraphViewSeries.GraphViewSeriesStyle(Color.GREEN, 2), accGraphViewsaveData);


        //GraphView graphView = new LineGraphView(this, "Saved");
        graphView.removeAllSeries();
        graphView.addSeries(accGraphViewSeries1);
        graphView.addSeries(accGraphViewSeries);
        layout.addView(graphView);
    }


    public ArrayList<double[]> filterData() {
        ArrayList<double[]> filterData = new ArrayList<double[]>();
        int len = 0;
        if (accDataList.size() > gyrDataList.size()) len = gyrDataList.size();
        else len = accDataList.size();
        for (int i = 0; i < len; i++) {
            filterData.add(UnlockApp.complementaryFilter(accDataList.get(i), gyrDataList.get(i)));
        }
        return filterData;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void showSaveGraph() {
        double[] savePitch = UnlockApp.loadArrays().get(0);
        double[] saveRoll = UnlockApp.loadArrays().get(1);
        GraphView.GraphViewData[] pitchGraphViewsaveData = new GraphView.GraphViewData[savePitch.length];
        for (int i = 0; i < savePitch.length; i++) {
            pitchGraphViewsaveData[i] = new GraphView.GraphViewData(i, savePitch[i]);
        }
        GraphView.GraphViewData[] rollGraphViewsaveData = new GraphView.GraphViewData[saveRoll.length];
        for (int i = 0; i < saveRoll.length; i++) {
            rollGraphViewsaveData[i] = new GraphView.GraphViewData(i, saveRoll[i]);
        }
        pitchsaveDataSeries = new GraphViewSeries("pitch1", new GraphViewSeries.GraphViewSeriesStyle(Color.RED, 2), pitchGraphViewsaveData);
        rollsaveDataSeries = new GraphViewSeries("roll1", new GraphViewSeries.GraphViewSeriesStyle(Color.YELLOW, 2), rollGraphViewsaveData);
        GraphView graphView = new LineGraphView(
                this // context
                , "Saved gesture" // heading
        );
        //graphView.addSeries(pitchsaveDataSeries);
        graphView.addSeries(rollsaveDataSeries);
        layout.addView(graphView);
    }


}

