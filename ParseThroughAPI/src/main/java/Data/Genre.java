package Data;

import jakarta.persistence.*;

@Entity
@Table(name = "genre")
public class Genre {
    @Id
    @Column(name = "mal_id")
    public int malId;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public String name;

    public String url;

    public int getMalId() {
        return malId;
    }

    public void setMalId(int malId) {
        this.malId = malId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Producer)) return false;
        return malId == ((Producer) o).malId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(malId);
    }
}
