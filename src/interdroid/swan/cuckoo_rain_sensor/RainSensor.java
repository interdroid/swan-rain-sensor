package interdroid.swan.cuckoo_rain_sensor;

import interdroid.swan.cuckoo_rain_sensor.R;

import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractCuckooSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy; // link to android library: vdb-avro

import android.content.ContentValues;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import interdroid.swan.cuckoo_sensors.CuckooPoller;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.google.android.gms.gcm.GoogleCloudMessaging; // link to android library: google-play-services_lib

/**
* A sensor for expected rain in the Netherlands
*
* @author roelof &lt;rkemp@cs.vu.nl&gt;
*
*/
public class RainSensor extends AbstractCuckooSensor {

	/**
	* The configuration activity for this sensor.
	*/
	public static class ConfigurationActivity
		extends AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.rain_preferences;
		}

	}

	/**
	* The lat configuration.
	*/
	public static final String LAT_CONFIG = "lat";

	/**
	* The lon configuration.
	*/
	public static final String LON_CONFIG = "lon";

	/**
	* The window configuration.
	*/
	public static final String WINDOW_CONFIG = "window";

	/**
	* The expected field.
	*/
	public static final String EXPECTED_FIELD = "expected";

	/**
	* The schema for this sensor.
	*/
	public static final String SCHEME = getSchema();

	/**
	* The provider for this sensor.
	*/
	public static class Provider extends AvroContentProviderProxy {

		/**
		* Construct the provider for this sensor.
		*/
		public Provider() {
			super(SCHEME);
		}

	}

	/**
	* @return the schema for this sensor.
	*/
	private static String getSchema() {
		String scheme =
			"{'type': 'record', 'name': 'rain', "
			+ "'namespace': 'interdroid.swan.cuckoo_rain_sensor.rain',"
			+ "\n'fields': ["
			+ SCHEMA_TIMESTAMP_FIELDS
			+ "\n{'name': '"
			+ EXPECTED_FIELD
			+ "', 'type': 'int'}"
			+ "\n]"
			+ "}";
		return scheme.replace('\'', '"');
	}

	@Override
	public final String[] getValuePaths() {
		return new String[] { EXPECTED_FIELD };
	}

	@Override
	public void initDefaultConfiguration(final Bundle defaults) {
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}


	/**
	* Data Storage Helper Method.
	* @param expected value for expected
	*/
	private void storeReading(int expected) {
		long now = System.currentTimeMillis();
		ContentValues values = new ContentValues();
		values.put(EXPECTED_FIELD, expected);
		putValues(values, now);
	}

	/**
	* =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	* Sensor Specific Implementation
	* =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	*/

	@Override
	public final CuckooPoller getPoller() {
		return new RainPoller();
	}

	@Override
	public String getGCMSenderId() {
		throw new java.lang.RuntimeException("<put your gcm project id here>");
	}

	@Override
	public String getGCMApiKey() {
		throw new java.lang.RuntimeException("<put your gcm api key here>");
	}

	public void registerReceiver() {
		IntentFilter filter = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
		filter.addCategory(getPackageName());
		registerReceiver(new BroadcastReceiver() {
			private static final String TAG = "rainSensorReceiver";

			@Override
			public void onReceive(Context context, Intent intent) {
				GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
				String messageType = gcm.getMessageType(intent);
				if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
						.equals(messageType)) {
					Log.d(TAG, "Received update but encountered send error.");
				} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
						.equals(messageType)) {
					Log.d(TAG, "Messages were deleted at the server.");
				} else {
					if (intent.hasExtra(EXPECTED_FIELD)) {
						storeReading(intent.getExtras().getInt("expected"));
					}
				}
				setResultCode(Activity.RESULT_OK);
			}
	}, filter, "com.google.android.c2dm.permission.SEND", null);
	}
}