package gruppn.kasslr.game;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.icu.text.MessagePattern;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * Created by Adam on 2016-09-29.
 */

public class LaneGame extends Activity {


    final String DEBUG_TAG = "GaME";

    private GestureDetectorCompat mDetector;
    private GameView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        view = new GameView(this);
        setContentView(view);
        mDetector = new GestureDetectorCompat(this, new GameGestureListener());
    }

    @Override
    public void onPause() {
        view.pause();
        finish();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tell the gameView resume method to execute
        view.resume();
    }


    class GameGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "vel x: " + velocityX);
            Log.d(DEBUG_TAG, "vel y: " + velocityY);
            view.updateInput(velocityX, velocityY);
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

}

class GameView extends SurfaceView implements Runnable {

    Thread gameThread = null;
    SurfaceHolder ourHolder;

    private int gameWidth;
    private int gameHeight;
    volatile boolean playing;
    Canvas canvas;
    Paint paint;

    final String DEBUG_TAG = "GAMELOGIC";
    final int NUM_LANES = 3;
    final int BACKGROUND_COLOR = Color.parseColor("#000000");

    private float frameRate = 80;
    private float frameTime = 1000 / frameRate;
    private double fps = 0;

    private float playerY = 150;
    private float playerX = 150;
    private float playerDeltaY = 0;
    private float playerDeltaX = 2;
    private int playerTarget = 0;

    private Set<Particle> particles = new HashSet<Particle>();
    private Set<Target> liveTargets = new HashSet<Target>();
    private int score = 0;

    private Background background;
    private float backgroundPosition = 0;

    private int tickCount = 0;

    public GameView(Context context) {
        super(context);
        ourHolder = getHolder();
        paint = new Paint();
        playing = true;

    }

    @Override
    public void run()
    {
        while (playing) {
            long startTime = System.currentTimeMillis();

            tick();

            if(ourHolder.getSurface().isValid()) {
                draw();
            }

            long endTime = System.currentTimeMillis();
            long deltaTime = (long) (frameTime - (endTime - startTime));
            fps = 1000.0 / (endTime - startTime)*1.0;
            try {
                if(deltaTime < 0)
                    deltaTime = 0;
                Thread.sleep(deltaTime);
            } catch (InterruptedException e) {
            }
        }
    }

    private void updateGameDimensions(){
        gameWidth = canvas.getWidth();
        gameHeight = canvas.getHeight();
        playerY = gameHeight - 260;
        playerX = gameWidth / 2;

        background = new Background(gameWidth, gameHeight);

        for(int i = 0; i < gameHeight; i+=2){
            spawnStars(i);
        }

    }

