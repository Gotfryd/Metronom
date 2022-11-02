package pl.pwr.metronom;

public class SongsList {
    private String composer;
    private String title;
    private short trackBpm;

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public short getTrackBpm() {
        return trackBpm;
    }

    public void setTrackBpm(short trackBpm) {
        this.trackBpm = trackBpm;
    }

    @Override
    public String toString() {
        return "SongsList{" +
                "composer='" + composer + '\'' +
                ", title='" + title + '\'' +
                ", trackBpm=" + trackBpm +
                '}';
    }
}
