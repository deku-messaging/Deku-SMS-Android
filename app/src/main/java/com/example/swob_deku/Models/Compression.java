package com.example.swob_deku.Models;

import android.util.Log;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

public class Compression {
    public static byte[] compressDeflate(byte[] input) {
        byte[] output = new byte[input.length];
        Deflater compresser = new Deflater();
        compresser.setInput(input);
        compresser.finish();
        int compressedDataLength = compresser.deflate(output);
        byte[] result = new byte[compressedDataLength];
        System.arraycopy(output, 0, result, 0, compressedDataLength);
        compresser.end();

        return result;
    }

    public static byte[] compressLZ4(byte[] inputBytes) {
        try {
            // Create an LZ4 compressor
            LZ4Factory factory = LZ4Factory.fastestInstance();
            LZ4Compressor compressor = factory.fastCompressor();

            // Compress the input bytes
            byte[] compressedBytes = compressor.compress(inputBytes);

            // Return the compressed bytes
            return compressedBytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] compressGzip(byte[] inputBytes) {
        try {
            // Create a ByteArrayOutputStream to hold the compressed output
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // Create a GZIPOutputStream to compress the input bytes
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);

            // Write the input bytes to the GZIPOutputStream
            gzipOutputStream.write(inputBytes);

            // Close the GZIPOutputStream to flush any remaining data and finalize the compression
            gzipOutputStream.close();

            // Return the compressed bytes
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decompressGZIP(byte[] inputBytes) {
        try {
            // Create a GZIP input stream from the input bytes
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(inputBytes));

            // Create a byte output stream to hold the decompressed bytes
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // Decompress the input bytes
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            // Close the streams
            gzipInputStream.close();
            byteArrayOutputStream.close();

            // Return the decompressed bytes
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static byte[] decompressDeflate(byte[] input) throws DataFormatException {
        try {
            // Create an Inflater for decompression
            Inflater inflater = new Inflater();
            inflater.setInput(input, 0, input.length);

            // Create a byte output stream to hold the decompressed bytes
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // Decompress the input bytes
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                byteArrayOutputStream.write(buffer, 0, count);
            }

            // Close the streams
            inflater.end();
            byteArrayOutputStream.close();

            // Return the decompressed bytes
            return byteArrayOutputStream.toByteArray();
        } catch (DataFormatException | IOException e) {
            e.printStackTrace();
        }
        return null;

//        Inflater decompresser = new Inflater();
//        decompresser.setInput(input, 0, input.length);
//        byte[] result = new byte[100];
//        int resultLength = decompresser.inflate(result);
//        decompresser.end();
    }
}