    public void draw() {
        canvas = ourHolder.lockCanvas();
        if(gameWidth == 0 || gameHeight == 0)
            updateGameDimensions();

        canvas.drawColor(BACKGROUND_COLOR);

        drawBackground();
        drawParticles();
        drawTargets();

        paint.setColor(Color.WHITE);
        canvas.drawRect(new Rect((int)(playerX-50), (int)playerY, (int)(playerX-50)+100, (int)playerY+100), paint);


        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("FPS: " + fps, 20, 40, paint);
        canvas.drawText("trgt: " + playerTarget, 20, 80, paint);
        canvas.drawText("run: " + tickCount, 20, 120, paint);
        canvas.drawText("spd: " + getTargetSpeed(), 20, 160, paint);
        canvas.drawText("particles: " + particles.size(), 20, 200, paint);
        canvas.drawText(background.getStats(), 20, 240, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(80);
        canvas.drawText(score+"", gameWidth-100, 100, paint);
        ourHolder.unlockCanvasAndPost(canvas);
    }

    private void drawBackground(){
        int startI = (int)(-backgroundPosition/background.getYPosition(-1));
        ArrayList<ArrayList<BackgroundTile>> chunks = background.getChunks(startI);
        int i = 0;
        for(ArrayList<BackgroundTile> tiles : chunks) {
            for(BackgroundTile tile : tiles){
                paint.setColor(tile.getColor());
                Path poly = tile.getPoly();
                poly.offset(0, gameHeight+background.getYPosition(i)+backgroundPosition%background.getYPosition(-1));
                canvas.drawPath(poly, paint);
            }
            i++;
        }
    }

    private void drawParticles(){
        for(Particle particle : particles){
            float progress = ((particle.getLifeSpan()-particle.getAge())*1.0F / particle.getLifeSpan()*1.0F);
            paint.setColor(particle.getColor(progress));
            canvas.drawCircle(particle.getX(), particle.getY(), progress*particle.getSize(), paint);
        }
    }

    private void drawTargets(){
        for(Target target : liveTargets){
            paint.setColor(Color.RED);
            if(target.isBenign())
                paint.setColor(Color.GREEN);
            canvas.drawCircle(target.getX(), target.getY(), 20, paint);
        }
    }

    public void updateInput(float velocityX, float velocityY){

        float absX = Math.abs(velocityX);
        float absY = Math.abs(velocityY);

        //playerDeltaX += velocityX/2000;
        //playerDeltaY += velocityY/2000;
        //sidewards movement
        if(absX > absY){
            Log.d(DEBUG_TAG, "flick sidewards w/ velocity " + velocityX);
           // playerDeltaX += velocityX/2000;
            if(velocityX > 0 && playerTarget < 1)
                playerTarget += 1;
            if(velocityX < 0 && playerTarget > -1)
                playerTarget -= 1;
            // vertical movement

            if (playerX > laneToX(playerTarget)) {
                playerDeltaX = -30;
            }
            if (playerX < laneToX(playerTarget)) {
                playerDeltaX = 30;
            }

        }else{
            Log.d(DEBUG_TAG, "flick verticla w/ velocity " + velocityY);
          //  playerDeltaY += velocityY/2000;
        }
    }

    public void tick() {
        tickCount++;

        if(playerY+100 > gameHeight || playerY < 0)
            playerDeltaY = -playerDeltaY;

        if(playerDeltaX > 0 && playerX > laneToX(playerTarget) || playerDeltaX < 0 && playerX < laneToX(playerTarget)){
            playerDeltaX = 0;
            playerX = laneToX(playerTarget);
        }


        playerX += playerDeltaX;

        playerY += playerDeltaY;

        playerDeltaY *= 0.995;

        if(gameWidth > 0) {

            spawnParticles();
            updateParticles();

            spawnTargets();
            updateTargets();

        }

        backgroundPosition += Math.sqrt(getTargetSpeed())/4;
    }

    private int laneToX(int lane){
        return gameWidth/2 + (gameWidth/4) * lane;
    }

    private void spawnParticles(){

        Random rand = new Random();

        //spawn exhaust
        for (int i=0; i < rand.nextInt(9); i++){
            float x = playerX-40+rand.nextInt(80);
            float y = playerY+80;
            float deltaX = -playerDeltaX * (rand.nextFloat()*0.08F) + (rand.nextFloat()*2.0F - 1.0F);
            float deltaY = -playerDeltaY * (rand.nextFloat()*0.08F) + (rand.nextFloat()*2.0F - 1.0F);
            int lifespan = rand.nextInt(200);
            Particle particle = new Particle(x, y, deltaX, deltaY, lifespan, rand.nextInt(255), 20, true);
            particles.add(particle);
        }

        // spawn stars
        spawnStars(0);

    }

    private void spawnStars(int y){
        Random rand = new Random();
        if(rand.nextFloat() < 0.1) {
            float x = rand.nextInt(gameWidth);
            int depth = rand.nextInt(3) + 1;
            float deltaY = depth*depth*getTargetSpeed()/30f;
            Particle particle = new Particle(x, y, 0, deltaY, 20000, 128+rand.nextInt(127), depth, false);
            particles.add(particle);
        }
    }

    private void updateParticles(){
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.tick(gameHeight);

            if(particle.isDead())
                iterator.remove();
        }

    }

    private void spawnTargets(){
        if(tickCount%200 != 0)
            return;

        Random rand = new Random();
        int benignTargetNum = rand.nextInt(NUM_LANES);
        for(int i=0; i < NUM_LANES; i++){
            int x = laneToX(i-1);
            int y = -rand.nextInt(gameHeight/3);
            Target target = new Target(x, y, benignTargetNum == i);
            liveTargets.add(target);
        }
    }

    private void updateTargets(){
        Iterator<Target> iterator = liveTargets.iterator();
        while (iterator.hasNext()) {
            Target target = iterator.next();
            target.tick(getTargetSpeed());

            if(target.getY() > playerY-5 && target.getDistance(playerX, target.getY()) < 50){
                if(target.isBenign())
                    score++;
                else
                    score -= 5;
                iterator.remove();
                continue;
            }

            if(target.getY() > gameHeight)
                iterator.remove();
        }

    }

    private int getTargetSpeed(){
        return (int)(4 + Math.sqrt(tickCount*0.4)* 0.3);
    }

    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }

    }

    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

}