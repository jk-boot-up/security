package com.jk.explore.security.saml;

import java.security.Key;
import java.security.PublicKey;
import java.util.List;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Validates one XML signature and says which URI it covers. Who supplies the key is the only difference between the service providers. */
final class SignatureCheck {

    /** What a successful validation proved: the signature is valid for a key, and it covers exactly the element with this ID. */
    record Result(String coveredId, PublicKey key) {
    }

    private SignatureCheck() {
    }

    /** The key the signature itself carries in KeyInfo. Trusting this proves only that <em>somebody</em> signed it. */
    static final KeySelector EMBEDDED_KEY = new KeySelector() {
        @Override
        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) throws KeySelectorException {
            if (keyInfo != null) {
                for (Object o : keyInfo.getContent()) {
                    if (o instanceof KeyValue kv) {
                        try {
                            PublicKey k = kv.getPublicKey();
                            return () -> k;
                        } catch (java.security.KeyException e) {
                            throw new KeySelectorException(e);
                        }
                    }
                }
            }
            throw new KeySelectorException("no key");
        }
    };

    static KeySelector pinned(PublicKey trusted) {
        return KeySelector.singletonKeySelector(trusted);
    }

    /** Validates {@code signatureElement} of {@code doc} (IDs must already be registered). */
    static Result validate(Document doc, Element signatureElement, KeySelector selector) {
        try {
            XMLSignatureFactory f = XMLSignatureFactory.getInstance("DOM");
            DOMValidateContext ctx = new DOMValidateContext(selector, signatureElement);
            ctx.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
            XMLSignature sig = f.unmarshalXMLSignature(ctx);
            if (sig.getSignedInfo().getReferences().size() != 1) {
                throw new SamlRefused("signature must cover exactly one element");
            }
            if (!sig.validate(ctx)) {
                throw new SamlRefused("signature is not valid");
            }
            Reference ref = (Reference) sig.getSignedInfo().getReferences().get(0);
            String uri = ref.getURI();
            if (uri == null || !uri.startsWith("#") || uri.length() < 2) {
                throw new SamlRefused("signature does not point at an element of this document");
            }
            Key key = sig.getKeySelectorResult().getKey();
            return new Result(uri.substring(1), (PublicKey) key);
        } catch (SamlRefused e) {
            throw e;
        } catch (Exception e) {
            throw new SamlRefused("signature could not be checked: " + e.getClass().getSimpleName());
        }
    }

    static List<Node> signatures(Document d) {
        var nodes = d.getElementsByTagNameNS(Xml.DS, "Signature");
        List<Node> out = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            out.add(nodes.item(i));
        }
        return out;
    }
}
