package com.example.starnav;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    private ViewPager2 instructionPager;
    private Button btnNext;
    private Button btnSkip;
    private ImageView starBg1;
    private ImageView starBg2;
    private ImageView starBg3;
    private LinearLayout dotsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);
        Log.d("WelcomeActivity", "Starting activity...");
        try {
            // Ваш текущий код
        } catch (Exception e) {
            Log.e("WelcomeActivity", "Error: " + e.getMessage());
            e.printStackTrace();
        }

        // Инициализация элементов
        instructionPager = findViewById(R.id.instructionPager);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        starBg1 = findViewById(R.id.star_bg_1);
        starBg2 = findViewById(R.id.star_bg_2);
        starBg3 = findViewById(R.id.star_bg_3);
        dotsContainer = findViewById(R.id.dotsContainer);

        // Настройка ViewPager
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new Instruction(R.string.instruction_1, R.drawable.ic_instruction_1));
        instructions.add(new Instruction(R.string.instruction_2, R.drawable.ic_instruction_2));
        instructions.add(new Instruction(R.string.instruction_3, R.drawable.ic_instruction_3));
        instructions.add(new Instruction(R.string.instruction_4, R.drawable.ic_instruction_4));

        InstructionAdapter adapter = new InstructionAdapter(instructions);
        instructionPager.setAdapter(adapter);

        // Настройка индикатора точек
        setupDotsIndicator(instructions.size());

        // Параллакс-эффект для звёздного фона
        instructionPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                float offset = position + positionOffset;

                // Разная скорость движения для разных слоёв звёзд
                starBg1.setTranslationX(offset * 30);
                starBg1.setTranslationY(offset * -20);

                starBg2.setTranslationX(offset * -40);
                starBg2.setTranslationY(offset * 15);

                starBg3.setTranslationX(offset * 50);
                starBg3.setTranslationY(offset * -30);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position);

                // Обновление текста кнопки
                if (position == instructions.size() - 1) {
                    btnNext.setText(R.string.skip);
                } else {
                    btnNext.setText(R.string.next);
                }
            }
        });

        // Обработчики кнопок
        btnNext.setOnClickListener(v -> {
            if (instructionPager.getCurrentItem() < instructions.size() - 1) {
                instructionPager.setCurrentItem(instructionPager.getCurrentItem() + 1);
            } else {
                Intent intent = new Intent(this, RegistrationActivity.class);
                startActivity(intent);
                finish(); // Закрываем WelcomeActivity
            }
        });

        btnSkip.setOnClickListener(v -> {
            //startActivity(new Intent(this, ApiKeyActivity.class));
            finish();
        });
    }


    private void updateDots(int position) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            ImageView dot = (ImageView) dotsContainer.getChildAt(i);
            if (i == position) {
                dot.setImageResource(R.drawable.dot_active);
            } else {
                dot.setImageResource(R.drawable.dot_inactive);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    static class Instruction {
        private int textRes;
        private int iconRes;

        public Instruction(int textRes, int iconRes) {
            this.textRes = textRes;
            this.iconRes = iconRes;
        }

        public int getTextRes() {
            return textRes;
        }

        public int getIconRes() {
            return iconRes;
        }
    }

    private void setupDotsIndicator(int count) {
        dotsContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(8),
                    dpToPx(8)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setImageResource(R.drawable.dot_inactive);
            dotsContainer.addView(dot);
        }
        updateDots(0);
    }

    private static class InstructionAdapter extends RecyclerView.Adapter<InstructionAdapter.ViewHolder> {

        private List<Instruction> instructions;

        public InstructionAdapter(List<Instruction> instructions) {
            this.instructions = instructions;
        }

        // ViewHolder ДОЛЖЕН быть static!
        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView instructionText;
            ImageView instructionIcon;

            public ViewHolder(View itemView) {
                super(itemView);
                // Убедитесь, что ID совпадают с item_instruction.xml
                instructionText = itemView.findViewById(R.id.instructionText);
                instructionIcon = itemView.findViewById(R.id.instructionIcon);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_instruction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Instruction instruction = instructions.get(position);
            holder.instructionText.setText(instruction.getTextRes());
            holder.instructionIcon.setImageResource(instruction.getIconRes());

            // Убедимся, что макет заполняет весь родительский контейнер
            holder.itemView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        @Override
        public int getItemCount() {
            return instructions.size();
        }
    }
}