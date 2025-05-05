package com.example.is413project;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nex3z.fingerpaintview.FingerPaintView;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView questionText;
    private ImageButton[] answerButtons = new ImageButton[4];

    private List<Bird> allBirds = new ArrayList<>();
    private Bird correctBird;
    private FingerPaintView drawingArea;
    private Interpreter digitModel;

    //Rachel Code Below
    // Score Tracking and a play again button
    private int score = 0;
    private int questionCount = 0;
    private final int MAX_QUESTIONS = 10;

    private TextView scoreText;
    private Button playAgainButton;

    //Rachel Code Below
    // Reference to the "Submit" button that triggers digit classification.
    // Hidden when the game ends, and shown again when a new game begins.
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        questionText = findViewById(R.id.questionText);
        answerButtons[0] = findViewById(R.id.option1);
        answerButtons[1] = findViewById(R.id.option2);
        answerButtons[2] = findViewById(R.id.option3);
        answerButtons[3] = findViewById(R.id.option4);

        drawingArea = findViewById(R.id.finger_paint_view);
        submitButton = findViewById(R.id.submit_button); // Initializes the Submit button for digit prediction

        try {
            digitModel = new Interpreter(loadModelFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model.", e);
        }

        submitButton.setOnClickListener(v -> handleDrawingSubmission());

        initBirdList();
        scoreText = findViewById(R.id.scoreText);
        showNewQuestion();


        playAgainButton = findViewById(R.id.play_again_button);
        playAgainButton.setVisibility(View.GONE);

        playAgainButton.setOnClickListener(v -> resetGame());
    }

    private void handleDrawingSubmission() {
        Bitmap drawing = drawingArea.exportToBitmap();
        Bitmap resizedDrawing = Bitmap.createScaledBitmap(drawing, 28, 28, true);

        ByteBuffer input = ByteBuffer.allocateDirect(4 * 28 * 28);
        input.order(ByteOrder.nativeOrder());

        for (int y = 0; y < 28; y++) {
            for (int x = 0; x < 28; x++) {
                int pixel = resizedDrawing.getPixel(x, y);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;
                float grayScale = (red * 0.299f + green * 0.587f + blue * 0.114f) / 255f;
                input.putFloat(1.0f - grayScale); // invert for MNIST
            }
        }

        input.rewind();

        float[][] output = new float[1][10];
        digitModel.run(input, output);

        int bestGuess = -1;
        float highestConfidence = 0f;
        for (int i = 0; i < 10; i++) {
            if (output[0][i] > highestConfidence) {
                highestConfidence = output[0][i];
                bestGuess = i;
            }
        }

        if (bestGuess >= 1 && bestGuess <= 4) {
            answerButtons[bestGuess - 1].performClick();
        } else {
            Toast.makeText(this, "Draw a number between 1 and 4.", Toast.LENGTH_SHORT).show();
        }

        drawingArea.clear();
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        try (AssetFileDescriptor file = getAssets().openFd("digit.tflite");
             FileInputStream stream = new FileInputStream(file.getFileDescriptor());
             FileChannel channel = stream.getChannel()) {
            return channel.map(FileChannel.MapMode.READ_ONLY, file.getStartOffset(), file.getLength());
        }
    }

    private void initBirdList() {
        allBirds.add(new Bird("Sparrow", R.drawable.sparrow));
        allBirds.add(new Bird("Robin", R.drawable.robin));
        allBirds.add(new Bird("Blue Jay", R.drawable.bluejay));
        allBirds.add(new Bird("Cardinal", R.drawable.cardinal));
        allBirds.add(new Bird("Eagle", R.drawable.eagle));
        allBirds.add(new Bird("Hawk", R.drawable.hawk));
        allBirds.add(new Bird("Owl", R.drawable.owl));
        allBirds.add(new Bird("Woodpecker", R.drawable.woodpecker));
    }

    private void showNewQuestion() {

        //Rachel Code Below
        //Limiting questions to 10 per set
        //Display point total out of 10 when game ends
        if (questionCount >= MAX_QUESTIONS) {
            Toast.makeText(this, "Game over! Final score: " + score + "/" + MAX_QUESTIONS, Toast.LENGTH_LONG).show();
            for (ImageButton button : answerButtons) {
                button.setEnabled(false);
            }
            submitButton.setVisibility(View.GONE);     // HIDE Submit button
            playAgainButton.setVisibility(View.VISIBLE); // SHOW Play Again button
            return;
        }

        questionCount++;
        scoreText.setText("Score: " + score + "/" + questionCount);


        Random random = new Random();
        correctBird = allBirds.get(random.nextInt(allBirds.size()));
        questionText.setText("Find the " + correctBird.getName() + "!");

        List<Bird> choices = new ArrayList<>();
        choices.add(correctBird);

        while (choices.size() < 4) {
            Bird candidate = allBirds.get(random.nextInt(allBirds.size()));
            if (!choices.contains(candidate)) {
                choices.add(candidate);
            }
        }

        Collections.shuffle(choices);

        //Rachel: Create a final copy of the bird inside the loop so the click listener uses the correct value.
        // Without this, all listeners might reference the last bird due to how lambdas capture variables.
        for (int i = 0; i < 4; i++) {
            final Bird bird = choices.get(i); // 👈 capture final value per iteration
            answerButtons[i].setImageResource(bird.getImageResourceId());

            answerButtons[i].setOnClickListener(view -> {
                if (bird.equals(correctBird)) {
                    score++;
                    Toast.makeText(MainActivity.this, "Correct!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Incorrect!", Toast.LENGTH_SHORT).show();
                }
                showNewQuestion();
            });
        }

    }
    private void resetGame() {
        score = 0;
        questionCount = 0;
        playAgainButton.setVisibility(View.GONE);
        submitButton.setVisibility(View.VISIBLE);  // SHOW Submit again
        for (ImageButton button : answerButtons) {
            button.setEnabled(true);
        }
        scoreText.setText("Score: 0/0");
        showNewQuestion();
    }
}
