package com.afkanerd.deku.Images.Images;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.afkanerd.deku.E2EE.Security.SecurityAES;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
//        return SMSHandler.rebuildStructuredSMSMessage(unstructuredImageBytes);
        return null;
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
//        SMSHandler.deleteMultipleMessages(context, ids);

        return Long.parseLong(headerMessageId);
    }

    public byte[] encryptImage(byte[] imageBytes) throws Throwable {
        SecurityAES aes = new SecurityAES();

        byte[] bytesJpegEncryption = aes.encrypt_256_cbc( imageBytes, secretKey.getBytes(StandardCharsets.UTF_8), null);

        return bytesJpegEncryption;
    }

    public static String getImageMetaRIL(byte[] data) {
        return String.valueOf(Byte.toUnsignedInt(data[0])) + String.valueOf(Byte.toUnsignedInt(data[1]));
    }

    public static Cursor getImagesCursor(Context context, String RIL) {
//        RIL = IMAGE_HEADER + RIL;
//
//        Cursor cursorImageCursor = SMSHandler.fetchSMSForImagesByRIL(context, RIL);
//        Log.d(ImageHandler.class.getName(), "Data image header RIL: " + RIL);
//        Log.d(ImageHandler.class.getName(), "Data image header counter: " + cursorImageCursor.getCount());
//        if(cursorImageCursor.moveToFirst()) {
//
//            SMS sms = new SMS(cursorImageCursor);
//            cursorImageCursor.close();
//
//            String body = sms.getBody().replace(RIL, "");
//
//            byte[] data = Base64.decode(body, Base64.DEFAULT);
//
//            Log.d(ImageHandler.class.getName(), "Data image ref: " + Byte.toUnsignedInt(data[0]));
//            int len = Byte.toUnsignedInt(data[2]);
//
//            StringBuilder query = new StringBuilder();
//            String[] parameters = new String[len];
//
//            for(Integer i=0; i<len; ++i ) {
//                if(i + 1 == len)
//                    query.append(Telephony.TextBasedSmsColumns.BODY + " like ?");
//                else
//                    query.append(Telephony.TextBasedSmsColumns.BODY + " like ? OR ");
//
//                parameters[i] = IMAGE_HEADER + Byte.toUnsignedInt(data[0]) + i + "%";
//            }
//
//            Cursor cursor = SMSHandler.fetchSMSForImages(context, query.toString(), parameters, sms.getThreadId());
//            Log.d(ImageHandler.class.getName(), "Data image founder counter: " + cursor.getCount() + "/" + len);
//            if(cursor.getCount() >= len) {
//                cursor.close();
//                return cursor;
//            }
//            cursor.close();
//        }
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

    public static byte[] getBitmapBytes(Bitmap bitmap) {
        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(buffer);
        return buffer.array();
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


    /**
     * Bitmap.Config.ARGB_8888 will produce the best quality image.
     * This is because it uses 32 bits per pixel, which allows for a wider range of colors and more detail.
     * createScaledBitmap will produce a lower quality image, because it uses a lower bit depth.
     * However, it will use less memory and be faster to render.
     *
     * Here is a table that summarizes the differences between the two methods:
     *
     * | Method | Bit depth | Quality | Memory usage | Speed |
     * |---|---|---|---|---|
     * | Bitmap.Config.ARGB_8888 | 32 bits | Best | High | Slow |
     * | createScaledBitmap | Lower bit depth | Lower | Low | Fast |
     *
     * Ultimately, the best method to use depends on your specific needs.
     * If you need the best possible quality image, then use Bitmap.Config.ARGB_8888.
     * If you need a lower quality image that uses less memory and is faster to render, then use createScaledBitmap.
     */

//    public Bitmap resizeImage(double resValue) {
//        // Create a new bitmap with the desired width and height
//        if(this.bitmap.getWidth() < resValue && this.bitmap.getHeight() < resValue)
//            return this.bitmap;
//
//        int[] dimensions = getDimension(this.bitmap.getWidth(), this.bitmap.getHeight(), resValue);
//        int width = dimensions[0];
//        int height = dimensions[1];
//
//        Bitmap resizedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//
//        // Create a canvas and draw the original bitmap onto it at the desired size
//        Canvas canvas = new Canvas(resizedBitmap);
//        RectF destRect = new RectF(0, 0, width, height);
//        canvas.drawBitmap(bitmap, null, destRect, null);
//
//        // Return the resized bitmap
//        return resizedBitmap;
//    }


    public static Bitmap removeAlpha(Bitmap bitmap) {
        if(bitmap.hasAlpha()) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            Bitmap.Config config = Bitmap.Config.RGB_565; // or Bitmap.Config.ARGB_8888 for higher quality
            Bitmap newBitmap = Bitmap.createBitmap(width, height, config);

            Canvas canvas = new Canvas(newBitmap);
            canvas.drawColor(Color.WHITE); // set background color

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setFilterBitmap(true);

            canvas.drawBitmap(bitmap, 0, 0, paint);

//            bitmap.recycle(); // release original bitmap memory

            return newBitmap;
        }
        return bitmap;
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

    public static boolean hasICCProfile(byte[] imageBytes) {
        return false;
    }



    public static byte[] extractContainerInformation(byte[] data) throws IOException {
        try {
            // Read the RIFF header
            byte[] riffHeader = new byte[4];
            int iterator = riffHeader.length;
            System.arraycopy(data, 0, riffHeader, 0, riffHeader.length);
            if (!new String(riffHeader, StandardCharsets.US_ASCII).equals("RIFF")) {
                throw new IOException("Not a WebP file: missing RIFF header");
            }

            // Read the file size (total size of the WebP container)
            byte[] fileSize = new byte[4];
            System.arraycopy(data, iterator, fileSize, 0, fileSize.length);
            Log.d(ImageHandler.class.getName(), "File size: " +
                    ByteBuffer.wrap(fileSize).order(ByteOrder.LITTLE_ENDIAN).getInt());
            iterator += 4;

            // Read the file type (should be "WEBP")
            byte[] webpHeader = new byte[4];
            System.arraycopy(data, iterator, webpHeader, 0, webpHeader.length);
            iterator += webpHeader.length;

            if (!new String(webpHeader, StandardCharsets.US_ASCII).equals("WEBP")) {
                throw new IOException("Not a WebP file: missing WEBP header: " +
                        new String(webpHeader, StandardCharsets.US_ASCII));
            }

            // Read the VP8 sub-chunk header
            byte[] vp8Header = new byte[8];

            System.arraycopy(data, iterator, vp8Header, 0, vp8Header.length);
            String vp8HeaderId = new String(vp8Header, 0, 4, StandardCharsets.US_ASCII);

            int vp8HeaderSize = ByteBuffer.wrap(vp8Header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (!vp8HeaderId.equals("VP8X") && !vp8HeaderId.equals("VP8 ")) {
                throw new IOException("Not a WebP file: missing VP8 sub-chunk: " + vp8HeaderId);
            }
            Log.d(ImageHandler.class.getName(), vp8HeaderId + " - " + vp8HeaderSize);

            // Skip the VP8 sub-chunk data
            iterator += vp8HeaderSize;

            List<String> subChunkHeaders = new ArrayList<String>();
            subChunkHeaders.add("VP8X");
            subChunkHeaders.add("VP8 ");
            subChunkHeaders.add("ICCP");
            subChunkHeaders.add("ANIM");
            subChunkHeaders.add("EXIF");
            subChunkHeaders.add("XMP ");
            subChunkHeaders.add("ALPH");
            while (iterator < data.length) {
                byte[] subChunkHeader = new byte[8];
                System.arraycopy(data, iterator, subChunkHeader, 0, subChunkHeader.length);
                String subChunkId = new String(subChunkHeader, 0, 4, StandardCharsets.US_ASCII);
                int subChunkSize = ByteBuffer.wrap(subChunkHeader, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                iterator += subChunkHeader.length;

                if (subChunkHeaders.contains(subChunkId)) {
                    Log.d(ImageHandler.class.getName(), subChunkId + ":" + subChunkSize);
//                    byte[] subChunkData = new byte[subChunkSize];
//                    System.arraycopy(data, iterator, subChunkData, 0, subChunkData.length);
                    if(subChunkId.equals("ICCP")) {
                        byte[] dataMinusICCP = new byte[data.length - (subChunkSize + subChunkHeader.length)];
                        System.arraycopy(data, 0, dataMinusICCP, 0, iterator - subChunkHeader.length);
                        System.arraycopy(data, iterator + subChunkSize, dataMinusICCP, iterator - subChunkHeader.length,
                                data.length - (iterator + subChunkSize));
                        data = dataMinusICCP;
                        iterator -= subChunkHeader.length;
                    } else iterator += subChunkSize;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return data;
    }

}
