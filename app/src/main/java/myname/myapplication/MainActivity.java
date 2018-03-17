package myname.myapplication;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private Sensor accelerometer;
    private SensorManager sm;
    private long curTime, lastUpdate;
    private float a,b,c,last_a,last_b,last_c;
    private final static long UPDATE_PERIOD = 100;
    private final static int SHAKE_THRESHOLD = 1000;
    protected static final int RESULT_SPEECH = 1;
    protected static final int REQUEST_CAMERA = 2;
    protected static final int SELECT_FILE = 3;
    ImageView imageView;
    TextView txt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //For shake dismiss
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        txt = (TextView)findViewById(R.id.txt);

            curTime = lastUpdate = (long) 0.0;
            a = b = c = last_a = last_b = last_c = (float) 0.0;
            sm.registerListener((SensorEventListener) this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);


    }//onCreate ends here



    //Setting dismiss by shaking
    @Override
    public void onSensorChanged(SensorEvent event) {
        long curTime  = System.currentTimeMillis();

        if((curTime-lastUpdate) > UPDATE_PERIOD){
            long diffTime = (curTime - lastUpdate);
            lastUpdate = curTime;

            a = event.values[0];
            b = event.values[1];
            c = event.values[2];

            float speed = Math.abs(a + b + c - last_a - last_b - last_c)/diffTime * 10000;
            if(speed > SHAKE_THRESHOLD){
                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                   txt.setText("");
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Oops! Your device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
            last_a = a;
            last_b = b;
            last_c  = c;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    txt.setText(text.get(0));

                    if(txt.getText().toString().contains("capture")||txt.getText().toString().contains("camera"))
                    {
                        CaptureImage();
                    }
                }

                break;
            }
            case REQUEST_CAMERA:{
                if(resultCode == Activity.RESULT_OK){


                    if(requestCode == REQUEST_CAMERA){
                        Log.e("camera", "not working1 ");

                        //Bundle bundle = data.getExtras();
                        final Bitmap mbitmap = (Bitmap) data.getExtras().get("data");
                        Log.e("camera", "not working2 ");
                        imageView.setImageBitmap(mbitmap);
                        Log.e("camera", "not working3 ");


                    }

                }
            }
        }
    }



    public void CaptureImage()
    {


                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    startActivityForResult(intent,REQUEST_CAMERA);


    }





}

