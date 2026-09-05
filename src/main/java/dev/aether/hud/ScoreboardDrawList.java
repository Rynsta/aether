package dev.aether.hud;

import dev.aether.renderer.NVGRenderer;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

final class ScoreboardDrawList {
    private final List<Command> commands = new ArrayList<>();
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
        commands.add(nvg -> nvg.rect(x, y, r - x, b - y, color));
    }

    void text(Font.PreparedText text) {
        commands.add(nvg -> nvg.minecraftText(text));
    }

    boolean isEmpty() { return left == Integer.MAX_VALUE; }
    int left() { return isEmpty() ? 0 : left; }
    int top() { return isEmpty() ? 0 : top; }
    int width() { return isEmpty() ? 0 : right - left; }
    int height() { return isEmpty() ? 0 : bottom - top; }

    void render(NVGRenderer nvg) {
        nvg.save();
        try {
            nvg.translate(-left(), -top());
            for (Command command : commands) command.render(nvg);
        } finally {
            nvg.restore();
        }
    }

    private interface Command {
        void render(NVGRenderer nvg);
    }
}
