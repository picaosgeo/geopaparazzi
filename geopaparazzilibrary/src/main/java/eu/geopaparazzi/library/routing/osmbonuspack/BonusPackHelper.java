/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.geopaparazzi.library.routing.osmbonuspack;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import eu.geopaparazzi.library.R;

/**
 * Useful functions and common constants.
 *
 * @author M.Kergall
 */
public class BonusPackHelper {

    /**
     * Log tag.
     */
    public static final String LOG_TAG = "BONUSPACK";

    /**
     * resource id value meaning "undefined resource id"
     */
    public static final int UNDEFINED_RES_ID = 0;

    /**
     * User agent sent to services by default
     */
    public static final String DEFAULT_USER_AGENT = "OsmBonusPack/1";

    /**
     * @return true if the device is the emulator, false if actual device.
     */
    public static boolean isEmulator() {
        //return Build.MANUFACTURER.equals("unknown");
        return ("google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT));
    }

    public static BoundingBoxE6 cloneBoundingBoxE6(BoundingBoxE6 bb) {
        return new BoundingBoxE6(
                bb.getLatNorthE6(),
                bb.getLonEastE6(),
                bb.getLatSouthE6(),
                bb.getLonWestE6());
    }

    /**
     * @return the BoundingBox enclosing bb1 and bb2 BoundingBoxes
     */
    public static BoundingBoxE6 concatBoundingBoxE6(BoundingBoxE6 bb1, BoundingBoxE6 bb2) {
        return new BoundingBoxE6(
                Math.max(bb1.getLatNorthE6(), bb2.getLatNorthE6()),
                Math.max(bb1.getLonEastE6(), bb2.getLonEastE6()),
                Math.min(bb1.getLatSouthE6(), bb2.getLatSouthE6()),
                Math.min(bb1.getLonWestE6(), bb2.getLonWestE6()));
    }

    /**
     * @return the whole content of the http request, as a string
     */
    private static String readStream(HttpConnection connection) {
        String result = connection.getContentAsString();
        return result;
    }

    /**
     * sends an http request, and returns the whole content result in a String.
     *
     * @param url
     * @return the whole content, or null if any issue.
     */
    public static String requestStringFromUrl(Context context, String url) throws IOException {
        HttpConnection connection = new HttpConnection();
        int status = connection.doGet(url);
        if (status != HttpStatus.SC_OK) {
            if (status == HttpStatus.SC_UNAUTHORIZED || status == HttpStatus.SC_FORBIDDEN) {
                throw new IOException(context.getString(R.string.unauthorized_api_key));
            }
        }
        String result = readStream(connection);
        connection.close();
        return result;
    }

    /**
     * requestStringFromPost: do a post request to a url with name-value pairs,
     * and returns the whole content result in a String.
     *
     * @param url
     * @param nameValuePairs
     * @return the content, or null if any issue.
     */
    public static String requestStringFromPost(String url, List<NameValuePair> nameValuePairs) {
        HttpConnection connection = new HttpConnection();
        connection.doPost(url, nameValuePairs);
        String result = readStream(connection);
        connection.close();
        return result;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Loads a bitmap from a url.
     *
     * @param url
     * @return the bitmap, or null if any issue.
     */
    public static Bitmap loadBitmap(String url) {
        Bitmap bitmap = null;
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            if (is == null)
                return null;
            bitmap = BitmapFactory.decodeStream(new FlushedInputStream(is));
            //Alternative providing better handling on loading errors?
            /*
            Drawable d = Drawable.createFromStream(new FlushedInputStream(is), null);
			if (is != null)
				is.close();
			if (d != null)
				bitmap = ((BitmapDrawable)d).getBitmap();
			*/
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**
     * Workaround on Android issue on bitmap loading
     *
     * @see <a href="http://stackoverflow.com/questions/4601352/createfromstream-in-android-returning-null-for-certain-url">Issue</a>
     */
    static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int byteValue = read();
                    if (byteValue < 0) {
                        break;  // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    /**
     * Parse a string-array resource with items like this: <item>key|value</item>
     *
     * @param ctx
     * @param stringArrayResourceId
     * @return the keys=>values as an HashMap
     */
    public static HashMap<String, String> parseStringMapResource(Context ctx, int stringArrayResourceId) {
        String[] stringArray = ctx.getResources().getStringArray(stringArrayResourceId);
        HashMap<String, String> map = new HashMap<>(stringArray.length);
        for (String entry : stringArray) {
            String[] splitResult = entry.split("\\|", 2);
            map.put(splitResult[0], splitResult[1]);
        }
        return map;
    }
}
