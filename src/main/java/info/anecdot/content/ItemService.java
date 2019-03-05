package info.anecdot.content;

import com.sun.org.apache.xml.internal.utils.DefaultErrorHandler;
import info.anecdot.xml.DomAndXPathSupport;
import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.map.LazyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
public class ItemService implements DomAndXPathSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EntityManager entityManager;

    private XPath xPath;

    @Override
    public XPath getXPath() {
        if (xPath == null) {
            xPath = XPathFactory.newInstance().newXPath();
        }

        return xPath;
    }

    public Item findItemBySiteAndURI(Site site, URI uri) {
        if (uri.getPath().equals("/")) {
            uri = URI.create(site.getHome());
        }

        return itemRepository.findItemByURI(uri);
    }

    public Item findItemBySiteAndURI(Site site, String uri) {
        return findItemBySiteAndURI(site, URI.create(uri));
    }

    @Transactional
    public boolean deleteItemBySiteAndURI(Site site, URI uri) {
        Item item = findItemBySiteAndURI(site, uri);
        if (item != null) {
            itemRepository.delete(item);

            return true;
        }

        return false;
    }

    @Transactional
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

        String ref = attribute("ref", node);
        if (StringUtils.hasText(ref)) {
            node = node(ref, node.getOwnerDocument().getDocumentElement());
        }

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

    private void loadMeta(Document document, Item item) {
        String expression = text("@meta", document.getDocumentElement());
        if (StringUtils.isEmpty(expression)) {
            expression = "current()/meta";
        }

        Node meta = node(expression, document.getDocumentElement());
        if (meta != null) {
            Node description = node("description", meta, true);
            item.setDescription(description.getTextContent());

            URI image = URI.create(text("image", meta));
            if (StringUtils.hasLength(image.getPath())) {
                item.setImage(image);
            }
        }
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
            item.setSyncId(site.getSyncId());

            URI uri = site.toURI(file);
            item.setUri(uri);

            BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                    fileAttributes.lastModifiedTime().toInstant(),
                    ZoneOffset.systemDefault());
            item.setLastModified(lastModified);

            loadMeta(document, item);

            return saveItem(item);
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void deleteAllObsoleteItems(Site site) {
        List<Item> items = itemRepository.findAllObsolete(site);
        itemRepository.deleteAll(items);

        int n = items.size();
        if (n > 0) {
            LOG.info("Deleted {} obsolete items", n);
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
        map.put("#name", Optional.ofNullable(knot.getSequence()).map(Sequence::getName).orElse(null));
        map.put("#value", knot.getValue());
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
