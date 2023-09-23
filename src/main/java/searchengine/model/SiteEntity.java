package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "sites")
@Getter
@Setter
@NoArgsConstructor
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum ('INDEXING', 'INDEXED', 'FAILED') NOT NULL")
    private StatusType status;

    @Column(name = "status_time", nullable = false)
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String url;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String name;

    @OneToMany(mappedBy = "siteEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Fetch(value = FetchMode.SELECT)
    private List<PageEntity> pages;

    @OneToMany(mappedBy = "siteEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LemmaEntity> lemmas;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SiteEntity that = (SiteEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}



