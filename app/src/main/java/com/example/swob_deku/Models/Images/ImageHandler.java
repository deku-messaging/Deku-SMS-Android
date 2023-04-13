package com.example.swob_deku.Models.Images;

import static com.example.swob_deku.Models.SMS.SMSHandler.ASCII_MAGIC_NUMBER;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.DataHelper;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityAES;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ImageHandler {

    Uri imageUri;
    Context context;
    public Bitmap bitmap;

    String iv = "1234567890123456";
    String secretKey = "12345678901234561234567890123456";

    public static final String IMAGE_HEADER = "--DEKU_IMAGE_HEADER--";
    public static final String IMAGE_TRANSMISSION_HEADER = "[[DTH";
    static final int MAX_NUMBER_SMS = 39;

    boolean edited = false;

    public ImageHandler(Context context, Uri imageUri) throws IOException {
        this.imageUri = imageUri;
        this.context = context;
        this.bitmap = MediaStore.Images.Media.getBitmap(this.context.getContentResolver(), this.imageUri);
    }

    public boolean isEdited() {
        return this.edited;
    }

    public static byte[] buildImage(byte[][] unstructuredImageBytes ) throws IOException {
        return SMSHandler.rebuildStructuredSMSMessage(unstructuredImageBytes);
    }

    public static boolean canComposeImage(Context context, String RIL) {
        Cursor cursor = getImagesCursor(context, RIL);
        boolean canCompose = false;
        if(cursor != null) {
            canCompose = true;
            cursor.close();
        }
        return canCompose;
    }

    public byte[] compressImage(int compressionRatio, Bitmap.CompressFormat compressFormat) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        this.bitmap.compress(compressFormat, compressionRatio, byteArrayOutputStream);

        byte[] compressedBitmapBytes = byteArrayOutputStream.toByteArray();
        return compressedBitmapBytes;
    }

    public byte[] compressImage(int compressionRatio) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Bitmap.CompressFormat bitmapCompressionFormat = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ?
                Bitmap.CompressFormat.WEBP_LOSSY : Bitmap.CompressFormat.WEBP;

        this.bitmap.compress(bitmapCompressionFormat, compressionRatio, byteArrayOutputStream);

        byte[] compressedBitmapBytes = byteArrayOutputStream.toByteArray();
        return compressedBitmapBytes;
    }

    public byte[] compressImage(int compressionRatio, Bitmap imageBitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Bitmap.CompressFormat bitmapCompressionFormat = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ?
                Bitmap.CompressFormat.WEBP_LOSSY : Bitmap.CompressFormat.WEBP;

        imageBitmap.compress(bitmapCompressionFormat, compressionRatio, byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    public static long composeAppendDeleteImages(Context context, String RIL, int ref) throws IOException {
        Cursor cursor = getImagesCursor(context, RIL);

        String defaultRIL = IMAGE_HEADER + ref;

        Cursor headerCursor = getImagesCursorAtIndex(context, RIL , 0);
        int len = 0;
        String headerMessageId = "";

        if(headerCursor != null) {
            SMS sms = new SMS(headerCursor);
            headerMessageId = sms.getId();

            String body = sms.getBody().replace(defaultRIL + 0, "");
            Log.d(ImageHandler.class.getName(), "Data image compose first body: " + body);
            len = Byte.toUnsignedInt(Base64.decode(body, Base64.DEFAULT)[2]);
            headerCursor.close();
        }
        Log.d(ImageHandler.class.getName(), "Data image compose len: " + len);
        byte[][] imageData = new byte[len][];

        String[] ids = new String[len];
        if(cursor.moveToFirst()) {
            do {
                // remove header
                SMS sms = new SMS(cursor);
                Log.d(ImageHandler.class.getName(), "Data image compose raw: " + sms.getBody());

                String body = sms.getBody();
                if(sms.getBody().contains(defaultRIL + "0" + len)) {
                    body = body.replace(defaultRIL + "0" + len, "");
                }
                else {
                    for (int i = len -1; i > -1; --i) {
                        if(sms.getBody().contains(defaultRIL + i)) {
                            body = sms.getBody().replace(defaultRIL + i, "");
                            break;
                        }
                    }
                }

                Log.d(ImageHandler.class.getName(), "Data image compose formatted: " + body);

                byte[] data = Base64.decode(body, Base64.DEFAULT);
                int index = Byte.toUnsignedInt(data[1]);

                Log.d(ImageHandler.class.getName(), "Data image compose index: " + index);
                Log.d(ImageHandler.class.getName(), "Data image compose data: " + data);

                imageData[index] = data;

                if(index != 0 )
                    ids[index] = sms.getId();

            } while(cursor.moveToNext());
        }
        cursor.close();
        byte[] imageBytes = buildImage(imageData);

        Bitmap bitmap = getImageFromBytes(imageBytes);

        if(bitmap == null)
            Log.d(ImageHandler.class.getName(), "Data image is not a real image!");
        else
            Log.d(ImageHandler.class.getName(), "Data image is a real image!");

        String appendedBody = IMAGE_HEADER + Base64.encodeToString(imageBytes, Base64.DEFAULT);

        SMSHandler.updateMessage(context, headerMessageId, appendedBody);
        SMSHandler.deleteSMSMessagesById(context, ids);

        return Long.parseLong(headerMessageId);
    }

    public byte[] encryptImage(byte[] imageBytes) throws Throwable {
        SecurityAES aes = new SecurityAES();

        byte[] bytesJpegEncryption = aes.encrypt( imageBytes, secretKey.getBytes(StandardCharsets.UTF_8));

        return bytesJpegEncryption;
    }

    public static String getImageMetaRIL(byte[] data) {
        return String.valueOf(Byte.toUnsignedInt(data[0])) + String.valueOf(Byte.toUnsignedInt(data[1]));
    }

    public static Cursor getImagesCursor(Context context, String RIL) {
        RIL = IMAGE_HEADER + RIL;

        Cursor cursorImageCursor = SMSHandler.fetchSMSForImagesByRIL(context, RIL);
        Log.d(ImageHandler.class.getName(), "Data image header RIL: " + RIL);
        Log.d(ImageHandler.class.getName(), "Data image header counter: " + cursorImageCursor.getCount());
        if(cursorImageCursor.moveToFirst()) {

            SMS sms = new SMS(cursorImageCursor);
            cursorImageCursor.close();

            String body = sms.getBody().replace(RIL, "");

            byte[] data = Base64.decode(body, Base64.DEFAULT);

            Log.d(ImageHandler.class.getName(), "Data image ref: " + Byte.toUnsignedInt(data[0]));
            // TODO: check if data is image
            int len = Byte.toUnsignedInt(data[2]);

            StringBuilder query = new StringBuilder();
            String[] parameters = new String[len];

            for(Integer i=0; i<len; ++i ) {
                if(i + 1 == len)
                    query.append(Telephony.TextBasedSmsColumns.BODY + " like ?");
                else
                    query.append(Telephony.TextBasedSmsColumns.BODY + " like ? OR ");

                parameters[i] = IMAGE_HEADER + Byte.toUnsignedInt(data[0]) + i + "%";
            }

            Cursor cursor = SMSHandler.fetchSMSForImages(context, query.toString(), parameters, sms.getThreadId());
            Log.d(ImageHandler.class.getName(), "Data image founder counter: " + cursor.getCount() + "/" + len);
            if(cursor.getCount() >= len) {
                return cursor;
            }
        }
        return null;
    }



    public static Cursor getImagesCursorAtIndex(Context context, String RIL, int index) {
        RIL = IMAGE_HEADER + RIL;

        Log.d(ImageHandler.class.getName(), "Data image compose first index: " + RIL);

        Cursor cursorImageCursor = SMSHandler.fetchSMSForImagesByRIL(context, RIL);
        if(cursorImageCursor.moveToFirst()) {
            return cursorImageCursor;
        }
        return null;
    }

    public static Bitmap getImageFromBytes(byte[] bytes) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public int[] getDimension(int width, int height, double ratio) {
        double wr = (double) width / height;
        double hr = (double) height / width;

        if(wr > hr) {
            width = (int) Math.round(ratio);
            height = (int) Math.round(ratio * hr);
        }
        else if(hr > wr) {
            height = (int) Math.round(ratio);
            width = (int) Math.round(ratio * wr);
        }
        else {
            width = (int) Math.round(ratio);
            height = (int) Math.round(ratio);
        }

        return new int[]{width, height};
    }

    public static boolean isImageBody(byte[] data) {
        /**
         * 0 = Reference ID
         * 1 = Message ID
         * 2 = Total number of messages
         */
        return data.length > 2
                && Byte.toUnsignedInt(data[0]) >= ASCII_MAGIC_NUMBER
                && Byte.toUnsignedInt(data[1]) >= 0;
    }

    private boolean isImageHeader(SMS sms) {
        byte[] data = Base64.decode(sms.getBody(), Base64.DEFAULT);

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        return bitmap != null || (data.length > 3
                && Byte.toUnsignedInt(data[0]) >= ASCII_MAGIC_NUMBER
                && Byte.toUnsignedInt(data[1]) >= 0 && Byte.toUnsignedInt(data[2]) <= MAX_NUMBER_SMS);
    }

    public Bitmap resizeImage(double resValue) throws IOException {
        // use ratios for compressions rather than just raw values
        Log.d(getClass().getName(), "Resizing value: " + resValue);
        if(this.bitmap.getWidth() < resValue && this.bitmap.getHeight() < resValue)
            return this.bitmap;

        int[] dimensions = getDimension(this.bitmap.getWidth(), this.bitmap.getHeight(), resValue);
        int width = dimensions[0];
        int height = dimensions[1];


        this.edited = true;
        return Bitmap.createScaledBitmap(this.bitmap, width, height, true);
    }

    public int getMaxResolution() {
        return Math.max(this.bitmap.getHeight(), this.bitmap.getWidth());
    }

    public double shannonEntropy(byte[] values) {
        Map<Byte, Integer> map = new HashMap<Byte, Integer>();
        // count the occurrences of each value
        for (Byte sequence : values) {
            if (!map.containsKey(sequence)) {
                map.put(sequence, 0);
            }
            map.put(sequence, map.get(sequence) + 1);
        }

        // calculate the entropy
        double result = 0.0;
        for (Byte sequence : map.keySet()) {
            double frequency = (double) map.get(sequence) / values.length;
            result -= frequency * (Math.log(frequency) / Math.log(2));
        }

        return result;
    }
}
