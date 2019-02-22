package info.anecdot.content;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Entity
public class Sequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    String name;

    @ManyToOne(optional = false)
    private Knot parent;

    @OneToMany(mappedBy = "sequence", fetch = FetchType.EAGER, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @OrderColumn(name = "ordinal")
    private final List<Knot> knots = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public Knot getParent() {
        return parent;
    }

    public void setParent(Knot parent) {
        this.parent = parent;
    }

    public List<Knot> getKnots() {
        return Collections.unmodifiableList(knots);
    }

    public void appendKnot(Knot knot) {
        if (knots.add(knot)) {
            knot.setSequence(this);
        }
    }
}
