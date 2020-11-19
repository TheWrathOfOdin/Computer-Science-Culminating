//android imports
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

//java imports
import static java.lang.Math.min;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

//tensorflow imports
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

/** A classifier specialized to label images using TensorFlow Lite. */
public abstract class Classifier {
  public static final String TAG = "ClassifierWithSupport";

  /** The model type used for classification. */
  public enum Model {
    FLOAT_MOBILENET,
    QUANTIZED_MOBILENET,
    FLOAT_EFFICIENTNET,
    QUANTIZED_EFFICIENTNET
  }

  /** The runtime device type used for executing classification. */
  public enum Device {
    CPU,
    NNAPI,
    GPU
  }

  /** Number of results to show in the UI. */
  private static final int MAX_RESULTS = 3;

  /** The loaded TensorFlow Lite model. */

  /** Image size along the x axis. */
  private final int imageSizeX;

  /** Image size along the y axis. */
  private final int imageSizeY;

  /** Optional GPU delegate for accleration. */
  private GpuDelegate gpuDelegate = null;

  /** Optional NNAPI delegate for accleration. */
  private NnApiDelegate nnApiDelegate = null;

  /** An instance of the driver class to run model inference with Tensorflow Lite. */
  protected Interpreter tflite;

  /** Options for configuring the Interpreter. */
  private final Interpreter.Options tfliteOptions = new Interpreter.Options();

  /** Labels corresponding to the output of the vision model. */
  private final List<String> labels;

  /** Input image TensorBuffer. */
  private TensorImage inputImageBuffer;

  /** Output probability TensorBuffer. */
  private final TensorBuffer outputProbabilityBuffer;

  /** Processer to apply post processing of the output probability. */
  private final TensorProcessor probabilityProcessor;

  /**
   * Creates a classifier with the provided configuration.
   *
   * @param activity The current Activity.
   * @param model The model to use for classification.
   * @param device The device to use for classification.
   * @param numThreads The number of threads to use for classification.
   * @return A classifier with the desired configuration.
   */

  public static Classifier create(Activity activity, Model model, Device device, int numThreads)
      throws IOException {
    if (model == Model.QUANTIZED_MOBILENET) {
      return new ClassifierQuantizedMobileNet(activity, device, numThreads);
    }
     else if (model == Model.QUANTIZED_EFFICIENTNET) {
      return new ClassifierQuantizedEfficientNet(activity, device, numThreads);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  /** An immutable result returned by a Classifier describing what was recognized. */
  public static class Recognition {
    //id of image used for the recognition
    private final String id;

    // name of image used for the recognition. //
    private final String title;

    /**
     * A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    private final Float confidence;

    /** Optional location within the source image for the location of the recognized object. */
    private RectF location;

    //Paramaterized Recognition constructor
    //paramters: Image id, Image name, Confidence, Location
    public Recognition(final String id, final String title, final Float confidence, final RectF location) {
      this.id = id;
      this.title = title;
      this.confidence = confidence;
      this.location = location;
    }

    //getter for Id
    public String getId() {
      return id;
    }

    //getter for name
    public String getTitle() {
      return title;
    }

    //getter for confidence
    public Float getConfidence() {
      return confidence;
    }

    //getter for location
    public RectF getLocation() {
      return new RectF(location);
    }

    //setter for location
    public void setLocation(RectF location) {
      this.location = location;
    }

    @Override
    //toString method, returns value of id, title, confidence, and location as a String
    public String toString() {
      String resultString = "";
      if (id != null) {
        resultString += "[" + id + "] ";
      }

      if (title != null) {
        resultString += title + " ";
      }

      if (confidence != null) {
        resultString += String.format("(%.1f%%) ", confidence * 100.0f);
      }

      if (location != null) {
        resultString += location + " ";
      }

      return resultString.trim();
    }
  }

  //Initializes Classifier
  protected Classifier(Activity activity, Device device, int numThreads) throws IOException {
    MappedByteBuffer imageModel = FileUtil.loadMappedFile(activity, getModelPath());
    //prebuilt code, chooses to use CPU, GPU, NNAPI
    switch (device) {
      case NNAPI:
        nnApiDelegate = new NnApiDelegate();
        tfliteOptions.addDelegate(nnApiDelegate);
        break;
      case GPU:
        gpuDelegate = new GpuDelegate();
        tfliteOptions.addDelegate(gpuDelegate);
        break;
      case CPU:
        break;
    }
    tfliteOptions.setNumThreads(numThreads);

    //instantiates Interpreter class with parameters imageModel, and tfliteOption (whether it uses the phone's CPU,GPU, NNAPI)
    tflite = new Interpreter(imageModel, tfliteOptions);

    //Loads labels out from the label file.
    labels = FileUtil.loadLabels(activity, getLabelPath());

    int imageIndex = 0;

    /**
     int[] imageShape is an array uses the TensorFlow Interpreter class to call methods built into that class,
     here we are calling getInputTensor() a method which identifies the shape and type of an image
     **/
    //stores shape of image
    int[] imageShape = tflite.getInputTensor(imageIndex).shape();
    //stores Y value of image in relation to size
    imageSizeY = imageShape[1];
    //stores x value of image in relation to size
    imageSizeX = imageShape[2];

    /**

     **/

    DataType imageDataType = tflite.getInputTensor(imageIndex).dataType();
    int probabilityTensorIndex = 0;
    int[] probabilityShape = tflite.getOutputTensor(probabilityTensorIndex).shape();
    DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

    // Creates the input tensor.
    inputImageBuffer = new TensorImage(imageDataType);

    // Creates the output tensor and its processor.
    outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

    // Creates the post processor for the output probability.
    probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

    Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
  }

  // Runs throguh the entire database (specific prediction list) and returns the classification results.
  public List<Recognition> recognizeImage(final Bitmap bitmap, int sensorOrientation) {
    // Logs this method so that it can be analyzed with systrace.
    Trace.beginSection("recognizeImage");

    //times how long it takes for app to recognize image
    Trace.beginSection("loadImage");
    long startTimeForLoadImage = SystemClock.uptimeMillis();
    inputImageBuffer = loadImage(bitmap, sensorOrientation);
    long endTimeForLoadImage = SystemClock.uptimeMillis();
    Trace.endSection();
    Log.v(TAG, "Timecost to load the image: " + (endTimeForLoadImage - startTimeForLoadImage));

    // Runs the inference (prediction) call.
    Trace.beginSection("runInference");
    long startTimeForReference = SystemClock.uptimeMillis();
    tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
    long endTimeForReference = SystemClock.uptimeMillis();
    Trace.endSection();
    Log.v(TAG, "Timecost to run model inference: " + (endTimeForReference - startTimeForReference));

    // Gets the map of label and probability.
    Map<String, Float> labeledProbability = new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer)).getMapWithFloatValue();
    Trace.endSection();

    // Gets top results.
    return getTopKProbability(labeledProbability);
  }

