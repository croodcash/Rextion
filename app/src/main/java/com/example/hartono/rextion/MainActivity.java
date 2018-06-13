package com.example.hartono.rextion;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static final String TESS_DATA = "/tessdata";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess";
    private TextView textView;
    private ImageView imageView;
    private TessBaseAPI tessBaseAPI;
    private Uri outputFileDir;
    private String mCurrentPhotoPath;
    /*static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) this.findViewById(R.id.textView);
        imageView = (ImageView) findViewById(R.id.imageView);
        final Activity activity = this;
        checkPermission();
        this.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
                dispatchTakePictureIntent();
            }
        });
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 1024);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1024) {
            if (resultCode == Activity.RESULT_OK) {
                prepareTessData();
                startOCR();
                //startOCR(outputFileDir);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Result canceled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Activity result failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void prepareTessData(){
        try{
            File dir = getExternalFilesDir(TESS_DATA);
            if(!dir.exists()){
                if (!dir.mkdir()) {
                    Toast.makeText(getApplicationContext(), "The folder " + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
                }
            }
            // String fileList[] = getAssets().list("");
            /*for(String fileName : fileList) {
                Toast.makeText(getApplicationContext(), "tess "+ fileName, Toast.LENGTH_SHORT).show();
            }*/
            String fileName  = "eng.traineddata";
            String pathToDataFile = dir + "/" + fileName;
            Toast.makeText(getApplicationContext(),"tess "+dir + "/" + fileName, Toast.LENGTH_SHORT).show();


            InputStream in = getAssets().open(fileName);
            OutputStream out = new FileOutputStream(pathToDataFile);
            byte [] buff = new byte[1024];
            int len ;
            while(( len = in.read(buff)) > 0){
                out.write(buff,0,len);
            }
            in.close();
            out.close();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void startOCR(){
        try{
            //Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
            //imageView.setImageBitmap(bitmap);
            Toast.makeText(getApplicationContext(),"tess 0", Toast.LENGTH_SHORT).show();
            if (OpenCVLoader.initDebug()) {
                Bitmap img = getOcrOfBitmap(bitmap);
                String result = this.getText(img);
                textView.setText(result);
            }


        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private void saveTmpImage(String name, Mat image) {
        Mat img = image.clone();
        if (img.channels() == 3) {
            Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGBA);
        }
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bmp);
        } catch (CvException e) {
            Log.d("mat2bitmap", e.getMessage());
        }
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "MyLibApp/tesscv");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("saveTmpImage", "failed to create directory");
                return;
            }
        }

        File dest = new File(mediaStorageDir.getPath() + File.separator + name + ".png");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dest);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Bitmap getOcrOfBitmap(Bitmap image) {
        if (image == null) {
            return null;
        }

        Mat imgBgra = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
        /*Mat imgBgra = new Mat();
        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, imgBgra);*/

        Utils.bitmapToMat(image, imgBgra);
        Imgproc.cvtColor(imgBgra, imgBgra, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.GaussianBlur(imgBgra, imgBgra, new Size(5, 5), 0);
        //Imgproc.threshold(imgBgra, imgBgra, 0, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);

        //Imgproc.medianBlur(imgBgra,imgBgra, 5);
        //Imgproc.adaptiveThreshold(imgBgra, imgBgra, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 40);
        Imgproc.adaptiveThreshold(imgBgra, imgBgra, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 99,10);
        // Imgproc.threshold(imgBgra, imgBgra, 0, 255, Imgproc.THRESH_OTSU);
       // saveTmpImage("srcInputBitmap", imgBgra);

//        if (img.empty()) {
//            return "";
//        }
//        if (img.channels()==3) {
//            Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
//        }
        saveTmpImage("srcInputBitmap1", imgBgra);
        Utils.matToBitmap(imgBgra, image);

        //Toast.makeText(getApplicationContext(),"tess 1", Toast.LENGTH_SHORT).show();
        return onPhotoTaken();
    }

    protected Bitmap onPhotoTaken() {
        /*BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;*/
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = 6;
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "MyLibApp/tesscv");
        String _path = mediaStorageDir.getPath() + "/srcInputBitmap1.png";
        Bitmap bitmap = BitmapFactory.decodeFile(_path, options);

        //Toast.makeText(getApplicationContext(),"tess 2", Toast.LENGTH_SHORT).show();
        try {
            ExifInterface exif = new ExifInterface(_path);
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Log.v(TAG, "Orient: " + exifOrientation);

            int rotate = 0;

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {

                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            }

            // Convert to ARGB_8888, required by tess
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        } catch (IOException e) {
            Log.e(TAG, "Couldn't correct orientation: " + e.toString());
        }

        // _image.setImageBitmap( bitmap );
        Log.v(TAG, "Before baseApi");
        imageView.setImageBitmap(bitmap);
        return bitmap;
    }

    private String getText(Bitmap bitmap){

        //Toast.makeText(getApplicationContext(),"tess 3", Toast.LENGTH_SHORT).show();
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        String dataPath = getExternalFilesDir("/").getPath() + "/";
        tessBaseAPI.init(dataPath, "eng");
        tessBaseAPI.setImage(bitmap);
        String retStr = "No result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.end();
        return retStr;
    }
}
