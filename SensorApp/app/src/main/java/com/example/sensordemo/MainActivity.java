package com.example.sensordemo;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, lightSensor, proximitySensor;

    private TextView accelData, lightData, proximityData;
    private View rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelData = findViewById(R.id.accelData);
        lightData = findViewById(R.id.lightData);
        proximityData = findViewById(R.id.proximityData);
        rootLayout = findViewById(R.id.rootLayout);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        // Handle missing sensors
        if (accelerometer == null) accelData.setText(R.string.sensor_not_available);
        if (lightSensor == null) lightData.setText(R.string.sensor_not_available);
        if (proximitySensor == null) proximityData.setText(R.string.sensor_not_available);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager != null) {
            if (accelerometer != null)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);

            if (lightSensor != null)
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);

            if (proximitySensor != null)
                sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            accelData.setText(getString(R.string.accel_format,
                    event.values[0], event.values[1], event.values[2]));
        } else if (sensorType == Sensor.TYPE_LIGHT) {
            float lightValue = event.values[0];
            lightData.setText(getString(R.string.light_format, lightValue));

            // Dynamic background adjustment based on light level
            if (lightValue < 50) {
                rootLayout.setBackgroundColor(Color.DKGRAY);
            } else if (lightValue < 500) {
                rootLayout.setBackgroundColor(Color.GRAY);
            } else if (lightValue < 1000) {
                // Default orange gradient
                rootLayout.setBackgroundResource(R.drawable.gradient_background);
            } else if (lightValue < 2000) {
                // Pure Red for 1000+
                rootLayout.setBackgroundColor(Color.RED);
            } else if (lightValue < 3000) {
                rootLayout.setBackgroundColor(Color.MAGENTA);
            } else if (lightValue < 4000) {
                rootLayout.setBackgroundColor(Color.BLUE);
            } else if (lightValue < 5000) {
                rootLayout.setBackgroundColor(Color.CYAN);
            } else {
                rootLayout.setBackgroundColor(Color.GREEN);
            }


        } else if (sensorType == Sensor.TYPE_PROXIMITY) {
            proximityData.setText(getString(R.string.proximity_format, event.values[0]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}