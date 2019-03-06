package info.anecdot.content;

import com.sun.org.apache.xml.internal.utils.DefaultErrorHandler;
import info.anecdot.xml.DomAndXPathSupport;
import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.map.LazyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Service
public class ItemService implements DomAndXPathSupport {

    private final XPath xPath = XPathFactory.newInstance().newXPath();

    @Autowired
    private ItemRepository itemRepository;

    @Override
    public XPath getXPath() {
        return xPath;
    }

    public List<Item> findAllItemsBySite(Site site) {
        return itemRepository.findAllByAsset_Site(site);
    }

    public Item findItemByAsset(Asset asset) {
        return itemRepository.findByAsset(asset);
    }

    public void deleteItem(Item item) {
        itemRepository.delete(item);
    }

    public void deleteItemForAsset(Asset asset) {
        Item item = findItemByAsset(asset);
        if (item != null) {
            deleteItem(item);
        }
    }

    private Item saveItem(Item item) {
        return itemRepository.save(item);
    }

    private Knot fromNode(Node node) {

//        String ref = attribute("ref", node);
//        if (StringUtils.hasText(ref)) {
//            node = node(ref, node.getOwnerDocument().getDocumentElement());
//        }

        Knot knot;

        if (hasChildElements(node)) {

            if (isRootNode(node)) {
                knot = new Item();
                ((Item) knot).setType(node.getNodeName());
            } else {
                knot = new Knot();
            }

            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node childNode = children.item(i);
                if (childNode.getNodeName().startsWith("#")) {
                    continue;
                }

                String childName = childNode.getNodeName();
                Sequence sequence = knot.getSequence(childName);
                if (sequence == null) {
                    sequence = new Sequence();
                    knot.addSequence(childName, sequence);
                }

                sequence.appendChild(fromNode(childNode));
            }

        } else {
            knot = new Text();
            ((Text) knot).setValue(node.getTextContent());
        }

        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr) attributes.item(i);
                knot.getAttributes().put(attribute.getName(), attribute.getValue());
            }
        }

        return knot;
    }

    public Item loadItem(Asset asset) {
        Item item = findItemByAsset(asset);
        if (item != null) {
            deleteItem(item);
        }

        Site site = asset.getSite();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Path file = Paths.get("./tmp", site.getHost(), asset.getPath());
        try (InputStream inputStream = Files.newInputStream(file)) {
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new DefaultErrorHandler());

            Document document = db.parse(inputStream);

            item = (Item) fromNode(document.getDocumentElement());
            item.setAsset(asset);

//            item.setSite(site);
//            item.setSyncId(site.getSyncId());

//            URI uri = site.toURI(file);
//            item.setUri(uri);

//            BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
//            LocalDateTime lastModified = LocalDateTime.ofInstant(
//                    fileAttributes.lastModifiedTime().toInstant(),
//                    ZoneOffset.systemDefault());
//            item.setLastModified(lastModified);

//            loadMeta(document, item);

            return saveItem(item);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <K> Map<K, Object> createMap() {
        return LazyMap.lazyMap(new LinkedHashMap<K, Object>() {
            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        }, this::createMap);
    }

    public Map<String, Object> toMap(Knot knot, Map<String, Object> parent) {
        Map<String, Object> map = createMap();

        map.put("#payload", knot);
        map.put("#knot", knot);
        map.put("#name", Optional.ofNullable(knot.getParent()).map(Sequence::getName).orElse(null));
        map.put("#value", knot instanceof Text ? ((Text) knot).getValue() : null);
        map.put("#parent", new AbstractMapDecorator<String, Object>(parent) {
            @Override
            public String toString() {
                Map<String, Object> decorated = decorated();
                return decorated.getClass().getName() + '@' + System.identityHashCode(decorated);
            }
        });

        List<Object> children = new ArrayList<>();

        knot.getSequences().forEach((sequence) -> {
            Map<Object, Object> values = createMap();
            int i = 0;

            for (Knot child : sequence.getChildren()) {
                Map<String, Object> childMap = toMap(child, map);
                if (i == 0) {
                    values.putAll(childMap);
                }
                values.put(Integer.toString(i++), childMap);
            }

            map.put(sequence.getName(), values);
            children.add(values);
        });

        map.put("#children", children);

        return map;
    }

    public Map<String, Object> toMap(Knot knot) {
        return toMap(knot, Collections.emptyMap());
    }
}
