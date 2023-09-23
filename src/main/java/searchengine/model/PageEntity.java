package searchengine.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;


@Entity
@Table(name = "pages")
@Getter
@Setter
@NoArgsConstructor
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteEntity;

    @Column(columnDefinition = "TEXT NOT NULL, UNIQUE KEY pathIndex (path(256), site_id)")
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    @OneToMany(mappedBy = "pageEntity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<IndexEntity> indexes;

}
