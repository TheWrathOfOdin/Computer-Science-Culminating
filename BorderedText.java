//android imports
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
//java import
import java.util.Vector;

//this class allows us to output our information to the UI
public class BorderedText {
  private final Paint interiorPaint;
  private final Paint exteriorPaint;

  private final float textSize;

  
  public BorderedText(final float textSize) {
    this(Color.WHITE, Color.BLACK, textSize);
  }
  
  //this method sets the size, interior and exterior for the text 
  public BorderedText(final int interiorColor, final int exteriorColor, final float textSize) {
    interiorPaint = new Paint();
    interiorPaint.setTextSize(textSize);
    interiorPaint.setColor(interiorColor);
    interiorPaint.setStyle(Style.FILL);
    interiorPaint.setAntiAlias(false);
    interiorPaint.setAlpha(255);

    exteriorPaint = new Paint();
    exteriorPaint.setTextSize(textSize);
    exteriorPaint.setColor(exteriorColor);
    exteriorPaint.setStyle(Style.FILL_AND_STROKE);
    exteriorPaint.setStrokeWidth(textSize / 8);
    exteriorPaint.setAntiAlias(false);
    exteriorPaint.setAlpha(255);

    this.textSize = textSize;
  }

  public void setTypeface(Typeface typeface) {
    interiorPaint.setTypeface(typeface);
    exteriorPaint.setTypeface(typeface);
  }
  
  //here we draw the text onto the actual canvas (our UI)
  public void drawText(final Canvas canvas, final float posX, final float posY, final String text) {
    canvas.drawText(text, posX, posY, exteriorPaint);
    canvas.drawText(text, posX, posY, interiorPaint);
  }
  
  //setter for interiorColor
  public void setInteriorColor(final int color) {
    interiorPaint.setColor(color);
  }
  //setter for exteriorColor
  public void setExteriorColor(final int color) {
    exteriorPaint.setColor(color);
  }

  //getter for textSize
  public float getTextSize() {
    return textSize;
  }

  //setter for alpha
  public void setAlpha(final int alpha) {
    interiorPaint.setAlpha(alpha);
    exteriorPaint.setAlpha(alpha);
  }
    
  //aligning our text
  public void setTextAlign(final Align align) {
    interiorPaint.setTextAlign(align);
    exteriorPaint.setTextAlign(align);
  }
}
