package com.example.HelloSphero;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;
import orbotix.sphero.Sphero;

import java.util.ArrayList;
import java.util.List;

/**
 * Connects to an available Sphero robot, and then flashes its LED.
 */
public class HelloWorldActivity extends Activity {
    private DrawView view;

    private class DrawView extends View {

        public DrawView(Context context) {
            super(context);
        }

        protected void onDraw(Canvas canvas) {
            Paint myPaint = new Paint();
            myPaint.setStyle(Paint.Style.FILL);
            myPaint.setColor(running ? Color.RED : Color.WHITE);
            for (Point point : points) {
                canvas.drawCircle(point.x, point.y, 10, myPaint);
            }
        }
    }

    static class Point {
        float x, y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private List<Point> points = new ArrayList<>();
    private boolean running = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!running && connected) {
            int action = event.getActionMasked();

            switch (action) {

                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    points.add(new Point(event.getX(), event.getY()));
                    view.invalidate();

                    break;

                case MotionEvent.ACTION_UP:
                    running = true;
                    points.add(new Point(event.getX(), event.getY()));
                    view.invalidate();

//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//
//                        }
//                    }).start();
//                    float maxDist = 0;
//                    for (int i = 1; i < points.size(); i++) {
//                        float dx = points.get(i).x - points.get(i - 1).x;
//                        float dy = points.get(i).y - points.get(i - 1).y;
//                        float dist = (float)Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
//                        if(dist > maxDist)
//                            maxDist = dist;
//                    }

                    for (int i = 1; i < points.size(); i++) {
//                        float baseSpd = 0.8f;
                        float dx = points.get(i).x - points.get(i - 1).x;
                        float dy = points.get(i).y - points.get(i - 1).y;
//                        float dist = (float)Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
                        float heading = (float) Math.toDegrees(Math.atan2(dy, dx));
                        if (heading < 0) heading += 360;
//                        float velocity = (float) (baseSpd * Math.sqrt(dist / maxDist));
                        if (mRobot != null) {
                            mRobot.drive(heading, 0.4f);
                            mRobot.setColor((int)(255 * Math.random()), (int)(255 * Math.random()), (int)(255 * Math.random()));
                        }
                        try {
                            Thread.sleep(75);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (mRobot != null) mRobot.stop();
                    running = false;
                    points = new ArrayList<>();
                    view.invalidate();

                    break;
            }
        }
        return super.onTouchEvent(event);
    }


    public static final String TAG = "OBX-HelloWorld";

    /**
     * The Sphero Robot
     */
    private Sphero mRobot;

    private boolean connected = false;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new DrawView(this);
        setContentView(view);
    }

    /**
     * Called when the user comes back to this app
     */
    @Override
    protected void onResume() {
        super.onResume();

        RobotProvider.getDefaultProvider().addConnectionListener(new ConnectionListener() {
            @Override
            public void onConnected(Robot robot) {
                mRobot = (Sphero) robot;
                try {
                    Log.d(TAG, "Connected On Thread: " + Thread.currentThread().getName());
                    Log.d(TAG, "Connected: " + mRobot);
                    Toast.makeText(HelloWorldActivity.this, mRobot.getName() + " Connected", Toast.LENGTH_LONG).show();
                    mRobot.enableStabilization(false);
                    mRobot.drive(90, 0);
                    mRobot.setBackLEDBrightness(.5f);
                    Thread.sleep(3000);
                    mRobot.enableStabilization(true);
                    connected = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConnectionFailed(Robot sphero) {
                Log.d(TAG, "Connection Failed: " + sphero);
                Toast.makeText(HelloWorldActivity.this, "Sphero Connection Failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnected(Robot robot) {
                Log.d(TAG, "Disconnected: " + robot);
                Toast.makeText(HelloWorldActivity.this, "Sphero Disconnected", Toast.LENGTH_SHORT).show();
                mRobot = null;
                boolean success = RobotProvider.getDefaultProvider().startDiscovery(HelloWorldActivity.this);
                if (!success) {
                    Toast.makeText(HelloWorldActivity.this, "Unable To start Discovery!", Toast.LENGTH_LONG).show();
                }
            }
        });

        RobotProvider.getDefaultProvider().addDiscoveryListener(new DiscoveryListener() {
            @Override
            public void onBluetoothDisabled() {
                Log.d(TAG, "Bluetooth Disabled");
                Toast.makeText(HelloWorldActivity.this, "Bluetooth Disabled", Toast.LENGTH_LONG).show();
            }

            @Override
            public void discoveryComplete(List<Sphero> spheros) {
                Log.d(TAG, "Found " + spheros.size() + " robots");
            }

            @Override
            public void onFound(List<Sphero> sphero) {
                Log.d(TAG, "Found: " + sphero);
                RobotProvider.getDefaultProvider().connect(sphero.iterator().next());
            }
        });

        boolean success = RobotProvider.getDefaultProvider().startDiscovery(this);
        if (!success) {
            Toast.makeText(HelloWorldActivity.this, "Unable To start Discovery!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when the user presses the back or home button
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mRobot != null) {
            mRobot.disconnect();
        }
    }

}
