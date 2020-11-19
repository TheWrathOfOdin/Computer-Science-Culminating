//Android imports
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

//Java imports
import java.io.IOException;
import java.util.List;

//Tensorflow imports
import org.tensorflow.lite.examples.classification.env.BorderedText;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;


public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  //variable declerations
  private static final Logger LOGGER = new Logger();
  private static final Size previewSize = new Size(640, 480);
  private static final float textSize = 10;
  private Bitmap rgbFrameBitmap = null;
  private long processingTime;
  private Integer sensorOrientation;
  private Classifier classifier;
  private BorderedText borderedText;
  private int imageSizeX;
  private int imageSizeY;

  @Override
  //getter for LayoutId, LayoutId is defined in the CameraActivity Class
  protected int getLayoutId() {
    return R.layout.tfe_ic_camera_connection_fragment;
  }

  @Override
  //getter for LayoutId, LayoutId is defined in the CameraActivity Class
  protected Size getDesiredPreviewFrameSize() {
    return previewSize;
  }

  @Override
  //getter for LayoutId, LayoutId is defined in the CameraActivity Class
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, textSize, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    recreateClassifier(getModel(), getDevice(), getNumThreads());
    if (classifier == null) {
      LOGGER.e("No classifier on preview!");
      return;
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
  }

  @Override
  //getter for processImage, processImage is defined in the CameraActivity Class
  protected void processImage() {
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
    //stores height and width of the preview
    final int cropSize = Math.min(previewWidth, previewHeight);

    runInBackground(
        new Runnable() {
          @Override
          //times how long classifier took to output prediction 
          public void run() {
            if (classifier != null) {
              final long startTime = SystemClock.uptimeMillis();
              final List<Classifier.Recognition> results =
                      classifier.recognizeImage(rgbFrameBitmap, sensorOrientation);
              processingTime = SystemClock.uptimeMillis() - startTime;
              LOGGER.v("Detect: %s", results);

              //visual component, outputs all info below into the UI
              runOnUiThread(
                      new Runnable() {
                        @Override
                        public void run() {
                          showResultsInBottomSheet(results);
                          showFrameInfo(previewWidth + "x" + previewHeight);
                          showCropInfo(imageSizeX + "x" + imageSizeY);
                          showCameraResolution(cropSize + "x" + cropSize);
                          showRotationInfo(String.valueOf(sensorOrientation));
                          showInference(processingTime + "ms");
                        }
                      });
            }
            readyForNextImage();
          }
        });
  }

  @Override
  //getter for LayoutId, LayoutId is defined in the CameraActivity Class
  protected void onInferenceConfigurationChanged() {

    //if there is so camera frame avaliable do nothing
    if (rgbFrameBitmap == null) {
      return;
    }
    //stores the device we're working on
    final Device device = getDevice();
    //stores model number of device we're working on
    final Model model = getModel();
    //stores how many cpu threads are avaliable for use
    final int numThreads = getNumThreads();
  }

  private void recreateClassifier(Model initModel, Device initDevice, int numThreads) {
    //clearing the classifier
    if (classifier != null) {
      LOGGER.d("Closing classifier.");
      classifier.close();
      classifier = null;
    }
    //catches error where classifier is not supported by GPU (prewrote code from the tesnsowflow library, the app doesn't work without this snippet)
    if (initDevice == Device.GPU
        && (initModel == Model.QUANTIZED_MOBILENET || initModel == Model.QUANTIZED_EFFICIENTNET)) {
      LOGGER.d("Not creating classifier: GPU doesn't support quantized models.");
      runOnUiThread(
          () -> {
            Toast.makeText(this, R.string.tfe_ic_gpu_quant_error, Toast.LENGTH_LONG).show();
          });
      return;
    }

    //try catch to create and display the classifer
    try {
      LOGGER.d("Creating classifier (model=%s, device=%s, numThreads=%d)", initModel, initDevice, numThreads);
      classifier = Classifier.create(this, initModel, initDevice, numThreads);
    }
    //catches error where classifier is not created
    catch (IOException | IllegalArgumentException e) {
      LOGGER.e(e, "Could not create Classifier.");
      return;
    }

    // Updates the input image size as the camera moves around
    imageSizeX = classifier.getImageSizeX();
    imageSizeY = classifier.getImageSizeY();
  }
}
