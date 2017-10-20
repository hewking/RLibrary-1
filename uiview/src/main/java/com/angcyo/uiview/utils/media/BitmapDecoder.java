package com.angcyo.uiview.utils.media;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;

import com.angcyo.uiview.R;
import com.angcyo.uiview.RApplication;
import com.angcyo.uiview.utils.RUtils;
import com.angcyo.uiview.utils.ScreenUtil;
import com.angcyo.uiview.utils.file.AttachmentStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BitmapDecoder {
    public static Bitmap decode(InputStream is) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        // RGB_565
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        /**
         * 在4.4上，如果之前is标记被移动过，会导致解码失败
         */
        try {
            if (is.markSupported()) {
                is.reset();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return BitmapFactory.decodeStream(is, null, options);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Bitmap decodeSampledForDisplay(String pathName) {
        return decodeSampledForDisplay(pathName, true);
    }

    public static Bitmap decodeSampledForDisplay(String pathName, boolean withTextureLimit) {
        float ratio = ImageUtil.MAX_IMAGE_RATIO;
        int[][] reqBounds = new int[][]{
                new int[]{ScreenUtil.screenWidth * 2, ScreenUtil.screenHeight},
                new int[]{ScreenUtil.screenWidth, ScreenUtil.screenHeight * 2},
                new int[]{(int) (ScreenUtil.screenWidth * 1.414), (int) (ScreenUtil.screenHeight * 1.414)},
        };

        // decode bound
        int[] bound = decodeBound(pathName);
        // pick request bound
        int[] reqBound = pickReqBoundWithRatio(bound, reqBounds, ratio);

        int width = bound[0];
        int height = bound[1];
        int reqWidth = reqBound[0];
        int reqHeight = reqBound[1];

        // calculate sample size
        int sampleSize = SampleSizeUtil.calculateSampleSize(width, height, reqWidth, reqHeight);

        if (withTextureLimit) {
            // adjust sample size
            sampleSize = SampleSizeUtil.adjustSampleSizeWithTexture(sampleSize, width, height);
        }

        int RETRY_LIMIT = 5;
        Bitmap bitmap = decodeSampled(pathName, sampleSize);
        while (bitmap == null && RETRY_LIMIT > 0) {
            sampleSize++;
            RETRY_LIMIT--;
            bitmap = decodeSampled(pathName, sampleSize);
        }

        return bitmap;
    }

    public static int[] decodeBound(String pathName) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        return new int[]{options.outWidth, options.outHeight};
    }

    public static int[] decodeBound(Resources res, int resId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        return new int[]{options.outWidth, options.outHeight};
    }

    private static int[] pickReqBoundWithRatio(int[] bound, int[][] reqBounds, float ratio) {
        float hRatio = bound[1] == 0 ? 0 : (float) bound[0] / (float) bound[1];
        float vRatio = bound[0] == 0 ? 0 : (float) bound[1] / (float) bound[0];

        if (hRatio >= ratio) {
            return reqBounds[0];
        } else if (vRatio >= ratio) {
            return reqBounds[1];
        } else {
            return reqBounds[2];
        }
    }

    public static Bitmap decodeSampled(String pathName, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        // RGB_565
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        // sample size
        options.inSampleSize = sampleSize;

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(pathName, options);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }

        return checkInBitmap(bitmap, options, pathName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static Bitmap checkInBitmap(Bitmap bitmap,
                                        BitmapFactory.Options options, String path) {
        boolean honeycomb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        if (honeycomb && bitmap != options.inBitmap && options.inBitmap != null) {
            options.inBitmap.recycle();
            options.inBitmap = null;
        }

        if (bitmap == null) {
            try {
                bitmap = BitmapFactory.decodeFile(path, options);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    public static int[] decodeBound(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            int[] bound = decodeBound(is);
            return bound;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new int[]{0, 0};
    }

    public static int[] decodeBound(InputStream is) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);

        return new int[]{options.outWidth, options.outHeight};
    }

    public static Bitmap decodeSampled(InputStream is, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        // RGB_565
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        // sample size
        options.inSampleSize = getSampleSize(is, reqWidth, reqHeight);

        try {
            return BitmapFactory.decodeStream(is, null, options);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Bitmap decodeSampled(String pathName, int reqWidth, int reqHeight) {
        return decodeSampled(pathName, getSampleSize(pathName, reqWidth, reqHeight));
    }

    public static int getSampleSize(InputStream is, int reqWidth, int reqHeight) {
        // decode bound
        int[] bound = decodeBound(is);

        // calculate sample size
        int sampleSize = SampleSizeUtil.calculateSampleSize(bound[0], bound[1], reqWidth, reqHeight);

        return sampleSize;
    }

    public static int getSampleSize(String pathName, int reqWidth, int reqHeight) {
        // decode bound
        int[] bound = decodeBound(pathName);

        // calculate sample size
        int sampleSize = SampleSizeUtil.calculateSampleSize(bound[0], bound[1], reqWidth, reqHeight);

        return sampleSize;
    }

    /**
     * ******************************* decode resource ******************************************
     */

    public static Bitmap decodeSampled(Resources resources, int resId, int reqWidth, int reqHeight) {
        return decodeSampled(resources, resId, getSampleSize(resources, resId, reqWidth, reqHeight));
    }

    public static int getSampleSize(Resources resources, int resId, int reqWidth, int reqHeight) {
        // decode bound
        int[] bound = decodeBound(resources, resId);

        // calculate sample size
        int sampleSize = SampleSizeUtil.calculateSampleSize(bound[0], bound[1], reqWidth, reqHeight);

        return sampleSize;
    }


    public static Bitmap decodeSampled(Resources res, int resId, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        // RGB_565
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        // sample size
        options.inSampleSize = sampleSize;

        try {
            return BitmapFactory.decodeResource(res, resId, options);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 星期二 2017-6-13 视频截图
     */
    public static boolean extractThumbnail(String videoPath, String thumbPath) {
        if (!AttachmentStore.isFileExist(thumbPath)) {
//            Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND);
            Bitmap thumbnail = createVideoThumbnail(videoPath);
            if (thumbnail != null) {
                AttachmentStore.saveBitmap(thumbnail, thumbPath, true);
                return true;
            }
        }
        return false;
    }

    /**
     * 截取并压缩视频帧
     */
    public static boolean extractThumbnailAndCompress(String videoPath, String thumbPath) {
        if (!AttachmentStore.isFileExist(thumbPath)) {
//            Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND);
            Bitmap thumbnail = createVideoThumbnail(videoPath);
            if (thumbnail != null) {
                Bitmap bitmap;// = RUtils.compressBitmap(thumbnail);//BitmapHelper.compressBitmap(thumbnail, 600, 800);
                //L.e("call: extractThumbnailAndCompress([videoPath, thumbPath])1-> " + thumbnail.getByteCount() + " " + bitmap.getByteCount());
                bitmap = RUtils.compressBitmap(thumbnail, 4);
                //L.e("call: extractThumbnailAndCompress([videoPath, thumbPath])2-> " + thumbnail.getByteCount() + " " + bitmap.getByteCount());
                //bitmap = RUtils.compressBitmap(thumbnail, 8);
                //L.e("call: extractThumbnailAndCompress([videoPath, thumbPath])3-> " + thumbnail.getByteCount() + " " + bitmap.getByteCount());

                AttachmentStore.saveBitmap(bitmap, thumbPath, true);
                return true;
            }
        }
        return false;
    }

    /**
     * 截图视频帧, 视频截图
     */
    public static Bitmap createVideoThumbnail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int[] times = new int[]{
                MediaMetadataRetriever.OPTION_NEXT_SYNC,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                MediaMetadataRetriever.OPTION_CLOSEST
        };

        int index = 0;
        retriever.setDataSource(filePath);

        while (index < times.length) {
            try {
                bitmap = retriever.getFrameAtTime(1000 * (index + 1), times[index]);
            } catch (Exception ex) {
                // Assume this is a corrupt video file
            }

            if (bitmap == null) {
                index++;

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }

        retriever.release();

        if (bitmap == null)
            return BitmapFactory.decodeResource(RApplication.getApp().getResources(), R.drawable.default_image);

        return bitmap;
    }
}
