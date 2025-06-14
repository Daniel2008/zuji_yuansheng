package com.damors.zuji.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 图片处理工具类
 * 提供图片压缩、旋转、文件操作等功能
 */
public class ImageUtils {
    
    private static final String TAG = "ImageUtils";
    private static final String FILE_PROVIDER_AUTHORITY = "com.damors.zuji.fileprovider";
    
    public static Bitmap getBitmap(Context context, int vectorDrawableId) {
        final VectorDrawableCompat drawable = VectorDrawableCompat.create(context.getResources(), vectorDrawableId, null);
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
    
    /**
     * 创建临时图片文件
     */
    public static File createTempImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
    
    /**
     * 获取文件URI（适配Android 7.0+）
     */
    public static Uri getFileUri(Context context, File file) {
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file);
    }
    
    /**
     * 压缩图片
     * @param context 上下文
     * @param imageUri 图片URI
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @param quality 压缩质量(0-100)
     * @return 压缩后的Bitmap
     */
    public static Bitmap compressImage(Context context, Uri imageUri, int maxWidth, int maxHeight, int quality) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }
            
            // 获取图片尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            // 计算缩放比例
            int inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
            
            // 重新打开输入流
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }
            
            // 解码图片
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            if (bitmap == null) {
                return null;
            }
            
            // 处理图片旋转
            bitmap = rotateImageIfRequired(context, bitmap, imageUri);
            
            // 进一步压缩到指定尺寸
            bitmap = scaleBitmap(bitmap, maxWidth, maxHeight);
            
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "压缩图片失败", e);
            return null;
        }
    }
    
    /**
     * 计算图片缩放比例
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    /**
     * 根据EXIF信息旋转图片
     */
    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) {
        try {
            InputStream input = context.getContentResolver().openInputStream(selectedImage);
            if (input == null) {
                return img;
            }
            
            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            input.close();
            
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (Exception e) {
            Log.e(TAG, "旋转图片失败", e);
            return img;
        }
    }
    
    /**
     * 旋转图片
     */
    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }
    
    /**
     * 缩放Bitmap到指定尺寸
     */
    private static Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }
        
        float scaleWidth = (float) maxWidth / width;
        float scaleHeight = (float) maxHeight / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        if (scaledBitmap != bitmap) {
            bitmap.recycle();
        }
        
        return scaledBitmap;
    }
    
    /**
     * 将Bitmap保存到临时文件
     */
    public static File saveBitmapToTempFile(Context context, Bitmap bitmap) {
        try {
            File tempFile = createTempImageFile(context);
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "保存Bitmap到文件失败", e);
            return null;
        }
    }
    
    /**
     * 将Bitmap转换为字节数组
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }
    
    /**
     * 删除临时文件
     */
    public static void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            Log.d(TAG, "删除临时文件: " + file.getName() + ", 结果: " + deleted);
        }
    }
}
