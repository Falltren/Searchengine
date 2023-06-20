package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Table(name = "lemmas")
@Setter
@Getter
@NoArgsConstructor
@Entity
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteEntity;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String lemma;

    @Column(nullable = false)
    private Integer frequency;

    @OneToMany(mappedBy = "lemmaEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IndexEntity> indexes;
}
