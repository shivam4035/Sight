package myname.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisInDomainResult;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

//8d13eb204f6244aab7054208d0b448dd

public class MainActivity extends AppCompatActivity implements SensorEventListener,TextToSpeech.OnInitListener {

    public VisionServiceClient visionServiceClient = new VisionServiceRestClient("80d28822cda5402b8d852830e82c0a79","https://westcentralus.api.cognitive.microsoft.com/vision/v1.0");

    TextToSpeech image;
    TextToSpeech face;
    TextToSpeech text;
    TextToSpeech speechVoice;
    TextToSpeech speechDate;
    TextToSpeech speechTime;


    private Sensor accelerometer;
    private SensorManager sm;


    private long curTime, lastUpdate;
    private float a,b,c,last_a,last_b,last_c;
    private final static long UPDATE_PERIOD = 100;
    private final static int SHAKE_THRESHOLD = 1000;
    protected static final int RESULT_SPEECH = 1;
    protected static final int REQUEST_CAMERA = 2;
    protected static final int CAMERA_FRONT_REQUEST_CODE = 3;
    protected static final int SELECT_FILE = 4;
    public int x=0;



    Bitmap mBitmap;
    ImageView imageView;


    TextView txt,txtDescription,txtName,txtText;


    StringBuilder stringBuilderText = new StringBuilder();
    StringBuilder stringBuilderImage = new StringBuilder();
    StringBuilder stringBuilderFace = new StringBuilder();
    String speech,lastWord,message,contactNumber;

    //Convert image to stream
    ByteArrayOutputStream outputStream;

    ByteArrayInputStream inputStreamforImage;
    ByteArrayInputStream inputStreamforText;
    ByteArrayInputStream InputStreamforFace;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        txtDescription = (TextView)findViewById(R.id.txtDescription);
        txtName = (TextView)findViewById(R.id.txtName);
        txtText = (TextView)findViewById(R.id.txtText);

        imageView = (ImageView)findViewById(R.id.imageView);

        //Convert image to stream
        outputStream = new ByteArrayOutputStream();

        mBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.dangel);
        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setImageBitmap(mBitmap);
        mBitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);

        image = new TextToSpeech(this, this);
        image.setLanguage(Locale.ENGLISH);
        face = new TextToSpeech(this,this);
        face.setLanguage(Locale.ENGLISH);
        text = new TextToSpeech(this,this);
        text.setLanguage(Locale.ENGLISH);
        speechVoice = new TextToSpeech(this,this);
        speechVoice.setLanguage(Locale.ENGLISH);
        speechDate = new TextToSpeech(this,this);
        speechDate.setLanguage(Locale.ENGLISH);
        speechTime = new TextToSpeech(this,this);
        speechTime.setLanguage(Locale.ENGLISH);


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
            if(speed > SHAKE_THRESHOLD && x==0){
                x=1;
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

                    speech = new String();

                    txt.setText(text.get(0));
                    x=0;
                    speech = txt.getText().toString();
                    //x=1;
                    if(txt.getText().toString()=="")
                    {
                        x=0;
                        txt.setText("Please try again !!");
                        speech = txt.getText().toString();
                        speechVoice.speak(String.valueOf(speech),QUEUE_FLUSH,null);
                    }


                    else if(speech.contains("recognise"))
                    {
                        speechVoice.speak(String.valueOf(speech),QUEUE_FLUSH,null);
                        getImageRecognized();

                    }
                    else if(speech.contains("gallery"))
                    {
                        x=0;
                        speechVoice.speak(String.valueOf(speech),QUEUE_FLUSH,null);
                        openGallery();
                    }
                    else if(speech.contains("front camera")||txt.getText().toString().contains("selfie"))
                    {
                        speechVoice.speak(String.valueOf(speech),QUEUE_FLUSH,null);
                        openFrontCamera();
                        x=0;
                    }

                    else if(speech.contains("capture")||txt.getText().toString().contains("camera"))
                    {
                        x=0;
                        speechVoice.speak(String.valueOf(speech),TextToSpeech.QUEUE_FLUSH,null);
                        CaptureImage();
                    }
                    else if(speech.contains("repeat"))
                    {
                        speechVoice.speak(String.valueOf(speech),QUEUE_FLUSH,null);
                        x=0;
                        repeatAudio();
                    }
                    else if(speech.contains("call"))
                    {
                         lastWord = speech.substring(speech.lastIndexOf(" ")+1);
                         contactNumber = getPhoneNumber(lastWord,this);
                        x=0;
                                if(contactNumber.equals("Unsaved")) {
                                    String fail;
                                    txt.setText("Please try again !!");
                                    fail = txt.getText().toString();
                                    speechVoice.speak(String.valueOf(fail),QUEUE_FLUSH,null);

                                }
                                else {

                                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contactNumber));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }

                        x=0;
                    }
                    else if(speech.contains("message")||speech.contains("send")) {
                        lastWord = speech.substring(speech.lastIndexOf(" ")+1);
                        contactNumber = getPhoneNumber(lastWord,this);
                        x=0;
                        if(contactNumber.equals("Unsaved")) {
                            String fail;
                            txt.setText("Please try again !!");
                            fail = txt.getText().toString();
                            speechVoice.speak(String.valueOf(fail),QUEUE_FLUSH,null);

                        }
                    }

                    else if(speech.contains("sms") || speech.contains("SMS"))
                    {
                        x=0;
                        message = speech.substring(speech.indexOf(' ')+1);
                        Intent smsMsgAppVar = new Intent(Intent.ACTION_VIEW);
                        smsMsgAppVar.setData(Uri.parse("sms:" +  contactNumber));
                        smsMsgAppVar.putExtra("sms_body",message);
                        startActivity(smsMsgAppVar);
                    }
                    else if(speech.contains("guidelines")||speech.contains("Guidelines"))
                    {
                        x=0;

                        String texthelp = "1. Shake mobile to give voice commands.\n" +
                                "2. Say Recognise to recognise the image selected.\n" +
                                "3. Say Open Camera to capture image.\n" +
                                "4. Say Call and the name of the person to call her.\n" +
                                "5. Say Send Message to and name of person to message. Followed by SMS and the message ahead.\n" +
                                "6. Say Open Gallery to select the image from gallery.\n" +
                                "7. Say Repeat to listen the information again.";

                        speechVoice.speak(String.valueOf(texthelp),QUEUE_FLUSH,null);
                    }
                    else if(speech.contains("time") || speech.contains("date"))
                    {
                        x=0;
                        DateFormat dfDate = new SimpleDateFormat("yyyy/MM/dd");
                        String date=dfDate.format(Calendar.getInstance().getTime());
                        DateFormat dfTime = new SimpleDateFormat("HH:mm");
                        String time = dfTime.format(Calendar.getInstance().getTime());
                        speechDate.speak("Today is "+String.valueOf(date),QUEUE_FLUSH,null);
                        speechTime.speak("And the time is "+String.valueOf(time),QUEUE_FLUSH,null);
                    }

                    else
                    {
                        x=0;
                        txt.setText("Please try again !!");
                        String text1 = txt.getText().toString();
                        speechVoice.speak(String.valueOf(text1),QUEUE_FLUSH,null);
                    }

                }

                break;
            }
            case REQUEST_CAMERA:{
                x=0;
                if(resultCode == Activity.RESULT_OK){

                        //Bundle bundle = data.getExtras();
                        mBitmap = (Bitmap) data.getExtras().get("data");
                        //Convert image to stream
                        outputStream = new ByteArrayOutputStream();
                        mBitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                        imageView.setImageBitmap(mBitmap);

                        getImageRecognized();

                    }
                    break;
                }
            case CAMERA_FRONT_REQUEST_CODE:{
                x=0;
                //Bundle bundle = data.getExtras();
                mBitmap = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(mBitmap);
                //mBitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                getImageRecognized();
                break;
            }

            case SELECT_FILE:{
                x=0;
                Uri selectImage = data.getData();
                imageView.setImageURI(selectImage);
                imageView.buildDrawingCache();
                mBitmap = imageView.getDrawingCache();
                outputStream = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                getImageRecognized();
            }
        }
    }

    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent.createChooser(intent,"Select File"),SELECT_FILE);
    }

    public String getPhoneNumber(String name,Context context) {
        String ret = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" like'%" + name +"%'";
        String[] projection = new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);
        if (c.moveToFirst()) {
            ret = c.getString(0);
        }
        c.close();
        if(ret==null)
            ret = "Unsaved";
        return ret;
    }

    private void repeatAudio() {

        face.speak(String.valueOf(stringBuilderFace),QUEUE_FLUSH,null);

        image.speak(String.valueOf(stringBuilderImage),QUEUE_FLUSH,null);

        text.speak(String.valueOf(stringBuilderText),QUEUE_FLUSH,null);
    }

    public void openFrontCamera() {

        Camera cam;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e("", "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

    }

    public void CaptureImage() {

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    startActivityForResult(intent,REQUEST_CAMERA);


    }

    public boolean isMobileDataEnable() {

        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // method is callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API and do whatever error
            // handling here as you want..
        }
        return mobileDataEnabled;
    }

    private boolean toggleMobileDataConnection(boolean ON) {

        try {
            // create instance of connectivity manager and get system service

            final ConnectivityManager conman = (ConnectivityManager) this
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            // define instance of class and get name of connectivity manager
            // system service class
            final Class conmanClass = Class
                    .forName(conman.getClass().getName());
            // create instance of field and get mService Declared field
            final Field iConnectivityManagerField = conmanClass
                    .getDeclaredField("mService");
            // Attempt to set the value of the accessible flag to true
            iConnectivityManagerField.setAccessible(true);
            // create instance of object and get the value of field conman
            final Object iConnectivityManager = iConnectivityManagerField
                    .get(conman);
            // create instance of class and get the name of iConnectivityManager
            // field
            final Class iConnectivityManagerClass = Class
                    .forName(iConnectivityManager.getClass().getName());
            // create instance of method and get declared method and type
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass
                    .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            // Attempt to set the value of the accessible flag to true
            setMobileDataEnabledMethod.setAccessible(true);
            // dynamically invoke the iConnectivityManager object according to
            // your need (true/false)
            setMobileDataEnabledMethod.invoke(iConnectivityManager, ON);
        } catch (Exception e) {
        }
        return true;
    }


    public void getImageRecognized() {

        x=0;

        if(isMobileDataEnable()==false)
        {
            // createNetErrorDialog();
            toggleMobileDataConnection(true);
        }
        else {
            inputStreamforImage = new ByteArrayInputStream(outputStream.toByteArray());
            inputStreamforText = new ByteArrayInputStream(outputStream.toByteArray());
            InputStreamforFace = new ByteArrayInputStream(outputStream.toByteArray());

            txtDescription.setText("Description..");
            txtName.setText("Name: ");
            txtText.setText("Text: ");

            final AsyncTask<InputStream, String, String> visionTask = new AsyncTask<InputStream, String, String>() {
                ProgressDialog mDialog = new ProgressDialog(MainActivity.this);

                @Override

                protected String doInBackground(InputStream... params) {
                    try {
                        publishProgress("Recognizing....");
                        String[] features = {"Description"};
                        String[] details = {};
                        //Log.d("Error", "Running1");
                        AnalysisResult result = visionServiceClient.analyzeImage(params[0], features, details);

                        String strResult = new Gson().toJson(result);
                        return strResult;

                    } catch (Exception e) {
                        return null;
                    }

                }

                @Override
                protected void onPreExecute() {
                    mDialog.show();
                }


                @Override
                protected void onPostExecute(String s) {
                    mDialog.dismiss();
                    AnalysisResult result = new Gson().fromJson(s, AnalysisResult.class);

                    stringBuilderImage.setLength(0);
                    stringBuilderImage.append("Description: ");

                    for (Caption caption : result.description.captions) {
                        stringBuilderImage.append(caption.text);
                    }

                    txtDescription.setText(stringBuilderImage);


                    image.speak(String.valueOf(stringBuilderImage), QUEUE_FLUSH, null);


                }

                @Override
                protected void onProgressUpdate(String... values) {
                    mDialog.setMessage(values[0]);
                }
            };

            visionTask.execute(inputStreamforImage);

            final AsyncTask<InputStream, String, String> recognizeCelbTask = new AsyncTask<InputStream, String, String>() {
                ProgressDialog mDialog = new ProgressDialog(MainActivity.this);

                @Override
                protected String doInBackground(InputStream... params) {
                    try {
                        publishProgress("Recognizing....");

                        String model = "celebrities";
                        AnalysisInDomainResult analysisInDomainResult = visionServiceClient.analyzeImageInDomain(params[0], model);

                        String celebResult = new Gson().toJson(analysisInDomainResult);
                        return celebResult;

                    } catch (Exception e) {
                        return null;
                    }

                }

                @Override
                protected void onPreExecute() {
                    mDialog.show();
                }

                @Override
                protected void onPostExecute(String s) {
                    mDialog.dismiss();
                    Gson gson = new Gson();
                    AnalysisInDomainResult analysisInDomainResult = gson.fromJson(s, AnalysisInDomainResult.class);

                    JsonArray detectedCelebs = analysisInDomainResult.result.get("celebrities").getAsJsonArray();
                    stringBuilderFace.setLength(0);

                    for (JsonElement element : detectedCelebs) {
                        JsonObject celeb = element.getAsJsonObject();

                        stringBuilderFace.append("Name: " + celeb.get("name").getAsString());
                    }
                    txtName.setText(stringBuilderFace);
                    face.speak(String.valueOf(stringBuilderFace), QUEUE_FLUSH, null);

                }

                @Override
                protected void onProgressUpdate(String... values) {
                    mDialog.setMessage(values[0]);
                }
            };

            recognizeCelbTask.execute(InputStreamforFace);

            final AsyncTask<InputStream, String, String> recognizeTextTask = new AsyncTask<InputStream, String, String>() {
                ProgressDialog mDialog = new ProgressDialog(MainActivity.this);

                @Override

                protected String doInBackground(InputStream... params) {
                    try {
                        publishProgress("Recognizing....");
                        OCR ocr = visionServiceClient.recognizeText(params[0], LanguageCodes.English, true);

                        String strResult = new Gson().toJson(ocr);
                        return strResult;

                    } catch (Exception e) {
                        return null;
                    }

                }

                @Override
                protected void onPreExecute() {
                    mDialog.show();
                }


                @Override
                protected void onPostExecute(String s) {
                    mDialog.dismiss();
                    OCR ocr = new Gson().fromJson(s, OCR.class);

                    stringBuilderText.setLength(0);

                    txtText.setText("");

                    for (Region region : ocr.regions) {
                        stringBuilderText.append("Text Written on image is: ");
                        for (Line line : region.lines) {
                            for (Word word : line.words) {
                                stringBuilderText.append(word.text + " ");
                            }
                        }

                    }

                    if (stringBuilderText.length() == 0) {
                        stringBuilderText.append("No text is there on the image");
                    }

                    txtText.setText(stringBuilderText);
                    text.speak(String.valueOf(stringBuilderText), QUEUE_FLUSH, null);

                }

                @Override
                protected void onProgressUpdate(String... values) {
                    mDialog.setMessage(values[0]);
                }
            };

            recognizeTextTask.execute(inputStreamforText);
        }

    }

    @Override
    public void onInit(int i) {

    }

    @Override
    public void onDestroy() {
        if (speechVoice != null) {
            speechVoice.stop();
            speechVoice.shutdown();

        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        x=0;
        onPause();
        if (speechVoice != null) {
            speechVoice.stop();
            speechVoice.shutdown();
        }
        super.onBackPressed();
    }
}

