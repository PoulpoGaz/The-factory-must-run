package fr.poulpogaz.run;

import com.badlogic.gdx.utils.Array;
import fr.poulpogaz.run.factory.blocks.Blocks;

public class ContentManager {

    @SuppressWarnings("unchecked")
    private static final Array<Content>[] contentMap = new Array[ContentType.values.length];

    static {
        for (int i = 0; i < contentMap.length; i++) {
            contentMap[i] = new Array<>();
        }
    }

    public static void createContent() {
        Blocks.load();
    }

    public static void loadContent() {
        for (Array<Content> array : contentMap) {
            for (int i = 0; i < array.size; i++) {
                array.get(i).load();
            }
        }
    }

    public static void register(Content content) {
        getContentOfType(content.getContentType()).add(content);
    }

    public static Array<Content> getContentOfType(ContentType type) {
        return contentMap[type.ordinal()];
    }
}
