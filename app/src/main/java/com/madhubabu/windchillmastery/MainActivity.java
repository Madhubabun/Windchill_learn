package com.madhubabu.windchillmastery;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout bottom;
    private JSONArray modules;
    private JSONArray lessons;
    private JSONArray quizzes;
    private SharedPreferences prefs;
    private boolean lightTheme;
    private String lastSearchQuery = "";
    private Runnable backAction;
    private Runnable redrawAction;
    private Runnable lessonBackAction;

    private int accent;
    private int accentSoft;
    private int surface;
    private int surface2;
    private int text;
    private int muted;
    private int bg;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("progress", MODE_PRIVATE);
        lightTheme = prefs.getBoolean("light_theme", false);
        applyPalette();
        loadData();
        buildShell();
        showHome();
    }

    private void loadData() {
        try (InputStream input = getAssets().open("windchill_content.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            JSONObject data = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            modules = data.getJSONArray("modules");
            lessons = data.getJSONArray("lessons");
            quizzes = data.getJSONArray("quizzes");
        } catch (Exception error) {
            throw new IllegalStateException("Could not load the bundled offline curriculum.", error);
        }
    }

    private void applyPalette() {
        if (lightTheme) {
            bg = Color.rgb(246, 247, 252);
            surface = Color.WHITE;
            surface2 = Color.rgb(239, 237, 255);
            text = Color.rgb(28, 32, 50);
            muted = Color.rgb(97, 104, 124);
            accent = Color.rgb(91, 71, 222);
            accentSoft = Color.rgb(233, 230, 255);
        } else {
            bg = Color.rgb(11, 16, 32);
            surface = Color.rgb(20, 27, 45);
            surface2 = Color.rgb(27, 36, 64);
            text = Color.rgb(245, 247, 255);
            muted = Color.rgb(170, 180, 207);
            accent = Color.rgb(139, 123, 255);
            accentSoft = Color.rgb(38, 37, 76);
        }
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        int systemUi = lightTheme
                ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                : 0;
        getWindow().getDecorView().setSystemUiVisibility(systemUi);
        if (root != null) {
            root.setBackgroundColor(bg);
        }
        if (bottom != null) {
            bottom.setBackgroundColor(surface);
        }
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(14), dp(18), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(8), dp(8), dp(8), dp(8));
        bottom.setBackgroundColor(surface);
        addNav("⌂", "Home", v -> showHome());
        addNav("▣", "Learn", v -> showLearn());
        addNav("✓", "Practice", v -> showPractice());
        addNav("⌕", "Search", v -> showSearch());
        root.addView(bottom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));

        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                topInset = systemBars.top;
                bottomInset = systemBars.bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(0, topInset, 0, 0);
            bottom.setPadding(dp(8), dp(8), dp(8), dp(8) + bottomInset);
            ViewGroup.LayoutParams navParams = bottom.getLayoutParams();
            navParams.height = dp(68) + bottomInset;
            bottom.setLayoutParams(navParams);
            return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : insets.consumeSystemWindowInsets();
        });

        setContentView(root);
        root.requestApplyInsets();
    }

    private void addNav(String icon, String label, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setFocusable(true);
        TextView iconView = tv(icon, 22, muted);
        TextView labelView = tv(label, 11, muted);
        item.addView(iconView);
        item.addView(labelView);
        item.setOnClickListener(listener);
        bottom.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void setActiveTab(String tab) {
        if (bottom == null) {
            return;
        }
        for (int i = 0; i < bottom.getChildCount(); i++) {
            View child = bottom.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout item = (LinearLayout) child;
                int color = item.getChildAt(1) instanceof TextView
                        && ((TextView) item.getChildAt(1)).getText().toString().equals(tab)
                        ? accent : muted;
                ((TextView) item.getChildAt(0)).setTextColor(color);
                ((TextView) item.getChildAt(1)).setTextColor(color);
            }
        }
    }

    private void clear() {
        content.removeAllViews();
    }

    private TextView tv(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setFontFeatureSettings("kern");
        return view;
    }

    private TextView heading(String value, float size) {
        TextView view = tv(value, size, text);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(8), 0, dp(6));
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private TextView pill(String label) {
        TextView view = tv(label, 11, accent);
        view.setPadding(dp(10), dp(5), dp(10), dp(5));
        view.setBackground(rounded(accentSoft, 30));
        return view;
    }

    private TextView button(String label, View.OnClickListener listener) {
        TextView view = tv(label, 13, Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(10), dp(4), dp(10), dp(4));
        view.setBackground(rounded(accent, 16));
        view.setOnClickListener(listener);
        view.setMinHeight(dp(46));
        view.setFocusable(true);
        return view;
    }

    private TextView secondaryButton(String label, View.OnClickListener listener) {
        TextView view = tv(label, 13, accent);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(10), dp(4), dp(10), dp(4));
        view.setBackground(rounded(accentSoft, 16));
        view.setOnClickListener(listener);
        view.setMinHeight(dp(46));
        view.setFocusable(true);
        return view;
    }

    private void top(String title, String subtitle) {
        top(title, subtitle, null);
    }

    private void top(String title, String subtitle, Runnable back) {
        clear();
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (back != null) {
            TextView backButton = secondaryButton("←", v -> back.run());
            LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(44), dp(44));
            backParams.setMargins(0, 0, dp(8), 0);
            row.addView(backButton, backParams);
        }
        TextView titleView = heading(title, 24);
        row.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
        TextView themeButton = secondaryButton(lightTheme ? "☾" : "☀", v -> toggleTheme());
        themeButton.setContentDescription(lightTheme ? "Switch to dark theme" : "Switch to light theme");
        row.addView(themeButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        content.addView(row);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = tv(subtitle, 14, muted);
            sub.setLineSpacing(0, 1.1f);
            sub.setPadding(0, dp(2), 0, dp(14));
            content.addView(sub);
        }
    }

    private void toggleTheme() {
        lightTheme = !lightTheme;
        prefs.edit().putBoolean("light_theme", lightTheme).apply();
        applyPalette();
        if (redrawAction != null) {
            redrawAction.run();
        }
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setBackground(rounded(surface, 20));
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(6), 0, dp(6));
        view.setLayoutParams(params);
        return view;
    }

    private void addSectionTitle(String label) {
        content.addView(heading(label, 19));
    }

    private void showHome() {
        backAction = null;
        lessonBackAction = this::showHome;
        redrawAction = this::showHome;
        setActiveTab("Home");
        top("Windchill Mastery", "A source-mapped learning path that works offline.");

        int completed = countDone();
        int total = lessons.length();
        int percent = total == 0 ? 0 : completed * 100 / total;
        LinearLayout hero = card();
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.addView(tv("YOUR LEARNING PATH", 12, muted));
        TextView progressText = heading(percent + "% complete", 27);
        progressText.setPadding(0, dp(4), 0, dp(8));
        hero.addView(progressText);
        hero.addView(tv(completed + " of " + total + " lessons completed", 13, muted));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(total, 1));
        progress.setProgress(completed);
        progress.setProgressTintList(ColorStateList.valueOf(accent));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(8));
        progressParams.topMargin = dp(12);
        hero.addView(progress, progressParams);
        TextView practiceStats = tv(
                countQuizCorrect() + " correct of " + countQuizAttempts() + " practice checks attempted",
                12, muted);
        practiceStats.setPadding(0, dp(12), 0, 0);
        hero.addView(practiceStats);
        content.addView(hero);

        addSectionTitle("Continue learning");
        JSONObject next = nextLesson();
        if (next != null) {
            addLessonCard(next, true);
        } else {
            addMessageCard("Path complete", "You have marked every lesson as complete. Revisit any module or use Practice to review.");
        }

        addSectionTitle("Recommended topics");
        List<JSONObject> nextTopics = nextLessons(next, 2);
        if (nextTopics.isEmpty()) {
            addMessageCard("Review at your pace", "Open a module to revisit lessons from the supplied curriculum.");
        } else {
            for (JSONObject lesson : nextTopics) {
                addLessonCard(lesson, false);
            }
        }

        List<JSONObject> recent = recentLessons(3);
        if (!recent.isEmpty()) {
            addSectionTitle("Recently studied");
            for (JSONObject lesson : recent) {
                addLessonCard(lesson, false);
            }
        }

        List<JSONObject> saved = savedLessons(3);
        addSectionTitle("Saved for later");
        if (saved.isEmpty()) {
            addMessageCard("No bookmarks yet", "Bookmark a lesson to keep it close at hand.");
        } else {
            TextView allSaved = secondaryButton("View all saved lessons", v -> showBookmarks());
            content.addView(allSaved);
            for (JSONObject lesson : saved) {
                addLessonCard(lesson, false);
            }
        }

        addSectionTitle("Curriculum");
        for (int i = 0; i < modules.length(); i++) {
            try {
                addModuleCard(modules.getJSONObject(i));
            } catch (Exception ignored) {
            }
        }
        TextView footer = tv(
                lessons.length() + " lessons · " + modules.length() + " modules · "
                        + quizzes.length() + " local practice checks · no account or network required",
                11, muted);
        footer.setLineSpacing(0, 1.15f);
        footer.setPadding(0, dp(18), 0, dp(12));
        content.addView(footer);
    }

    private List<JSONObject> nextLessons(JSONObject first, int limit) {
        List<JSONObject> result = new ArrayList<>();
        if (first == null) {
            return result;
        }
        try {
            int start = 0;
            for (int i = 0; i < lessons.length(); i++) {
                if (lessons.getJSONObject(i).getString("id").equals(first.getString("id"))) {
                    start = i + 1;
                    break;
                }
            }
            for (int i = start; i < lessons.length() && result.size() < limit; i++) {
                JSONObject lesson = lessons.getJSONObject(i);
                if (!isLessonDone(lesson)) {
                    result.add(lesson);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private void addMessageCard(String title, String message) {
        LinearLayout view = card();
        view.addView(heading(title, 16));
        TextView body = tv(message, 13, muted);
        body.setLineSpacing(0, 1.15f);
        view.addView(body);
        content.addView(view);
    }

    private void addModuleCard(JSONObject module) {
        try {
            String moduleId = module.getString("id");
            int total = moduleLessonCount(moduleId);
            int done = moduleDoneCount(moduleId);
            LinearLayout view = card();
            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(pill(module.getString("number")));
            TextView title = heading(module.getString("title"), 16);
            title.setPadding(dp(10), 0, 0, 0);
            header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            view.addView(header);
            TextView description = tv(module.getString("description"), 13, muted);
            description.setLineSpacing(0, 1.1f);
            description.setPadding(0, dp(8), 0, dp(6));
            view.addView(description);
            view.addView(tv(done + " / " + total + " lessons · " + modulePercent(moduleId) + "% complete",
                    12, accent));
            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(Math.max(total, 1));
            progress.setProgress(done);
            progress.setProgressTintList(ColorStateList.valueOf(accent));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(5));
            params.topMargin = dp(8);
            view.addView(progress, params);
            view.setFocusable(true);
            view.setOnClickListener(v -> showModule(module));
            content.addView(view);
        } catch (Exception ignored) {
        }
    }

    private int moduleLessonCount(String moduleId) {
        int count = 0;
        for (int i = 0; i < lessons.length(); i++) {
            if (lessons.optJSONObject(i) != null
                    && lessons.optJSONObject(i).optString("moduleId").equals(moduleId)) {
                count++;
            }
        }
        return count;
    }

    private int moduleDoneCount(String moduleId) {
        int count = 0;
        for (int i = 0; i < lessons.length(); i++) {
            JSONObject lesson = lessons.optJSONObject(i);
            if (lesson != null && lesson.optString("moduleId").equals(moduleId) && isLessonDone(lesson)) {
                count++;
            }
        }
        return count;
    }

    private int modulePercent(String moduleId) {
        int total = moduleLessonCount(moduleId);
        return total == 0 ? 0 : moduleDoneCount(moduleId) * 100 / total;
    }

    private void showLearn() {
        backAction = null;
        lessonBackAction = this::showLearn;
        redrawAction = this::showLearn;
        setActiveTab("Learn");
        top("Learn", "Follow the supplied curriculum from foundational topics toward advanced material.");
        for (int i = 0; i < modules.length(); i++) {
            try {
                addModuleCard(modules.getJSONObject(i));
            } catch (Exception ignored) {
            }
        }
    }

    private void showModule(JSONObject module) {
        setActiveTab("Learn");
        lessonBackAction = () -> showModule(module);
        redrawAction = () -> showModule(module);
        backAction = this::showLearn;
        try {
            top(module.getString("number") + "  " + module.getString("title"),
                    module.getString("description"), backAction);
            TextView moduleProgress = tv(
                    moduleDoneCount(module.getString("id")) + " of "
                            + moduleLessonCount(module.getString("id")) + " lessons complete",
                    13, accent);
            content.addView(moduleProgress);
            String moduleId = module.getString("id");
            for (int i = 0; i < lessons.length(); i++) {
                JSONObject lesson = lessons.getJSONObject(i);
                if (lesson.getString("moduleId").equals(moduleId)) {
                    addLessonCard(lesson, false);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void addLessonCard(JSONObject lesson, boolean featured) {
        try {
            LinearLayout view = card();
            if (featured) {
                view.setBackground(rounded(surface2, 20));
            }
            TextView section = tv(lesson.optString("sectionTitle", "Lesson"), 11, accent);
            view.addView(section);
            TextView title = heading(lesson.getString("title"), featured ? 18 : 16);
            view.addView(title);
            String meta = lessonLevel(lesson) + "  ·  " + compactArray(lesson.optJSONArray("releases"));
            view.addView(tv(meta, 12, muted));
            if (isLessonDone(lesson) || isBookmarked(lesson)) {
                String state = (isLessonDone(lesson) ? "✓ Complete" : "")
                        + (isLessonDone(lesson) && isBookmarked(lesson) ? "   ·   " : "")
                        + (isBookmarked(lesson) ? "★ Saved" : "");
                TextView status = tv(state, 11, accent);
                status.setPadding(0, dp(7), 0, 0);
                view.addView(status);
            }
            view.setFocusable(true);
            Runnable returnAction = lessonBackAction;
            view.setOnClickListener(v -> showLesson(lesson, returnAction));
            content.addView(view);
        } catch (Exception ignored) {
        }
    }

    private String lessonLevel(JSONObject lesson) {
        int tier = lesson.optInt("tier", 1);
        if (tier <= 1) {
            return "Beginner";
        }
        if (tier == 2) {
            return "Intermediate";
        }
        if (tier == 3) {
            return "Advanced";
        }
        return "Expert";
    }

    private void showLesson(JSONObject lesson, Runnable returnAction) {
        recordRecent(lesson);
        setActiveTab("Learn");
        redrawAction = () -> showLesson(lesson, returnAction);
        backAction = returnAction == null ? this::showLearn : returnAction;
        try {
            top(lesson.getString("title"),
                    lesson.getString("moduleTitle") + "  ·  " + lesson.getString("sectionTitle"),
                    backAction);

            LinearLayout focusCard = card();
            focusCard.addView(heading("Lesson focus", 17));
            TextView focusNote = tv(
                    "This entry is a source-map record. The bundled data supplies a focus and references, not full lesson prose; the notes below stay within that evidence.",
                    13, muted);
            focusNote.setLineSpacing(0, 1.15f);
            focusCard.addView(focusNote);
            content.addView(focusCard);

            LinearLayout sourceCard = card();
            sourceCard.addView(heading("Source focus", 17));
            TextView sourceFocus = tv(lesson.getString("sourceFocus"), 13, text);
            sourceFocus.setLineSpacing(0, 1.15f);
            sourceCard.addView(sourceFocus);
            content.addView(sourceCard);

            LinearLayout references = card();
            references.addView(heading("Source references", 17));
            JSONArray sourceIds = lesson.optJSONArray("sourceIds");
            if (sourceIds != null && sourceIds.length() > 0) {
                for (int i = 0; i < sourceIds.length(); i++) {
                    references.addView(tv("•  " + sourceIds.optString(i), 13, text));
                }
            } else {
                references.addView(tv("No source ID is listed for this entry.", 13, muted));
            }
            TextView provenance = tv(
                    "Status: " + lesson.optString("status", "unstated")
                            + "  ·  Level: " + lessonLevel(lesson)
                            + "  ·  Releases: " + compactArray(lesson.optJSONArray("releases")),
                    12, accent);
            provenance.setLineSpacing(0, 1.15f);
            provenance.setPadding(0, dp(9), 0, 0);
            references.addView(provenance);
            content.addView(references);

            LinearLayout reflection = card();
            reflection.addView(heading("Quick recall", 17));
            TextView prompt = tv(
                    "In your own words, summarize “" + lesson.getString("title")
                            + "” using the source focus above. What would you want to verify in the listed reference?",
                    14, text);
            prompt.setLineSpacing(0, 1.15f);
            reflection.addView(prompt);
            content.addView(reflection);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            boolean complete = isLessonDone(lesson);
            TextView completeButton = button(complete ? "✓ Completed" : "Mark complete", v -> {
                prefs.edit().putBoolean("done_" + lesson.optString("id"), !isLessonDone(lesson)).apply();
                showLesson(lesson, returnAction);
            });
            TextView bookmarkButton = secondaryButton(
                    isBookmarked(lesson) ? "★ Saved" : "☆ Save",
                    v -> {
                        prefs.edit().putBoolean("saved_" + lesson.optString("id"), !isBookmarked(lesson)).apply();
                        showLesson(lesson, returnAction);
                    });
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, dp(48), 1);
            actionParams.setMargins(0, dp(8), dp(6), dp(12));
            actions.addView(completeButton, actionParams);
            LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(48), 1);
            saveParams.setMargins(dp(6), dp(8), 0, dp(12));
            actions.addView(bookmarkButton, saveParams);
            content.addView(actions);
        } catch (Exception ignored) {
        }
    }

    private void showBookmarks() {
        setActiveTab("Home");
        lessonBackAction = this::showBookmarks;
        redrawAction = this::showBookmarks;
        backAction = this::showHome;
        top("Saved lessons", "Bookmarks are stored on this device.", backAction);
        List<JSONObject> saved = savedLessons(lessons.length());
        if (saved.isEmpty()) {
            addMessageCard("Nothing saved yet", "Open a lesson and tap ☆ Save to bookmark it.");
            return;
        }
        for (JSONObject lesson : saved) {
            addLessonCard(lesson, false);
        }
    }

    private void showPractice() {
        backAction = null;
        redrawAction = this::showPractice;
        setActiveTab("Practice");
        top("Practice", "Short, source-mapped checks. Answers and scores stay on this device.");

        LinearLayout scoreCard = card();
        int attempted = countQuizAttempts();
        int correct = countQuizCorrect();
        scoreCard.addView(tv("YOUR PRACTICE SCORE", 12, muted));
        scoreCard.addView(heading(correct + " / " + attempted + " correct", 23));
        scoreCard.addView(tv(quizzes.length() + " checks available", 12, muted));
        content.addView(scoreCard);

        boolean hasMisses = false;
        for (int i = 0; i < quizzes.length(); i++) {
            if (prefs.getBoolean("quiz_done_" + i, false)
                    && !prefs.getBoolean("quiz_correct_" + i, false)) {
                if (!hasMisses) {
                    addSectionTitle("Review missed checks");
                    hasMisses = true;
                }
                addMissedQuiz(quizzes.optJSONObject(i), i);
            }
        }
        addSectionTitle("Knowledge checks");
        for (int i = 0; i < quizzes.length(); i++) {
            try {
                addQuiz(quizzes.getJSONObject(i), i);
            } catch (Exception ignored) {
            }
        }
    }

    private void addMissedQuiz(JSONObject quiz, int index) {
        if (quiz == null) {
            return;
        }
        LinearLayout view = card();
        view.addView(pill("REVIEW " + (index + 1)));
        TextView question = heading(quiz.optString("q"), 15);
        view.addView(question);
        JSONArray options = quiz.optJSONArray("options");
        int answer = quiz.optInt("a", -1);
        if (options != null && answer >= 0 && answer < options.length()) {
            TextView correctAnswer = tv("Answer: " + options.optString(answer), 13, accent);
            correctAnswer.setPadding(0, dp(4), 0, dp(4));
            view.addView(correctAnswer);
        }
        TextView explanation = tv(quiz.optString("why"), 13, muted);
        explanation.setLineSpacing(0, 1.1f);
        view.addView(explanation);
        content.addView(view);
    }

    private void addQuiz(JSONObject quiz, int index) throws Exception {
        LinearLayout view = card();
        view.addView(pill("CHECK " + (index + 1)));
        TextView question = heading(quiz.getString("q"), 16);
        view.addView(question);
        RadioGroup optionsView = new RadioGroup(this);
        JSONArray options = quiz.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            RadioButton option = new RadioButton(this);
            option.setId(View.generateViewId());
            option.setText(options.getString(i));
            option.setTextColor(text);
            option.setTextSize(14);
            option.setPadding(0, dp(4), 0, dp(4));
            optionsView.addView(option);
        }
        view.addView(optionsView);

        TextView result = tv("", 13, accent);
        result.setLineSpacing(0, 1.1f);
        result.setPadding(0, dp(6), 0, dp(8));
        view.addView(result);
        boolean alreadyAnswered = prefs.getBoolean("quiz_done_" + index, false);
        if (alreadyAnswered) {
            int previousChoice = prefs.getInt("quiz_choice_" + index, -1);
            if (previousChoice >= 0 && previousChoice < optionsView.getChildCount()) {
                optionsView.check(optionsView.getChildAt(previousChoice).getId());
            }
            boolean wasCorrect = prefs.getBoolean("quiz_correct_" + index, false);
            result.setText((wasCorrect ? "Saved result: correct. " : "Saved result: review this one. ")
                    + quiz.getString("why"));
            result.setTextColor(wasCorrect ? accent : muted);
        }

        TextView check = button("Check answer", v -> {
            int checkedId = optionsView.getCheckedRadioButtonId();
            if (checkedId == -1) {
                result.setText("Choose an answer first.");
                result.setTextColor(muted);
                return;
            }
            int selected = optionsView.indexOfChild(optionsView.findViewById(checkedId));
            boolean isCorrect = selected == quiz.optInt("a", -1);
            prefs.edit()
                    .putBoolean("quiz_done_" + index, true)
                    .putBoolean("quiz_correct_" + index, isCorrect)
                    .putInt("quiz_choice_" + index, selected)
                    .apply();
            result.setText((isCorrect ? "Correct. " : "Not quite. ") + quiz.optString("why"));
            result.setTextColor(isCorrect ? accent : muted);
        });
        view.addView(check, new LinearLayout.LayoutParams(-1, dp(46)));
        content.addView(view);
    }

    private void showSearch() {
        backAction = null;
        lessonBackAction = this::showSearch;
        redrawAction = this::showSearch;
        setActiveTab("Search");
        top("Search", "Find lesson titles, source focus, reference IDs, releases, and more.");
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Try MVC, BOM, workflow, QuerySpec…");
        input.setHintTextColor(muted);
        input.setTextColor(text);
        input.setTextSize(15);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(rounded(surface, 16));
        content.addView(input, new LinearLayout.LayoutParams(-1, dp(52)));

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        TextView prompt = tv("Enter at least two characters to search the offline curriculum.", 13, muted);
        prompt.setPadding(dp(4), dp(14), dp(4), 0);
        results.addView(prompt);
        content.addView(results);
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                lastSearchQuery = value.toString();
                results.removeAllViews();
                String query = value.toString().toLowerCase(Locale.ROOT).trim();
                if (query.length() < 2) {
                    TextView hint = tv("Enter at least two characters to search the offline curriculum.", 13, muted);
                    hint.setPadding(dp(4), dp(14), dp(4), 0);
                    results.addView(hint);
                    return;
                }
                int matches = 0;
                for (int i = 0; i < lessons.length() && matches < 40; i++) {
                    JSONObject lesson = lessons.optJSONObject(i);
                    if (lesson != null && searchText(lesson).contains(query)) {
                        addSearchResult(results, lesson);
                        matches++;
                    }
                }
                if (matches == 0) {
                    TextView empty = tv("No matching source-map entries found.", 13, muted);
                    empty.setPadding(dp(4), dp(14), dp(4), 0);
                    results.addView(empty);
                } else {
                    TextView matchCountLabel = tv(matches + (matches == 40 ? "+ matches" : " matches"), 12, muted);
                    matchCountLabel.setPadding(dp(4), dp(10), dp(4), 0);
                    results.addView(matchCountLabel, 0);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable value) {
            }
        });
        if (!lastSearchQuery.isEmpty()) {
            input.setText(lastSearchQuery);
            input.setSelection(input.getText().length());
        }
    }

    private String searchText(JSONObject lesson) {
        StringBuilder value = new StringBuilder();
        value.append(lesson.optString("title")).append(' ')
                .append(lesson.optString("moduleTitle")).append(' ')
                .append(lesson.optString("sectionTitle")).append(' ')
                .append(lesson.optString("sourceFocus")).append(' ')
                .append(lesson.optString("status")).append(' ')
                .append(lesson.optString("tier")).append(' ')
                .append(compactArray(lesson.optJSONArray("sourceIds"))).append(' ')
                .append(compactArray(lesson.optJSONArray("releases")));
        return value.toString().toLowerCase(Locale.ROOT);
    }

    private void addSearchResult(LinearLayout results, JSONObject lesson) {
        LinearLayout view = card();
        view.addView(tv(lesson.optString("sectionTitle"), 11, accent));
        view.addView(heading(lesson.optString("title"), 15));
        TextView detail = tv(lesson.optString("moduleTitle") + "  ·  " + lessonLevel(lesson), 12, muted);
        view.addView(detail);
        view.setFocusable(true);
        view.setOnClickListener(v -> showLesson(lesson, this::showSearch));
        results.addView(view);
    }

    private String compactArray(JSONArray values) {
        if (values == null || values.length() == 0) {
            return "No release listed";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(values.optString(i));
        }
        return result.toString();
    }

    private JSONObject nextLesson() {
        for (int i = 0; i < lessons.length(); i++) {
            JSONObject lesson = lessons.optJSONObject(i);
            if (lesson != null && !isLessonDone(lesson)) {
                return lesson;
            }
        }
        return null;
    }

    private int countDone() {
        int count = 0;
        for (int i = 0; i < lessons.length(); i++) {
            if (isLessonDone(lessons.optJSONObject(i))) {
                count++;
            }
        }
        return count;
    }

    private boolean isLessonDone(JSONObject lesson) {
        return lesson != null && prefs.getBoolean("done_" + lesson.optString("id"), false);
    }

    private boolean isBookmarked(JSONObject lesson) {
        return lesson != null && prefs.getBoolean("saved_" + lesson.optString("id"), false);
    }

    private int countQuizAttempts() {
        int count = 0;
        for (int i = 0; i < quizzes.length(); i++) {
            if (prefs.getBoolean("quiz_done_" + i, false)) {
                count++;
            }
        }
        return count;
    }

    private int countQuizCorrect() {
        int count = 0;
        for (int i = 0; i < quizzes.length(); i++) {
            if (prefs.getBoolean("quiz_done_" + i, false)
                    && prefs.getBoolean("quiz_correct_" + i, false)) {
                count++;
            }
        }
        return count;
    }

    private void recordRecent(JSONObject lesson) {
        String id = lesson.optString("id");
        String existing = prefs.getString("recent_lessons", "");
        String[] prior = existing.isEmpty() ? new String[0] : existing.split(",");
        StringBuilder recent = new StringBuilder(id);
        int kept = 1;
        for (String oldId : prior) {
            if (!oldId.equals(id) && kept < 8) {
                recent.append(',').append(oldId);
                kept++;
            }
        }
        prefs.edit().putString("recent_lessons", recent.toString()).apply();
    }

    private List<JSONObject> recentLessons(int limit) {
        List<JSONObject> result = new ArrayList<>();
        String recent = prefs.getString("recent_lessons", "");
        if (recent.isEmpty()) {
            return result;
        }
        for (String id : recent.split(",")) {
            JSONObject lesson = lessonForId(id);
            if (lesson != null) {
                result.add(lesson);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    private List<JSONObject> savedLessons(int limit) {
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < lessons.length(); i++) {
            JSONObject lesson = lessons.optJSONObject(i);
            if (isBookmarked(lesson)) {
                result.add(lesson);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    private JSONObject lessonForId(String id) {
        for (int i = 0; i < lessons.length(); i++) {
            JSONObject lesson = lessons.optJSONObject(i);
            if (lesson != null && lesson.optString("id").equals(id)) {
                return lesson;
            }
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if (backAction != null) {
            Runnable action = backAction;
            backAction = null;
            action.run();
        } else {
            super.onBackPressed();
        }
    }
}
