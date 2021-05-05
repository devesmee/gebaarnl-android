package com.example.gebaarnl;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.CountDownTimer;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.glutil.EglManager;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CameraFragment extends Fragment {

    View currentView;
    ImageView letterImageView;
    String level;
    TextView currentLetterTextView;
    TextView countdownPracticeTextView;
    TextView showWholeHandTextView;
    int countdown;
    Boolean awaitingWholeHand;
    CountDownTimer timer;
    int countdownTimerValue;

    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;

    NormalizedLandmark wristLandmark;
    NormalizedLandmark thumbLandmark;
    NormalizedLandmark pointerLandmark;
    NormalizedLandmark middleLandmark;
    NormalizedLandmark ringLandmark;
    NormalizedLandmark pinkyLandmark;

    float wristThumbY;
    float wristThumbX;
    float wristPointer;
    float wristMiddle;
    float wristRing;
    float wristPinky;

    private static final String TAG = "CameraFragment";
    private static final String BINARY_GRAPH_NAME = "hand_tracking_mobile_gpu.binarypb";
    private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
    private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks";
    private static final String INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands";
    private static final int NUM_HANDS = 1;
    // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
    // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
    // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
    // corner, whereas MediaPipe in general assumes the image origin is at top-left.
    private static final boolean FLIP_FRAMES_VERTICALLY = true;

    static {
        // Load all native libraries needed by the app.
        System.loadLibrary("mediapipe_jni");
        System.loadLibrary("opencv_java3");
    }

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private SurfaceTexture previewFrameTexture;
    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private SurfaceView previewDisplayView;
    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private FrameProcessor processor;
    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private ExternalTextureConverter converter;
    // Handles camera access via the {@link CameraX} Jetpack support library.
    private CameraXPreviewHelper cameraHelper;

    public CameraFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CameraFragment.
     */
    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle levelBundle = getArguments();
        level = levelBundle.getString("chosen_level");
        countdown = levelBundle.getInt("countdown");
    }

    @Override
    public void onResume() {
        super.onResume();
        timer.start();
        converter =
                new ExternalTextureConverter(
                        eglManager.getContext(), 2);
        converter.setFlipY(FLIP_FRAMES_VERTICALLY);
        converter.setConsumer(processor);
        if (PermissionHelper.cameraPermissionsGranted(getActivity())) {
            startCamera();
        }
        countdown = 2;
    }

    @Override
    public void onPause() {
        super.onPause();
        converter.close();
        timer.cancel();
        // Hide preview display until we re-open the camera again.
        previewDisplayView.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        converter.close();
        timer.cancel();

        // Hide preview display until we re-open the camera again.
        previewDisplayView.setVisibility(View.GONE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        currentView = inflater.inflate(R.layout.fragment_camera, container, false);

        fragmentManager = getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();

        previewDisplayView = new SurfaceView(getContext());
        countdownPracticeTextView = (TextView) currentView.findViewById(R.id.countdownPractice);
        showWholeHandTextView = (TextView) currentView.findViewById(R.id.showWholeHand);
        currentLetterTextView = (TextView) currentView.findViewById(R.id.currentLetter);
        currentLetterTextView.setText(level);
        Log.e("CREATE COUNTDOWN: ", String.valueOf(countdown));

        countdownTimerValue = 5;
        awaitingWholeHand = true;
        timer = new CountDownTimer(5000, 300) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownTimerValue--;
            }

            @Override
            public void onFinish() {
                timer.start();
                countdownTimerValue = 5;
            }
        };

        setupPreviewDisplayView();
        setImageInstruction();
        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        AndroidAssetUtil.initializeNativeAssetManager(getActivity());
        eglManager = new EglManager(null);
        processor =
                new FrameProcessor(
                        getActivity(),
                        eglManager.getNativeContext(),
                        BINARY_GRAPH_NAME,
                        INPUT_VIDEO_STREAM_NAME,
                        OUTPUT_VIDEO_STREAM_NAME);
        processor
                .getVideoSurfaceOutput()
                .setFlipY(FLIP_FRAMES_VERTICALLY);

        PermissionHelper.checkAndRequestCameraPermissions(getActivity());
        AndroidPacketCreator packetCreator = processor.getPacketCreator();
        Map<String, Packet> inputSidePackets = new HashMap<>();
        inputSidePackets.put(INPUT_NUM_HANDS_SIDE_PACKET_NAME, packetCreator.createInt32(NUM_HANDS));
        processor.setInputSidePackets(inputSidePackets);

        return currentView;
    }

    private void setImageInstruction() {
        letterImageView = currentView.findViewById(R.id.letterImageView);
        switch (level){
            case "A":
                letterImageView.setImageResource(R.drawable.letter_a);
                break;
            case "B":
                letterImageView.setImageResource(R.drawable.letter_b);
                break;
            case "C":
                letterImageView.setImageResource(R.drawable.letter_c);
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void onCameraStarted(SurfaceTexture surfaceTexture) {
        previewFrameTexture = surfaceTexture;
        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        previewDisplayView.setVisibility(View.VISIBLE);
    }

    protected Size cameraTargetResolution() {
        return null; // No preference and let the camera (helper) decide.
    }

    public void startCamera() {
        cameraHelper = new CameraXPreviewHelper();
        cameraHelper.setOnCameraStartedListener(
                surfaceTexture -> {
                    onCameraStarted(surfaceTexture);
                });
        CameraHelper.CameraFacing cameraFacing = CameraHelper.CameraFacing.FRONT;
        cameraHelper.startCamera(
                getActivity(), cameraFacing, /*unusedSurfaceTexture=*/ null, cameraTargetResolution());
    }

    protected Size computeViewSize(int width, int height) {
        return new Size(width, height);
    }

    protected void onPreviewDisplaySurfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        Size viewSize = computeViewSize(width, height);
        Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
        boolean isCameraRotated = cameraHelper.isCameraRotated();

        // Connect the converter to the camera-preview frames as its input (via
        // previewFrameTexture), and configure the output width and height as the computed
        // display size.
            processor.addPacketCallback(
                    OUTPUT_LANDMARKS_STREAM_NAME,
                    (packet) -> {
                        List<NormalizedLandmarkList> multiHandLandmarks =
                                PacketGetter.getProtoVector(packet, NormalizedLandmarkList.parser());
                        if(countdownTimerValue == 4) {
                            letterDetection(multiHandLandmarks);
                        }
                    });

            converter.setSurfaceTextureAndAttachToGLContext(
                    previewFrameTexture,
                    isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                    isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
    }

    private void setupPreviewDisplayView() {
        previewDisplayView.setVisibility(View.GONE);
        ViewGroup viewGroup = currentView.findViewById(R.id.camera_preview);
        viewGroup.addView(previewDisplayView);

        previewDisplayView
                .getHolder()
                .addCallback(
                        new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                onPreviewDisplaySurfaceChanged(holder, format, width, height);
                            }

                            @Override
                            public void surfaceDestroyed(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(null);
                            }
                        });
    }

    private synchronized void letterDetection(List<NormalizedLandmarkList> handLandmarks) {
        // full hand coordinates:
        // 0 = 0.47 / 0.80  4 = 0.18 / 0.57  8 = 0.38 / 0.42  12 = 0.49 / 0.41  16 = 0.56 / 0.44  20 = 0.64 / 0.50
        // A coordinates:
        // 0 = 0.55 / 0.77  4 = 0.18 / 0.62  8 = 0.46 / 0.64  12 = 0.51 / 0.67  16 = 0.56 / 0.69  20 = 0.61 / 0.69
        // B coordinates:
        // 0 = 0.47 / 0.80  4 = 0.49 / 0.53  8 = 0.38 / 0.42  12 = 0.49 / 0.41  16 = 0.56 / 0.44  20 = 0.64 / 0.50
        // C coordinates:
        // 0 = 0.47 / 0.80  4 = 0.43 / 0.55  8 = 0.40 / 0.48  12 = 0.49 / 0.48  16 = 0.58 / 0.49  20 = 0.67 / 0.55

        wristLandmark = handLandmarks.get(0).getLandmark(0);
        thumbLandmark = handLandmarks.get(0).getLandmark(4);
        pointerLandmark = handLandmarks.get(0).getLandmark(8);
        middleLandmark = handLandmarks.get(0).getLandmark(12);
        ringLandmark = handLandmarks.get(0).getLandmark(16);
        pinkyLandmark = handLandmarks.get(0).getLandmark(20);

        wristThumbY = wristLandmark.getY() - thumbLandmark.getY();
        wristThumbX = wristLandmark.getX() - thumbLandmark.getX();
        wristPointer = wristLandmark.getY() - pointerLandmark.getY();
        wristMiddle = wristLandmark.getY() - middleLandmark.getY();
        wristRing = wristLandmark.getY() - ringLandmark.getY();
        wristPinky = wristLandmark.getY() - pinkyLandmark.getY();

        //Log.e("wristThumbY: ", String.valueOf(wristThumbY));
        //Log.e("wristThumbX: ", String.valueOf(wristThumbX));
        //Log.e("wristPointer: ", String.valueOf(wristPointer));
        //Log.e("wristMiddle: ", String.valueOf(wristMiddle));
        //Log.e("wristRing: ", String.valueOf(wristRing));
        //Log.e("wristPinky: ", String.valueOf(wristPinky));
        //Log.e("AWAITING WHOLE HAND : ", String.valueOf(awaitingWholeHand));
        //Log.e("countdown : ", String.valueOf(countdown));

        if (awaitingWholeHand) {
            Log.e("CHECK FOR: ", "whole hand!");
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showWholeHandTextView.setVisibility(View.VISIBLE);
                }
            });
            if (wristThumbY > 0.15 && wristThumbX > 0.3 && wristPointer > 0.35 && wristMiddle > 0.35 && wristRing > 0.30 && wristPinky > 0.25) {
                Log.e("WHOLE HAND: ", "recognized!");
                awaitingWholeHand = false;
            }
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showWholeHandTextView.setVisibility(View.INVISIBLE);
                }
            });
            switch(level){
                case "A":
                    Log.e("CHECK FOR: ", "A");
                    if (!awaitingWholeHand && wristThumbY > 0.15 && wristPointer < 0.20 && wristMiddle < 0.15 && wristRing < 0.15 && wristPinky < 0.15) {
                        Log.e("A: ", "recognized!");
                        awaitingWholeHand = true;
                        countdown--;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setCountdownLabel(countdown);
                            }
                        });
                    }
                    break;
                case "B":
                    Log.e("CHECK FOR: ", "B");
                    if (!awaitingWholeHand && wristThumbY < 0.25 && wristThumbX < 0.1 && wristPointer > 0.35 && wristMiddle > 0.35 && wristRing > 0.30 && wristPinky > 0.25) {
                        Log.e("B: ", "recognized!");
                        awaitingWholeHand = true;
                        countdown--;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setCountdownLabel(countdown);
                            }
                        });
                    }
                    break;
                case "C":
                    Log.e("CHECK FOR: ", "C");
                    if (!awaitingWholeHand && wristThumbY > 0.15 && wristThumbX < 0.1 && wristPointer < 0.35 && wristPointer > 0.15 && wristMiddle < 0.35 && wristMiddle > 0.20 && wristRing < 0.35 && wristRing > 0.20 && wristPinky < 0.30 && wristPinky > 0.10) {
                        Log.e("C: ", "recognized!");
                        awaitingWholeHand = true;
                        countdown--;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setCountdownLabel(countdown);
                            }
                        });
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private String getMultiHandLandmarksDebugString(List<NormalizedLandmarkList> multiHandLandmarks) {
        if (multiHandLandmarks.isEmpty()) {
            return "No hand landmarks";
        }
        String multiHandLandmarksStr = "Number of hands detected: " + multiHandLandmarks.size() + "\n";
        int handIndex = 0;
        for (NormalizedLandmarkList landmarks : multiHandLandmarks) {
            multiHandLandmarksStr +=
                    "\t#Hand landmarks for hand[" + handIndex + "]: " + landmarks.getLandmarkCount() + "\n";
            int landmarkIndex = 0;
            for (LandmarkProto.NormalizedLandmark landmark : landmarks.getLandmarkList()) {
                multiHandLandmarksStr +=
                        "\t\tLandmark ["
                                + landmarkIndex
                                + "]: ("
                                + landmark.getX()
                                + ", "
                                + landmark.getY()
                                + ", "
                                + landmark.getZ()
                                + ")\n";
                ++landmarkIndex;
            }
            ++handIndex;
        }
        return multiHandLandmarksStr;
    }

    public void setCountdownLabel(int countdown) {
        switch (countdown){
            /* set to 2 otherwise the app & phone will crash
            case 3:
                countdownPracticeTextView.setText(R.string.three_times);
                break;
             */
            case 2:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countdownPracticeTextView.setText(R.string.two_times);
                    }
                });
                break;
            case 1:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countdownPracticeTextView.setText(R.string.one_time);
                    }
                });
                break;
            case 0:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countdownPracticeTextView.setText(R.string.done);
                    }
                });
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.putExtra("completed_level", level);
                startActivity(intent);

                break;
        }
    }
}