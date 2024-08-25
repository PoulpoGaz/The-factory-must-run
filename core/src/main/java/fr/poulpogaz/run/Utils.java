package fr.poulpogaz.run;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;

import java.util.function.Predicate;
import java.util.function.Supplier;

import static fr.poulpogaz.run.Variables.*;

public class Utils {

    public static final GlyphLayout layout = new GlyphLayout();

    public static void drawStringCentered(String str, float x, float y, float width, float height) {
        layout.setText(font, str);

        font.draw(batch, str,
                x + (width - layout.width) / 2,
                y - (height - layout.height) / 2);
    }

    public static void drawStringCentered(String str, float x, float y, float width) {
        layout.setText(font, str);

        font.draw(batch, str,
                x + (width - layout.width) / 2,
                y);
    }

    public static <T> Pool<T> newPool(Supplier<T> constructor) {
        return new Pool<>() {
            @Override
            protected T newObject() {
                return constructor.get();
            }
        };
    }


    public static void drawRoundedLine(ShapeRenderer sr, float x1, float y1, float x2, float y2, float width) {
        sr.rectLine(x1, y1, x2, y2, width);

        float dx = x2 - x1;
        float dy = y2 - y1;

        float alpha = MathUtils.radiansToDegrees * MathUtils.atan2(dy, dx);
        float theta = 90 - alpha;

        float radius = width / 2;
        sr.arc(x2, y2, radius, -theta, 180, 8);
        sr.arc(x1, y1, radius, -theta + 180, 180, 8);
    }

    public static void drawPolyRoundedLine(ShapeRenderer sr, float[] vertices, float width) {
        drawPolyRoundedLine(sr, vertices, width, 0, vertices.length);
    }

    public static void drawPolyRoundedLine(ShapeRenderer sr, float[] vertices, float width, int start, int count) {
        for (int i = start; i < count - 2; i += 2) {
            drawRoundedLine(sr,
                            vertices[i], vertices[i + 1], vertices[i + 2], vertices[i + 3],
                            width);
        }
    }




    public static boolean lineRectangleIntersects(float x1, float y1, float x2, float y2,
                                                  float xMin, float yMin, float xMax, float yMax) {
        return x1 >= xMin && y1 >= yMin && x1 < xMax && y1 < yMax
            || x2 >= xMin && y2 >= yMin && x2 < xMax && y2 < yMax
            || lineRectangleIntersects(x1, y1, x2, y2,
                                       xMin, yMin, xMax, yMin)
            || lineRectangleIntersects(x1, y1, x2, y2,
                                       xMin, yMax, xMax, yMax)
            || lineRectangleIntersects(x1, y1, x2, y2,
                                       xMin, yMin, xMin, yMax)
            || lineRectangleIntersects(x1, y1, x2, y2,
                                       xMax, yMin, xMax, yMax);
    }

    public static boolean lineLineIntersects(float x1, float y1, float x2, float y2,
                                             float x3, float y3, float x4, float y4) {
        float div = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        float t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / div;
        float u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / div;

        return div > 0
            ? (0 <= t && t <= div && 0 <= u && u <= div)
            : (0 >= t && t >= div && 0 >= u && u >= div);
    }


    public static void draw(TextureRegion region, float x, float y, float rotationDeg) {
        float w = region.getRegionWidth();
        float h = region.getRegionHeight();

        batch.draw(region, x, y, w / 2f, h / 2, w, h, 1, 1, rotationDeg);
    }

    public static TextureRegion flip(TextureRegion region, boolean flipX, boolean flipY) {
        if (!flipX && !flipY) {
            return region;
        }

        TextureRegion r = new TextureRegion(region);
        r.flip(flipX, flipY);
        return r;
    }

    public static TextureRegion[] loadAnimation(String name) {
        return atlas.findRegions(name).toArray();
    }


    public static <T> int count(T[] array, T element) {
        int n = 0;
        for (T t : array) {
            if (t == element) {
                n++;
            }
        }

        return n;
    }

    public static <T> int indexOf(T[] array, Predicate<T> equals) {
        for (int i = 0; i < array.length; i++) {
            if (equals.test(array[i])) {
                return i;
            }
        }

        return -1;
    }
}
