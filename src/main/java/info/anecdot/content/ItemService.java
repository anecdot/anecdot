package info.anecdot.content;

import com.sun.org.apache.xml.internal.utils.DefaultErrorHandler;
import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.map.LazyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EntityManager entityManager;

    public Item findItemBySiteAndURI(Site site, String uri) {
        if (uri.equals("/")) {
            uri = site.getHome();
        }

        return itemRepository.findItemByURI(uri);
    }

    @Transactional
    public boolean deleteItemBySiteAndURI(Site site, String uri) {
        Item item = findItemBySiteAndURI(site, uri);
        if (item != null) {
            itemRepository.delete(item);

            return true;
        }

        return false;
    }

    public Item saveItem(Item item) {
        Site site = item.getSite();
        if (deleteItemBySiteAndURI(site, item.getUri())) {
            entityManager.flush();
        }

        return itemRepository.saveAndFlush(item);
    }

    private boolean hasChildElements(Node node) {
        NodeList children = node.getChildNodes();
        if (children.getLength() == 0) {
            return false;
        }

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }

        return false;
    }

    private boolean isRootNode(Node node) {
        Node parentNode = node.getParentNode();
        return parentNode != null && parentNode.getNodeType() == Node.DOCUMENT_NODE;
    }

    private Knot fromNode(Node node) {
        Knot knot;

        if (isRootNode(node)) {
            knot = new Item();
            ((Item) knot).setType(node.getNodeName());
        } else {
            knot = new Knot();
        }

        if (hasChildElements(node)) {

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

                sequence.appendKnot(fromNode(childNode));
            }
        } else {
            knot.setValue(node.getTextContent());
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

    @Transactional
//    @CacheEvict(cacheNames = "items", key = "{#site.host, #site.toURI(#file)}")
    public Item loadItem(Site site, Path file) throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try (InputStream inputStream = Files.newInputStream(file)) {
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new DefaultErrorHandler());

            Document document = db.parse(inputStream);

            Item item = (Item) fromNode(document.getDocumentElement());
            item.setSite(site);

            String uri = site.toURI(file);
            item.setUri(uri);

            BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                    fileAttributes.lastModifiedTime().toInstant(),
                    ZoneOffset.systemDefault());
            item.setLastModified(lastModified);

            return saveItem(item);
        } catch (SAXException | ParserConfigurationException e) {
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

    public Map<String, Object> toMap(Knot payload, Map<String, Object> parent) {
        Map<String, Object> map = createMap();

        map.put("#payload", payload);
        map.put("#name", Optional.ofNullable(payload.getSequence()).map(Sequence::getName).orElse(null));
        map.put("#value", payload.getValue());
//        if (payload instanceof Item) {
//            map.put("#tags", ((Item) payload).getTags());
//        }
        map.put("#parent", new AbstractMapDecorator<String, Object>(parent) {
            @Override
            public String toString() {
                Map<String, Object> decorated = decorated();
                return decorated.getClass().getName() + '@' + System.identityHashCode(decorated);
            }
        });

        List<Object> children = new ArrayList<>();

        payload.getSequences().forEach((sequence) -> {
            Map<Object, Object> values = createMap();
            int i = 0;

            for (Knot child : sequence.getKnots()) {
                Map<String, Object> childMap = toMap(child, map);
                if (i == 0) {
                    values.putAll(childMap);
                }
                values.put(Integer.toString(i++), childMap);
            }

            map.put(sequence.name, values);
            children.add(values);
        });

        map.put("#children", children);

        return map;
    }

    public Map<String, Object> toMap(Knot payload) {
        return toMap(payload, Collections.emptyMap());
    }
}
