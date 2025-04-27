package com.example.is413project;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView questionText;
    private ImageButton[] optionButtons = new ImageButton[4];

    private List<Bird> birds = new ArrayList<>();
    private Bird correctBird;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        questionText = findViewById(R.id.questionText);
        optionButtons[0] = findViewById(R.id.option1);
        optionButtons[1] = findViewById(R.id.option2);
        optionButtons[2] = findViewById(R.id.option3);
        optionButtons[3] = findViewById(R.id.option4);

        // Initialize bird data
        loadBirds();

        // Start first question
        loadNewQuestion();
    }

    private void loadBirds() {
        // For example purposes, using drawable resource IDs
        birds.add(new Bird("Sparrow", R.drawable.sparrow));
        birds.add(new Bird("Robin", R.drawable.robin));
        birds.add(new Bird("Blue Jay", R.drawable.bluejay));
        birds.add(new Bird("Cardinal", R.drawable.cardinal));
        birds.add(new Bird("Eagle", R.drawable.eagle));
        birds.add(new Bird("Hawk", R.drawable.hawk));
        birds.add(new Bird("Owl", R.drawable.owl));
        birds.add(new Bird("Woodpecker", R.drawable.woodpecker));
        // Add more birds and images as needed
    }

    private void loadNewQuestion() {
        // Pick a random correct bird
        Random rand = new Random();
        correctBird = birds.get(rand.nextInt(birds.size()));

        questionText.setText("Which bird is a " + correctBird.getName() + "?");

        // Prepare 4 options including correct bird
        List<Bird> options = new ArrayList<>();
        options.add(correctBird);

        while (options.size() < 4) {
            Bird randomBird = birds.get(rand.nextInt(birds.size()));
            if (!options.contains(randomBird)) {
                options.add(randomBird);
            }
        }

        // Shuffle options
        Collections.shuffle(options);

        // Set images to buttons
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