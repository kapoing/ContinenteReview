package pt.continente.review.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import pt.continente.review.R;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

public class Common {
	//private static final String TAG = "CntRev - Common";
	
	/*
	 * CONFIGURATION VARIABLES
	 */
	private static final int LOG_LEVEL = 7;
	public static final String bugSenseAppKey = "6804ac88";
	// END OF CONFIGURATION
	
	public static final class revStates {
		public static final int PENDING_USER = 1;
		public static final int PENDING_SYSTEM = 2;
		public static final int WORK_IN_PROGRESS = 5;
		public static final int COMPLETED = 10;
		public static final int SUBMITED = 15;
	}
	
	public static final class httpVariables {
		private static final String SERVER_IP = "195.170.168.33";
		public static final String IMAGE_PREFIX = "http://www.continente.pt/Images/media/Products/";
		public static final String ARTICLE_PREFIX = "http://" + SERVER_IP + "/ContinenteReview/article.php?ean=";
		public static final String DIMENSIONS_PREFIX = "http://" + SERVER_IP + "/ContinenteReview/dimensions.php?article_id=";
		public static final String REVIEW_PREFIX = "http://" + SERVER_IP + "/ContinenteReview/review.php";
	}
	
	public static void log(int debugLevel, String tag, String msg) {
		if (debugLevel <= LOG_LEVEL)
			Log.i(tag, msg);
	}

	public static void shortToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
	}
	
	public static void longToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
	}
	
	public static int pixelsFromDPs(Context c, int DPs) {
	    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DPs, c.getResources().getDisplayMetrics());
	}

	public static byte[] imageToBlob(Bitmap image) {
		if(image == null)
			return null;
		byte[] blob = null;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.PNG, 100, stream);
/*		boolean success = image.compress(Bitmap.CompressFormat.PNG, 100, stream);
		Common.log(5, TAG, "imageToBlob: image compression " + (success ? "was" : "WAS NOT") + " a success");
		if (!success) {
			Bitmap cloneImg = Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), false); 
			stream = new ByteArrayOutputStream();
			cloneImg.compress(Bitmap.CompressFormat.PNG, 100, stream);
		}
*/		blob = stream.toByteArray();
		return blob;
	}

	public static Bitmap blobToImage(byte[] blob) {
		if(blob == null)
			return null;
		Bitmap image = null;
    	ByteArrayInputStream imageStream = new ByteArrayInputStream(blob);
    	image = BitmapFactory.decodeStream(imageStream);
		return image;
	}

	public static boolean isNetworkConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null)
			return false;
		else
			return true;
	}

	public static boolean isEmailValid(CharSequence email) {
		return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
	}

	public static void sendAppReviewByEmail(Context c) {
		/* Create the Intent */
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

		/* Fill it with Data */
		Resources resources = c.getResources();
		emailIntent.setType("plain/text");
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{resources.getString(R.string.dialog_feedbackEmailAddress)});
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, resources.getString(R.string.dialog_feedbackSubject) + " (v" + resources.getString(R.string.app_versionName) + ")");
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, resources.getString(R.string.dialog_feedbackBody));

		/* Send it off to the Activity-Chooser */
		c.startActivity(Intent.createChooser(emailIntent, "Enviar email..."));
	}
}
