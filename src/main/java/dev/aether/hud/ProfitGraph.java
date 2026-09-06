package dev.aether.hud;

import dev.aether.modules.profit.SessionProfitHistory;
import dev.aether.renderer.NVGRenderer;
import dev.aether.ui.theme.Theme;
import dev.aether.ui.util.Fonts;
import dev.aether.util.AetherLang;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ProfitGraph {
    static final float HEIGHT = 124f;
    private final ProfitGraphScale scale = new ProfitGraphScale();
    private long generation = -1;
    private long lastWindow;
    private float[] vertices = new float[0];

    void render(NVGRenderer nvg, float x, float y, float width,
                SessionProfitHistory.Snapshot snapshot, long windowMillis, long nowNanos) {
        List<SessionProfitHistory.Point> points = visiblePoints(snapshot, windowMillis);
        double min = points.stream().mapToDouble(SessionProfitHistory.Point::coins).min().orElse(0);
        double max = points.stream().mapToDouble(SessionProfitHistory.Point::coins).max().orElse(0);
        var bounds = scale.update(min, max, nowNanos, generation != snapshot.generation() || lastWindow != windowMillis);
        generation = snapshot.generation();
        lastWindow = windowMillis;
        double step = ProfitGraphScale.tickStep(bounds.max() - bounds.min());
        double firstTick = Math.ceil(bounds.min() / step) * step;
        double offset = ProfitGraphScale.offset(bounds);
        float labelWidth = 48f;
        float plotX = x + labelWidth + 7f;
        float plotY = y + 23f;
        float plotWidth = width - (plotX - x) - 3f;
        float plotHeight = 65f;
        nvg.text(Fonts.REGULAR, "Net profit (coins)", x, y + 3f, 9f, Theme.HUD_LABEL);
        if (offset != 0) {
            String label = AetherLang.localize("Axis offset") + String.format(Locale.ROOT, " %+,.0f", offset);
            nvg.textRight(Fonts.MONO, label, x, y + 3f, width, 8f, Theme.HUD_LABEL);
        }

        for (double tick = firstTick; tick <= bounds.max(); tick += step) {
            float tickY = plotY + plotHeight * (1f - (float) bounds.fraction(tick));
            nvg.line(plotX, tickY, plotX + plotWidth, tickY, tick == 0 ? 1f : 0.6f, Theme.HUD_SEP);
            nvg.textRight(Fonts.MONO, ProfitGraphScale.label(tick - offset, step), x, tickY - 4f,
                    labelWidth, 8f, Theme.HUD_LABEL);
        }
        nvg.line(plotX, plotY, plotX, plotY + plotHeight, 0.7f, Theme.HUD_SEP);
        nvg.line(plotX, plotY + plotHeight, plotX + plotWidth, plotY + plotHeight, 0.7f, Theme.HUD_SEP);
        nvg.text(Fonts.MONO, ageLabel(windowMillis), plotX, plotY + plotHeight + 6f, 8f, Theme.HUD_LABEL);
        String middle = ageLabel(windowMillis / 2);
        nvg.textCentered(Fonts.MONO, middle, plotX, plotY + plotHeight + 6f, plotWidth, 10f, 8f, Theme.HUD_LABEL);
        nvg.textRight(Fonts.MONO, "Now", plotX, plotY + plotHeight + 6f, plotWidth, 8f, Theme.HUD_LABEL);
        nvg.textCentered(Fonts.REGULAR, "Active time", plotX, y + 105f, plotWidth, 11f, 8f, Theme.HUD_LABEL);

        int required = points.size() * 4;
        if (vertices.length < required) vertices = new float[required];
        int count = 0;
        float previousY = 0;
        double start = snapshot.elapsedMillis() - windowMillis;
        for (var point : points) {
            float px = plotX + (float) ((point.timeMillis() - start) / windowMillis) * plotWidth;
            float py = plotY + (1f - (float) bounds.fraction(point.coins())) * plotHeight;
            if (count > 0) {
                vertices[count++] = px;
                vertices[count++] = previousY;
            }
            vertices[count++] = px;
            vertices[count++] = py;
            previousY = py;
        }
        nvg.pushScissor(plotX, plotY - 1f, plotWidth + 1f, plotHeight + 2f);
        nvg.polyline(vertices, count / 2, 1.5f, Theme.HUD_ACCENT);
        nvg.popScissor();
    }

    static List<SessionProfitHistory.Point> visiblePoints(SessionProfitHistory.Snapshot snapshot, long windowMillis) {
        double start = Math.max(0, snapshot.elapsedMillis() - windowMillis);
        var points = new ArrayList<SessionProfitHistory.Point>();
        long previous = 0;
        for (var point : snapshot.points()) {
            if (point.timeMillis() <= start) {
                previous = point.coins();
            } else {
                if (points.isEmpty()) points.add(new SessionProfitHistory.Point(start, previous));
                points.add(point);
            }
        }
        if (points.isEmpty()) points.add(new SessionProfitHistory.Point(start, previous));
        points.add(new SessionProfitHistory.Point(snapshot.elapsedMillis(), points.getLast().coins()));
        return points;
    }

    private static String ageLabel(long millis) {
        long seconds = millis / 1_000;
        return seconds % 60 == 0 ? "-" + seconds / 60 + "m" : "-" + seconds / 60 + "m " + seconds % 60 + "s";
    }
}
