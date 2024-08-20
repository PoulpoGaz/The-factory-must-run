package fr.poulpogaz.run;

// in game content
public abstract class Content {

    protected final String name;

    public Content(String name) {
        this.name = name;
        ContentManager.register(this);
    }

    public abstract void load();

    public abstract ContentType getContentType();
}
