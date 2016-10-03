package gruppn.kasslr;

import com.google.android.cameraview.CameraView;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class CameraActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_IMAGE_DESCRIPTION = 2;

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };
/*
    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    private int mCurrentFlash;
*/
    private Kasslr app;

    private CameraView mCameraView;

    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        app = (Kasslr) getApplication();
        mCameraView = (CameraView) findViewById(R.id.camera);
        mCameraView.addCallback(mCallback);

        Button takePicture = (Button) findViewById(R.id.snap);
        takePicture.setOnClickListener(view -> mCameraView.takePicture());
        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        */
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance(R.string.camera_permission_confirmation,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION,
                            R.string.camera_permission_not_granted)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onPause() {
        mCameraView.stop();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Please give camera permission",
                            Toast.LENGTH_SHORT).show();
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
        switch (item.getItemId()) {
            case R.id.switch_flash:
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                break;
            case R.id.switch_camera:
                if (mCameraView != null) {
                    int facing = mCameraView.getFacing();
                    mCameraView.setFacing(facing == CameraView.FACING_FRONT ?
                            CameraView.FACING_BACK : CameraView.FACING_FRONT);
                }
                break;
        }
        */
        return false;
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private CameraView.Callback mCallback = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);

            getBackgroundHandler().post(() -> {
                File file = getNewPictureFile();

                OutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                    os.write(data);
                } catch (IOException e) {
                    Log.w(TAG, "Cannot write to " + file, e);
                    return;
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(CameraActivity.this, AddWordActivity.class);
                        intent.putExtra(AddWordActivity.EXTRA_IMAGE, Uri.fromFile(file));
                        intent.putExtra(AddWordActivity.EXTRA_FINISH_ON_BACK, true);

                        String transition = getString(R.string.transition_add_word);
                        ActivityOptionsCompat options = ActivityOptionsCompat
                                .makeSceneTransitionAnimation(CameraActivity.this, cameraView, transition);

                        ActivityCompat.startActivityForResult(CameraActivity.this, intent,
                                REQUEST_IMAGE_DESCRIPTION, options.toBundle());
                    }
                });
            });
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_DESCRIPTION) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Added " + data.getExtras().getString(AddWordActivity.EXTRA_IMAGE_DESCRIPTION)
                        + " to vocabulary", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File getNewPictureFile() {
        File dir = app.getPictureDirectory();
        if (!dir.exists()) {
            dir.mkdir();
        }

        return new File(dir, UUID.randomUUID().toString() + ".jpg");
    }

    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(@StringRes int message,
                                                             String[] permissions, int requestCode,
                                                             @StringRes int notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putInt(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(args.getInt(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                        if (permissions == null) {
                            throw new IllegalArgumentException();
                        }
                        ActivityCompat.requestPermissions(getActivity(), permissions,
                                args.getInt(ARG_REQUEST_CODE));
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        Toast.makeText(getActivity(), args.getInt(ARG_NOT_GRANTED_MESSAGE),
                                Toast.LENGTH_SHORT).show();
                    }).create();
        }

    }

}