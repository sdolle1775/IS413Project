package com.example.is413project;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
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
    private ImageButton[] optionButtons = new ImageButton[4];

    private List<Bird> birds = new ArrayList<>();
    private Bird correctBird;
    private FingerPaintView fingerPaintView;
    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        questionText = findViewById(R.id.questionText);
        optionButtons[0] = findViewById(R.id.option1);
        optionButtons[1] = findViewById(R.id.option2);
        optionButtons[2] = findViewById(R.id.option3);
        optionButtons[3] = findViewById(R.id.option4);

        fingerPaintView = findViewById(R.id.finger_paint_view);
        Button submitButton = findViewById(R.id.submit_button);


        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // Submit drawing for recognition
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap drawing = fingerPaintView.exportToBitmap();
                Bitmap resized = Bitmap.createScaledBitmap(drawing, 28, 28, true);

                ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * 1 * 28 * 28 * 1); // float = 4 bytes
                inputBuffer.order(ByteOrder.nativeOrder());

                for (int y = 0; y < 28; y++) {
                    for (int x = 0; x < 28; x++) {
                        int pixel = resized.getPixel(x, y);
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        float gray = (r * 0.299f + g * 0.587f + b * 0.114f) / 255.0f;
                        gray = 1.0f - gray; // <-- FIX HERE
                        inputBuffer.putFloat(gray);
                    }
                }

                inputBuffer.rewind();

                try {
                    float[][] output = new float[1][10];
                    tflite.run(inputBuffer, output);

                    int predictedDigit = -1;
                    float maxProb = 0;
                    for (int i = 0; i < 10; i++) {
                        if (output[0][i] > maxProb) {
                            maxProb = output[0][i];
                            predictedDigit = i;
                        }
                    }

                    if (predictedDigit >= 1 && predictedDigit <= 4) {
                        int index = predictedDigit - 1;
                        optionButtons[index].performClick();
                    } else {
                        Toast.makeText(MainActivity.this, "Please draw a number between 1 and 4.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error running model: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                fingerPaintView.clear();
            }
        });

        // Initialize bird data
        loadBirds();
        // Start first question
        loadNewQuestion();
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        try (AssetFileDescriptor fileDescriptor = getAssets().openFd("digit.tflite");
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
             FileChannel fileChannel = inputStream.getChannel()) {

            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getLength();

            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }


    private void loadBirds() {
        birds.add(new Bird("Sparrow", R.drawable.sparrow));
        birds.add(new Bird("Robin", R.drawable.robin));
        birds.add(new Bird("Blue Jay", R.drawable.bluejay));
        birds.add(new Bird("Cardinal", R.drawable.cardinal));
        birds.add(new Bird("Eagle", R.drawable.eagle));
        birds.add(new Bird("Hawk", R.drawable.hawk));
        birds.add(new Bird("Owl", R.drawable.owl));
        birds.add(new Bird("Woodpecker", R.drawable.woodpecker));
    }

    private void loadNewQuestion() {
        Random rand = new Random();
        correctBird = birds.get(rand.nextInt(birds.size()));

        questionText.setText("Which bird is a " + correctBird.getName() + "?");

        List<Bird> options = new ArrayList<>();
        options.add(correctBird);

        while (options.size() < 4) {
            Bird randomBird = birds.get(rand.nextInt(birds.size()));
            if (!options.contains(randomBird)) {
                options.add(randomBird);
            }
        }

        Collections.shuffle(options);

        for (int i = 0; i < 4; i++) {
            Bird selectedBird = options.get(i);
            optionButtons[i].setImageResource(selectedBird.getImageResId());

            final boolean isCorrect = selectedBird.equals(correctBird);
            optionButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isCorrect) {
                        Toast.makeText(MainActivity.this, "Correct!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Incorrect!", Toast.LENGTH_SHORT).show();
                    }
                    loadNewQuestion();
                }
            });
        }
    }
}
