package Data;

import jakarta.persistence.*;

@Entity
@Table(name = "user_stat")
public class UserStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    @Column(name = "days_watched")
    private double daysWatched;

    @Column(name = "mean_score")
    private double meanScore;

    @Column(nullable = false)
    private int watching;

    @Column(nullable = false)
    private int completed;

    @Column(name = "on_hold", nullable = false)
    private int onHold;

    @Column(nullable = false)
    private int dropped;

    @Column(name = "plan_to_watch", nullable = false)
    private int planToWatch;

    @Column(name = "total_entries", nullable = false)
    private int totalEntries;

    @Column(nullable = false)
    private int rewatched;

    @Column(name = "episodes_watched", nullable = false)
    private int episodesWatched;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public double getDaysWatched() {
        return daysWatched;
    }

    public void setDaysWatched(double days_watched) {
        this.daysWatched = days_watched;
    }

    public double getMeanScore() {
        return meanScore;
    }

    public void setMeanScore(double mean_score) {
        this.meanScore = mean_score;
    }

    public int getWatching() {
        return watching;
    }

    public void setWatching(int watching) {
        this.watching = watching;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getOnHold() {
        return onHold;
    }

    public void setOnHold(int on_hold) {
        this.onHold = on_hold;
    }

    public int getDropped() {
        return dropped;
    }

    public void setDropped(int dropped) {
        this.dropped = dropped;
    }

    public int getPlanToWatch() {
        return planToWatch;
    }

    public void setPlanToWatch(int plan_to_watch) {
        this.planToWatch = plan_to_watch;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int total_entries) {
        this.totalEntries = total_entries;
    }

    public int getRewatched() {
        return rewatched;
    }

    public void setRewatched(int rewatched) {
        this.rewatched = rewatched;
    }

    public int getEpisodesWatched() {
        return episodesWatched;
    }

    public void setEpisodesWatched(int episodes_watched) {
        this.episodesWatched = episodes_watched;
    }
}