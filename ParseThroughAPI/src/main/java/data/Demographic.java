package data;

import jakarta.persistence.*;

@Entity
@Table(name = "demographic")
public class Demographic {
    @Id
    @Column(name = "mal_id")
    public int malId;

    @Column(nullable = false)
    public String name;

    public String url;

    public int getMalId() {
        return malId;
    }

    public void setMalId(int malId) {
        this.malId = malId;
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
        if (o == null || getClass() != o.getClass()) return false;
        Demographic that = (Demographic) o;
        return malId == that.malId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(malId);
    }
}
