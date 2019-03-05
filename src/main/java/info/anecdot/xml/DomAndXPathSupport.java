package info.anecdot.xml;

import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Stephan Grundner
 */
public interface DomAndXPathSupport {

    class NodeListIterator implements Iterator<Node> {

        private final NodeList nodeList;
        private int i = 0;

        @Override
        public boolean hasNext() {
            return i < nodeList.getLength();
        }

        @Override
        public Node next() {
            return nodeList.item(i++);
        }

        public NodeListIterator(NodeList nodeList) {
            this.nodeList = nodeList;
        }
    }

    class NamedNodeMapIterator implements Iterator<Node> {

        private final NamedNodeMap namedNodeMap;
        private int i = 0;

        @Override
        public boolean hasNext() {
            return i < namedNodeMap.getLength();
        }

        @Override
        public Node next() {
            return namedNodeMap.item(i++);
        }

        public NamedNodeMapIterator(NamedNodeMap namedNodeMap) {
            this.namedNodeMap = namedNodeMap;
        }
    }

    static Stream<Node> nodes(NodeList nodes) {
        NodeListIterator nodeListIterator = new NodeListIterator(nodes);
        return StreamSupport.stream(Spliterators
                .spliterator(nodeListIterator, 0L, 0), false);
    }

    static Stream<Node> nodes(NamedNodeMap nodes) {
        NamedNodeMapIterator namedNodeMapIterator = new NamedNodeMapIterator(nodes);
        return StreamSupport.stream(Spliterators
                .spliterator(namedNodeMapIterator, 0L, 0), false);
    }

    XPath getXPath();

    default Stream<Node> nodes(String expression, Object source) {
        try {
            return nodes((NodeList) getXPath().evaluate(expression, source, XPathConstants.NODESET));
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    default Node node(String expression, Object source, boolean resolveRef) {
        try {
            Node node = (Node) getXPath().evaluate(expression, source, XPathConstants.NODE);
            if (resolveRef) {
                String ref = attribute("ref", node);
                if (StringUtils.hasText(ref)) {
                    node = node(ref, source);
                }
            }

            return node;
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    default Node node(String expression, Object source) {
        return node(expression, source, false);
    }

    default String text(String expression, Object source) {
        try {
            return (String) getXPath().evaluate(expression, source, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    default String attribute(String name, Node node) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            Attr attr = (Attr) attributes.getNamedItem(name);
            if (attr != null) {
                return attr.getValue();
            }
        }

        return null;
    }
}
