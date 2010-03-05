package org.springframework.roo.addon.mvc.jsp;

import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides convenience methods which can be used for round tripping xml documents.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class JspRoundTripper {
	
	/**
	 * This method will compare the original document with the proposed document and return 
	 * an adjusted document if necessary. Adjustments are only made if new elements or 
	 * attributes are proposed. Changes to the order of attributes or elements in the 
	 * original document will not result in an adjustment.
	 * 
	 * @param original document as read from the file system
	 * @param proposed document as determined by the JspViewManager
	 * @return the new document if changes are necessary, null if no changes are necessary
	 */
	public boolean compareDocuments(Document original, Document proposed) {
		boolean originalDocumentAdjusted = false;
//		originalDocumentAdjusted = checkNamespaces(original, proposed);
		originalDocumentAdjusted = compareElements(original.getDocumentElement(), proposed.getDocumentElement(), false);
		return originalDocumentAdjusted;
	}
	
	/**
	 * Compare necessary namespace declarations between original and proposed document, if 
	 * namespaces in the original are missing compared to the proposed, we add them to the 
	 * original.
	 * 
	 * @param original document as read from the file system
	 * @param proposed document as determined by the JspViewManager
	 * @return the new document if changes are necessary, null if no changes are necessary
	 */
	private boolean checkNamespaces(Document original, Document proposed) {
	    boolean originalDocumentChanged = false;
		NamedNodeMap nsNodes = proposed.getDocumentElement().getAttributes();
		for (int i = 0; i < nsNodes.getLength(); i++) {
			if (null == XmlUtils.findFirstAttribute("//@" + nsNodes.item(i).getNodeName(), original.getDocumentElement())) {
				original.getDocumentElement().setAttribute(nsNodes.item(i).getNodeName(), nsNodes.item(i).getNodeValue());
				originalDocumentChanged = true;
			}
		}
		return originalDocumentChanged;
	}
	
	private boolean compareElements(Element original, Element proposed, boolean originalDocumentChanged) {
		NodeList proposedChildren = proposed.getChildNodes();
		for (int i = 0; i < proposedChildren.getLength(); i++) {
			Element proposedElement = (Element) proposedChildren.item(i);
			String proposedId = proposedElement.getAttribute("id");
			if (! (proposedId.length() == 0)) { //only proposed elements with an id will be considered
				Element originalElement = XmlUtils.findFirstElement("//*[@id='" + proposedId + "']", original);				
				if (null == originalElement) { //insert proposed element given the original document has no element with a matching id
					Element placeHolder = XmlUtils.findFirstElementByName("util:placeholder", original);
					if (placeHolder != null) { //insert right before place holder if we can find it
						placeHolder.getParentNode().insertBefore(original.getOwnerDocument().importNode(proposedElement, false), placeHolder);
					} else { //find the best place to insert the element
						if (proposed.getAttribute("id").length() != 0) { //try to find the id of the proposed element's parent id in the original document 
							Element originalParent = XmlUtils.findFirstElement("//*[@id='" + proposed.getAttribute("id") + "']", original);
							if (originalParent != null) { //found parent with the same id, so we can just add it as new child
								originalParent.appendChild(original.getOwnerDocument().importNode(proposedElement, false));
							} else { //no parent found so we add it as a child of the root element (last resort)
								original.appendChild(original.getOwnerDocument().importNode(proposedElement, false));
							}
						} else { //no parent found so we add it as a child of the root element (last resort)
							original.appendChild(original.getOwnerDocument().importNode(proposedElement, false));
						}
					}
					originalDocumentChanged = true;
				} else { //we found a element in the original document with a matching id		
					if (!equalElements(originalElement, proposedElement)) { //check if the elements are equal
						String originalElementHashCode = originalElement.getAttribute("z");
						if (originalElementHashCode.length() > 0) { //only act if a hash code exists
							if (originalElementHashCode.equals(XmlUtils.calculateUniqueKeyFor(originalElement))) { //only act if hashcodes match (no user changes in the element)
								originalElement.getParentNode().replaceChild(original.getOwnerDocument().importNode(proposedElement, false), originalElement); //replace the original with the proposed element
								originalDocumentChanged = true;
							} 
						}
					}
				}
			}
			originalDocumentChanged = compareElements(original, proposedElement, originalDocumentChanged);
		}
		return originalDocumentChanged;
	}
	
	private boolean equalElements(Element a, Element b) {
		if (!a.getTagName().equals(b.getTagName())) { 
			return false;
		}
		if (a.getAttributes().getLength() != b.getAttributes().getLength()) {
			return false;
		}
		NamedNodeMap attributes = a.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node node = attributes.item(i);
			if (!node.getNodeName().equals("z") && !node.getNodeName().startsWith("_")) {
				if (b.getAttribute(node.getNodeName()).length() == 0 || !b.getAttribute(node.getNodeName()).equals(node.getNodeValue())) {
					return false;
				}
			}
		}
		return true;
	}
}
