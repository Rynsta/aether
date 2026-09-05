package dev.aether.hud;

import dev.aether.renderer.NVGRenderer;
import dev.aether.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

final class ScoreboardDrawList {
    static final float PADDING = 10f;
    private final List<Line> lines = new ArrayList<>();
    private int left = Integer.MAX_VALUE;
    private int top = Integer.MAX_VALUE;
    private int right = Integer.MIN_VALUE;
    private int bottom = Integer.MIN_VALUE;

    void fill(int x1, int y1, int x2, int y2, int color) {
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int r = Math.max(x1, x2);
        int b = Math.max(y1, y2);
        left = Math.min(left, x);
        top = Math.min(top, y);
        right = Math.max(right, r);
        bottom = Math.max(bottom, b);
    }

    void text(ScoreboardText text, int x, int y, int vanillaWidth) {
        Alignment alignment = lines.isEmpty() ? Alignment.CENTER : x == left + 2 ? Alignment.LEFT : Alignment.RIGHT;
        lines.add(new Line(text, x, y, vanillaWidth, alignment));
    }

    boolean isEmpty() { return left == Integer.MAX_VALUE; }
    int left() { return isEmpty() ? 0 : left; }
    int top() { return isEmpty() ? 0 : top; }
    int width() { return isEmpty() ? 0 : right - left; }
    int height() { return isEmpty() ? 0 : bottom - top; }
    float panelWidth() { return isEmpty() ? 0 : width() + PADDING * 2; }
    float panelHeight() { return isEmpty() ? 0 : height() + PADDING * 2; }

    void render(NVGRenderer nvg) {
        if (isEmpty()) return;
        HudStyle.panel(nvg, panelWidth(), panelHeight());
        HudStyle.accent(nvg, panelWidth(), Theme.HUD_ACCENT, Theme.HUD_ACCENT);
        nvg.save();
        try {
            nvg.translate(PADDING - left(), PADDING - top());
            for (int i = 0; i < lines.size(); i++) {
                Line line = lines.get(i);
                boolean title = line.alignment() == Alignment.CENTER;
                float available = title ? width() - 4 : line.alignment() == Alignment.RIGHT ? line.vanillaWidth() : right - line.x();
                if (line.alignment() == Alignment.LEFT && i + 1 < lines.size()) {
                    Line score = lines.get(i + 1);
                    if (score.y() == line.y() && score.vanillaWidth() > 0) available = score.x() - line.x() - 2;
                }
                float textWidth = Math.min(available, line.text().width(nvg, title));
                float x = switch (line.alignment()) {
                    case LEFT -> line.x();
                    case CENTER -> line.x() + line.vanillaWidth() / 2f - textWidth / 2f;
                    case RIGHT -> line.x() + line.vanillaWidth() - textWidth;
                };
                line.text().render(nvg, x, line.y(), available, title);
            }
        } finally {
            nvg.restore();
        }
    }

    private enum Alignment { LEFT, CENTER, RIGHT }
    private record Line(ScoreboardText text, int x, int y, int vanillaWidth, Alignment alignment) { }
}
