package pt.continente.review.common;

import pt.continente.review.R;
import pt.continente.review.tables.ArticlesTable;
import pt.continente.review.tables.DimensionsTable;
import pt.continente.review.tables.ReviewDimensionsTable;
import pt.continente.review.tables.ReviewImagesTable;
import pt.continente.review.tables.ReviewsTable;
import pt.continente.review.tables.SQLiteHelper;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {
	private static final String TAG = "CntRev - Preferences";
	
	Context context;
	
	public Preferences() {
		Common.log(5, TAG, "Preferences: started");
		context = this;
	}

	@SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//FIXME fix deprecated
		addPreferencesFromResource(R.xml.prefs);

		/*
		 * EMAIL CHECK
		 */
		//TODO fix deprecated
		Preference userEmail = findPreference("userEmail");
		if (userEmail != null)
			userEmail.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference pref, Object obj) {
					if(Common.isEmailValid((String) obj) || ((String) obj).equals("")) {
						//Common.shortToast(context, "email is valid");
						return true;
					}
					Common.longToast(context, getResources().getString(R.string.prefs_userEmail_invalid));
					return false;
				}
			});

		/*
		 * CLEAR DB CLICK
		 */
		//TODO fix deprecated
		Preference clearDB = findPreference("clearDB");
		if (clearDB != null)
			clearDB.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					Common.log(5, TAG, "onCreate: onPreferenceClick: will process clearDB click");
					AlertDialog.Builder alert = new AlertDialog.Builder(context);
					alert.setTitle(getResources().getString(R.string.dialog_prefsDeleteReviewsTitle));
					alert.setMessage(getResources().getString(R.string.dialog_prefsDeleteReviewsMessage));
					alert.setPositiveButton(getResources().getString(R.string.button_generalDelete), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Common.log(5, TAG, "onCreate: onPreferenceClick: will clear DB");
							limparBD();
						}
					});
					alert.setNegativeButton(getResources().getString(R.string.button_generalReturn), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
						}
					});
					alert.show();
			        return true;
				}
			});

		/*
		 * FEEDBACK CLICK
		 */
		//TODO fix deprecated
		Preference feedback = findPreference("feedback");
		if (feedback != null)
			feedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					Common.sendAppReviewByEmail(context);
			        return true;
				}
			});
		
		
		/*
		 * APP VERSION CLICK
		 */
		//TODO fix deprecated
		Preference appVersion = findPreference("appVersion");
		if (appVersion != null)
			appVersion.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					Common.log(5, TAG, "onCreate: onPreferenceClick: will process appVersion click");
					AlertDialog.Builder alert = new AlertDialog.Builder(context);
					alert.setTitle(getResources().getString(R.string.prefs_appVersion));
					alert.setMessage("Vers�o n�mero: " + getResources().getString(R.string.app_versionName));
					alert.setNegativeButton(getResources().getString(R.string.button_generalReturn), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
						}
					});
					alert.show();
			        return true;
				}
			});

		/*
		 * APP CREATORS CLICK
		 */
		//TODO fix deprecated
		Preference appCreators = findPreference("appCreators");
		if (appCreators != null)
			appCreators.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					Common.log(5, TAG, "onCreate: onPreferenceClick: will process appVersion click");
					AlertDialog.Builder alert = new AlertDialog.Builder(context);
					alert.setTitle(getResources().getString(R.string.prefs_appCreators));
					alert.setMessage(getResources().getString(R.string.preds_appCreatorsContent));
					alert.setNegativeButton(getResources().getString(R.string.button_generalReturn), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
						}
					});
					alert.show();
			        return true;
				}
			});
	}

    public void limparBD() {
    	SQLiteHelper dbHelper = new SQLiteHelper(this);
    	ArticlesTable artTab;
    	ReviewsTable revTab;
    	ReviewImagesTable revImgTab;
    	DimensionsTable dimTab;
    	ReviewDimensionsTable revDimTab;
    	try {
			artTab = new ArticlesTable(dbHelper);
			artTab.open();
			revTab = new ReviewsTable(dbHelper);
			revTab.open();
			revImgTab = new ReviewImagesTable(dbHelper);
			revImgTab.open();
			dimTab = new DimensionsTable(dbHelper);
			dimTab.open();
			revDimTab = new ReviewDimensionsTable(dbHelper);
			revDimTab.open();
		} catch (Exception e) {
			Common.log(1, TAG, "criarArtigosTeste: could not open the table - " + e.getMessage());
			return;
		}
    	artTab.deleteAllItems();
    	revTab.deleteAllItems();
    	revImgTab.deleteAllItems();
    	dimTab.deleteAllItems();
    	revDimTab.deleteAllItems();
    	
    	artTab.close();
    	revTab.close();
    	revImgTab.close();
    	dimTab.close();
    	revDimTab.close();
    }

}
