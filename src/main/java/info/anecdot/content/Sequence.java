package info.anecdot.content;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"knot_id", "name"}))
public class Sequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "knot_id")
    private Knot knot;

    private String name;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private final List<Knot> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Knot getKnot() {
        return knot;
    }

    public void setKnot(Knot knot) {
        this.knot = knot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Knot> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void appendChild(Knot child) {
        if (children.add(child)) {
            child.setParent(this);
        }
    }
}
