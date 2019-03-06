package info.anecdot.content;

import javax.persistence.*;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Stephan Grundner
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Knot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Sequence parent;

    @OneToMany(mappedBy = "knot", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @MapKey(name = "name")
    private final Map<String, Sequence> sequences = new LinkedHashMap<>();

    @ElementCollection
    @CollectionTable(name = "knot_attribute")
    @Column(name = "value")
    @MapKeyColumn(name = "name")
    @OrderColumn(name = "ordinal")
    private final Map<String, String> attributes = new LinkedHashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sequence getParent() {
        return parent;
    }

    public void setParent(Sequence parent) {
        this.parent = parent;
    }

    public Collection<Sequence> getSequences() {
        return Collections.unmodifiableCollection(sequences.values());
    }

    public Sequence getSequence(String name) {
        return sequences.get(name);
    }

    public Sequence addSequence(String name, Sequence sequence) {
        Sequence removed = sequences.put(name, sequence);
        if (removed != null && removed != sequence) {
            removed.setKnot(null);
            removed.setName(null);
        }

        sequence.setName(name);
        sequence.setKnot(this);

        return removed;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Item getItem() {
        if (this instanceof Item) {
            return (Item) this;
        } else if (parent != null) {
            Knot knot = parent.getKnot();
            if (knot != null) {
                return knot.getItem();
            }
        }

        return null;
    }
}