  //Closes the interpreter and model
  public void close() {
    if (tflite != null) {
      tflite.close();
      tflite = null;
    }
    if (gpuDelegate != null) {
      gpuDelegate.close();
      gpuDelegate = null;
    }
    if (nnApiDelegate != null) {
      nnApiDelegate.close();
      nnApiDelegate = null;
    }
  }

  //getter for size of the image along the x axis
  public int getImageSizeX() {
    return imageSizeX;
  }

  //getter for size of the image along the x axis
  public int getImageSizeY() {
    return imageSizeY;
  }

  //loads the input image
  private TensorImage loadImage(final Bitmap bitmap, int sensorOrientation) {
    // Loads bitmap into a TensorImage.
    inputImageBuffer.load(bitmap);

    // Creates processor for the TensorImage.
    int cropSize = min(bitmap.getWidth(), bitmap.getHeight());
    int numRotation = sensorOrientation / 90;
    // TODO(b/143564309): Fuse ops inside ImageProcessor.
    ImageProcessor imageProcessor = new ImageProcessor.Builder().add(new ResizeWithCropOrPadOp(cropSize, cropSize)).add(new ResizeOp(imageSizeX, imageSizeY, ResizeMethod.NEAREST_NEIGHBOR)).add(new Rot90Op(numRotation)).add(getPreprocessNormalizeOp()).build();
    return imageProcessor.process(inputImageBuffer);
  }

  //Gets the top results.
  private static List<Recognition> getTopKProbability(Map<String, Float> labelProb) {
    // Find the best classifications.
    PriorityQueue<Recognition> pq =
        new PriorityQueue<>(
            MAX_RESULTS,
            new Comparator<Recognition>() {
              @Override
              public int compare(Recognition lhs, Recognition rhs) {
                // Intentionally reversed to put high confidence at the head of the queue.
                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
              }
            });

    //cycles only through image id's
    for (Map.Entry<String, Float> entry : labelProb.entrySet()) {
      pq.add(new Recognition("" + entry.getKey(), entry.getKey(), entry.getValue(), null));
    }

    //cycles through model database and stores top 5 recognitions in final List
    final ArrayList<Recognition> recognitions = new ArrayList<>();
    int recognitionsSize = min(pq.size(), MAX_RESULTS);
    for (int i = 0; i < recognitionsSize; i++) {
      recognitions.add(pq.poll());
    }
    return recognitions;
  }

  //gets the name of the model file in the assets folder
  protected abstract String getModelPath();

  //Gets the name of the label file in the assets folder
  protected abstract String getLabelPath();

  //Gets the TensorOperator
  protected abstract TensorOperator getPreprocessNormalizeOp();
  protected abstract TensorOperator getPostprocessNormalizeOp();
}
