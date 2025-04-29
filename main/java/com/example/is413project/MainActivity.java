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
        Button submitButton = findViewById(R.id.submit_button);

        try {
            digitModel = new Interpreter(loadModelFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model.", e);
        }

        submitButton.setOnClickListener(v -> handleDrawingSubmission());

        initBirdList();
        showNewQuestion();
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

        for (int i = 0; i < 4; i++) {
            Bird bird = choices.get(i);
            answerButtons[i].setImageResource(bird.getImageResourceId());

            answerButtons[i].setOnClickListener(view -> {
                if (bird.equals(correctBird)) {
                    Toast.makeText(MainActivity.this, "Correct!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Incorrect!", Toast.LENGTH_SHORT).show();
                }
                showNewQuestion();
            });
        }
    }
}
