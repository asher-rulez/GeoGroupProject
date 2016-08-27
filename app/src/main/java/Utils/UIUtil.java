package Utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.widget.EditText;

import novitskyvitaly.geogroupproject.R;

/**
 * Created by Asher on 19.08.2016.
 */
public class UIUtil {

    public static ProgressDialog ShowProgressDialog(Context context, String message) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle(message);
        progressDialog.show();
        return progressDialog;
    }

    public static void SetEditTextIsValid(Context context, EditText field, boolean isValid) {
        if(isValid)
            field.getBackground()
                    .setColorFilter(ContextCompat.getColor(context, R.color.validation_green_text_color), PorterDuff.Mode.SRC_ATOP);
        else field.getBackground()
                .setColorFilter(ContextCompat.getColor(context, R.color.validation_red_text_color), PorterDuff.Mode.SRC_ATOP);

//        Bitmap validationBitmap = decodeScaledBitmapFromDrawableResource(context.getResources(),
//                isValid ? R.drawable.validation_ok : R.drawable.validation_wrong,
//                context.getResources().getDimensionPixelSize(R.dimen.edittext_validation_img_size),
//                context.getResources().getDimensionPixelSize(R.dimen.edittext_validation_img_size));
//        Drawable validationDrawable = new BitmapDrawable(context.getResources(), validationBitmap);
//        field.setCompoundDrawablesWithIntrinsicBounds(validationDrawable, null, null, null);
//        field.setCompoundDrawablePadding(10);
    }

    public static void RemoveValidationFromEditText(Context context, EditText field) {
        field.getBackground().setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);
        field.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
    }

    public static Bitmap decodeScaledBitmapFromDrawableResource(Resources resources, int drawableID,
                                                                int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, drawableID, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, drawableID, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth){
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
