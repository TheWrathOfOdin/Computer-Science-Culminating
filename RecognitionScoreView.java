//android imports
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
//java imports
import java.util.List;
//Tensorflow imports
import org.tensorflow.lite.examples.classification.tflite.Classifier.Recognition;

//This class controls the output for the RecognitionScore otherwise known as the confidence or inference
public class RecognitionScoreView extends View implements ResultsView {
  private static final float TEXT_SIZE_DIP = 16;
  private final float textSizePx;
  private final Paint fgPaint;
  private final Paint bgPaint;
  private List<Recognition> results;

  public RecognitionScoreView(final Context context, final AttributeSet set) {
    //creates a custom "view" in the app
    super(context, set);

    //UI components
    textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    fgPaint = new Paint();
    fgPaint.setTextSize(textSizePx);

    //color of view
    bgPaint = new Paint();
    bgPaint.setColor(0xcc4285f4);
  }

  @Override
  //after the recognition results are displayed, the view is redrawn as soon as possible
  public void setResults(final List<Recognition> results) {
    this.results = results;
    //tells app to redraw view
    postInvalidate();
  }

  @Override
  //draws the canvas
  public void onDraw(final Canvas canvas) {
    final int x = 10;
    int y = (int) (fgPaint.getTextSize() * 1.5f);

    canvas.drawPaint(bgPaint);

    //once there are some recognition results they start to be outputted into the UI
    if (results != null) {
      for (final Recognition recog : results) {
        canvas.drawText(recog.getTitle() + ": " + recog.getConfidence(), x, y, fgPaint);
        y += (int) (fgPaint.getTextSize() * 1.5f);
      }
    }
  }
}
