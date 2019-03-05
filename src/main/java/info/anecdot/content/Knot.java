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
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "kind")
@DiscriminatorValue("knot")
public class Knot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(insertable = false, updatable = false)
    private String kind;

    @ManyToOne
    private Sequence sequence;

    @Lob
    private String value;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @MapKey(name = "name")
    private final Map<String, Sequence> sequences = new LinkedHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attribute",
            uniqueConstraints = @UniqueConstraint(columnNames = {"knot_id", "name"}))
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private final Map<String, String> attributes = new LinkedHashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public Sequence getSequence() {
        return sequence;
    }

    public void setSequence(Sequence sequence) {
        this.sequence = sequence;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
            removed.setParent(null);
            removed.setName(null);
        }

        sequence.setName(name);
        sequence.setParent(this);

        return removed;
    }
    
    public Item getItem() {
        if (this instanceof Item) {
            return (Item) this;
        } else if (sequence != null) {
            Knot parent = sequence.getParent();
            if (parent != null) {
                return parent.getItem();
            }
        }

        return null;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
