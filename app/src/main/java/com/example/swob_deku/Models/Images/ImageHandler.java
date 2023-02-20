package com.example.swob_deku.Models.Images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import com.example.swob_deku.Models.Security.SecurityAES;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ImageHandler {

    Uri imageUri;
    Context context;
    public Bitmap bitmap;

    String iv = "1234567890123456";
    String secretKey = "12345678901234561234567890123456";

    public ImageHandler(Context context, Uri imageUri) throws IOException {
        this.imageUri = imageUri;
        this.context = context;
        this.bitmap = MediaStore.Images.Media.getBitmap(this.context.getContentResolver(), this.imageUri);
    }

    public byte[] getBitmapByte(Bitmap bitmap) {
        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);
        return byteBuffer.array();
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


    public int[] getDimension(int width, int height, int ratio) {
        double wr = (double) width / height;
        double hr = (double) height / width;

        if(wr > hr) {
            width = ratio;
            height = (int) Math.round(ratio * hr);
        }
        else if(hr > wr) {
            height = ratio;
            width = (int) Math.round(ratio * wr);
        }
        else {
            width = ratio;
            height = ratio;
        }

        return new int[]{width, height};
    }

    public Bitmap resizeImage(int resValue) throws IOException {
        // use ratios for compressions rather than just raw values
        if(this.bitmap.getWidth() < resValue && this.bitmap.getHeight() < resValue)
            return this.bitmap;

        int[] dimensions = getDimension(this.bitmap.getWidth(), this.bitmap.getHeight(), resValue);
        int width = dimensions[0];
        int height = dimensions[1];


        return Bitmap.createScaledBitmap(this.bitmap, width, height, true);
    }

    public byte[] encryptImage(byte[] imageBytes) throws Throwable {
        SecurityAES aes = new SecurityAES();

        byte[] bytesJpegEncryption = aes.encrypt(iv.getBytes(StandardCharsets.UTF_8),
                imageBytes, secretKey.getBytes(StandardCharsets.UTF_8));

        return bytesJpegEncryption;
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
}
