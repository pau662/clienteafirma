package es.gob.afirma.standalone.signdetails;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import es.gob.afirma.core.AOInvalidFormatException;
import es.gob.afirma.core.signers.AOSigner;
import es.gob.afirma.core.signers.AOSignerFactory;
import es.gob.afirma.core.signers.AdESPolicy;
import es.gob.afirma.core.util.tree.AOTreeModel;
import es.gob.afirma.signers.xades.XAdESConstants;
import es.gob.afirma.signers.xades.XAdESUtil;
import es.gob.afirma.signers.xml.Utils;
import es.gob.afirma.signers.xml.XMLConstants;
import es.gob.afirma.signvalidation.SignValidity;
import es.gob.afirma.signvalidation.ValidateXMLSignature;
import es.gob.afirma.standalone.SimpleAfirmaMessages;
import es.gob.afirma.standalone.crypto.CompleteSignInfo;
import es.gob.afirma.standalone.crypto.TimestampsAnalyzer;
import es.gob.afirma.standalone.ui.preferences.PreferencesPanelXades;

public class XAdESSignAnalyzer implements SignAnalyzer {

	static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	List <SignDetails> signDetailsList;
	List<CertificateDetails> certDetailsList;
	Document signDocument;
	AOTreeModel signersTree;

	private static final String FORMAT_XADES = "XAdES"; //$NON-NLS-1$s

    public static final String URL_SHA1_RSA    = "http://www.w3.org/2000/09/xmldsig#rsa-sha1"; //$NON-NLS-1$
    private static final String URL_SHA256_RSA  = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"; //$NON-NLS-1$
    private static final String URL_SHA384_RSA  = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384"; //$NON-NLS-1$
    private static final String URL_SHA512_RSA  = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512"; //$NON-NLS-1$

    public static final Map<String, String> SIGN_ALGOS_URI;
    static {
    	SIGN_ALGOS_URI = new HashMap<>();
    	SIGN_ALGOS_URI.put(URL_SHA1_RSA, "SHA1withRSA"); //$NON-NLS-1$
    	SIGN_ALGOS_URI.put(URL_SHA256_RSA, "SHA256withRSA"); //$NON-NLS-1$
    	SIGN_ALGOS_URI.put(URL_SHA384_RSA, "SHA384withRSA"); //$NON-NLS-1$
    	SIGN_ALGOS_URI.put(URL_SHA512_RSA, "SHA512withRSA"); //$NON-NLS-1$
	}

	static final String[] SUPPORTED_XADES_NAMESPACE_URIS = new String[] {
			XAdESConstants.NAMESPACE_XADES_NO_VERSION,
		    XAdESConstants.NAMESPACE_XADES_1_2_2,
		    XAdESConstants.NAMESPACE_XADES_1_3_2,
		    XAdESConstants.NAMESPACE_XADES_1_4_1
	};

	public XAdESSignAnalyzer(final byte [] data) throws Exception {
    	try {

            CompleteSignInfo signInfo;
            signInfo = getSignInfo(data);

            this.signersTree = signInfo.getSignsTree();
    		this.signDetailsList = new ArrayList<SignDetails>();
    		this.certDetailsList = new ArrayList<CertificateDetails>();
    		this.signDocument = Utils.getNewDocumentBuilder().parse(new ByteArrayInputStream(data));
    		final NodeList signaturesList = this.signDocument.getElementsByTagNameNS(XMLConstants.DSIGNNS, XMLConstants.TAG_SIGNATURE);
    		createSignDetails(signaturesList);
    	}
    	catch (final Exception e) {
    		throw new AOInvalidFormatException("No se ha podido cargar el documento XML de firmas", e); //$NON-NLS-1$
    	}
	}

	@Override
	public AOTreeModel getSignersTree() {
		return this.signersTree;
	}

	@Override
	public List<SignDetails> getAllSignDetails() {
		return this.signDetailsList;
	}

	@Override
	public String getSignFormat() {
		return FORMAT_XADES;
	}

	@Override
	public String getDataLocation() {
        // Tomamos la raiz del documento
        final Element rootSig = this.signDocument.getDocumentElement();

        // Identificamos el tipo de la firma por medio de las referencias de la primera de ellas
    	final Element signatureElement = XAdESUtil.getFirstSignatureElement(this.signDocument.getDocumentElement());

    	// Obtenemos el listado de referencias a datos de la firma
    	final List<Element> dataReferenceList = XAdESUtil.getSignatureDataReferenceList(signatureElement);

        // Establecemos la variante de firma
    	if (XAdESUtil.isSignatureElementEnveloped(signatureElement, dataReferenceList)) {
        	return SimpleAfirmaMessages.getString("ValidationInfoDialog.12"); //$NON-NLS-1$
        }
    	else if (XAdESUtil.isSignatureWithManifest(dataReferenceList)) {
        	return SimpleAfirmaMessages.getString("ValidationInfoDialog.16"); //$NON-NLS-1$
        }
        else if (XAdESUtil.isSignatureElementExternallyDetached(dataReferenceList)) {
        	return SimpleAfirmaMessages.getString("ValidationInfoDialog.15"); //$NON-NLS-1$
        }
        else if (XAdESUtil.isSignatureElementInternallyDetached(rootSig, dataReferenceList)) {
        	return SimpleAfirmaMessages.getString("ValidationInfoDialog.14"); //$NON-NLS-1$
        }
        else if (XAdESUtil.isSignatureElementEnveloping(signatureElement, dataReferenceList)) {
        	return SimpleAfirmaMessages.getString("ValidationInfoDialog.13"); //$NON-NLS-1$
        }

    	return ISignatureFormatDetector.FORMAT_UNRECOGNIZED;
	}

	private void createSignDetails(final NodeList signaturesList) throws AOInvalidFormatException {
    	try {
    		for (int i = 0 ; i < signaturesList.getLength() ; i++) {
    			final Element signature = (Element) signaturesList.item(i);
    			final XAdESSignDetails signDetails = buildSignDetails(signature);
    			final List<SignValidity> validity = ValidateXMLSignature.validateSign(signature);
    			signDetails.setValidityResult(validity);
    			this.signDetailsList.add(signDetails);
    		}
    	}
    	catch (final Exception e) {
    		throw new AOInvalidFormatException("No se ha podido cargar el documento XML de firmas", e); //$NON-NLS-1$
    	}
	}

	private XAdESSignDetails buildSignDetails(final Element signElement) throws AOInvalidFormatException {
		final XAdESSignDetails xadesSignDetails = new XAdESSignDetails();

		final String format = SignatureFormatDetectorXades.resolveSignerXAdESFormat(signElement);
		xadesSignDetails.setFormat(format);
		final Element signatureMethodElement = XAdESUtil.getSignatureMethodElement(signElement);
		if (signatureMethodElement != null) {
			String algorithm = SIGN_ALGOS_URI.get(signatureMethodElement.getAttribute("Algorithm")); //$NON-NLS-1$
			if (algorithm == null) {
				algorithm = signatureMethodElement.getAttribute("Algorithm"); //$NON-NLS-1$
			}
			xadesSignDetails.setAlgorithm(algorithm);
		}

		// ARBOL DE FIRMANTES
		buildCertDetails(signElement.getElementsByTagNameNS(XMLConstants.DSIGNNS, "X509Data").item(0), //$NON-NLS-1$
				xadesSignDetails.getSigners());

		// QUALIFYING PROPERTIES
		final NodeList qualifyingPropsNodeList = signElement
				.getElementsByTagNameNS("*", XAdESConstants.TAG_QUALIFYING_PROPERTIES); //$NON-NLS-1$

		boolean existingNamespace = false;

		if (qualifyingPropsNodeList.getLength() > 0) {
			final Element qualifyingProps = (Element) qualifyingPropsNodeList.item(0);
			final String namespaceUri = qualifyingProps.getNamespaceURI();

			for (final String xadesNameSpace : SUPPORTED_XADES_NAMESPACE_URIS) {
				if (xadesNameSpace.equals(namespaceUri)) {
					existingNamespace = true;
				}
			}

			if (!existingNamespace) {
				throw new AOInvalidFormatException(
						"Una de las firmas encontradas en el documento contiene una version inexistente de XAdES"); //$NON-NLS-1$
			}

			// FIRMANTE
			buildCertDetails(qualifyingProps.getElementsByTagNameNS(namespaceUri, "X509Data").item(0), //$NON-NLS-1$
					xadesSignDetails.getSigners());

			// INFORMACION DECLARADA DE DATOS
			final NodeList signedDataObjectPropertiesList = qualifyingProps.getElementsByTagNameNS(namespaceUri,
					XAdESConstants.TAG_SIGNED_DATA_OBJECT_PROPERTIES);
			if (signedDataObjectPropertiesList.getLength() > 0) {
				final NodeList dataObjectFormatList = ((Element) signedDataObjectPropertiesList.item(0))
						.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_DATA_OBJECT_FORMAT);
				final List<DataObjectFormat> dataObjectFormats = new ArrayList<DataObjectFormat>();
				for (int m = 0; m < dataObjectFormatList.getLength(); m++) {
					final DataObjectFormat dof = new DataObjectFormat();
					final Element dataObjectFormaElmt = (Element) dataObjectFormatList.item(m);
					final String ref = dataObjectFormaElmt.getAttribute("ObjectReference"); //$NON-NLS-1$
					dof.setIdentifier(ref);
					final NodeList descriptionNodeList = dataObjectFormaElmt
							.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_DESCRIPTION);
					if (descriptionNodeList.getLength() > 0) {
						dof.setDescription(descriptionNodeList.item(0).getTextContent().trim());
					}
					dof.setMimeType(dataObjectFormaElmt.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_MIME_TYPE)
							.item(0).getTextContent().trim());
					dataObjectFormats.add(dof);
				}

				xadesSignDetails.setDataObjectFormats(dataObjectFormats);
			}

			// POLITICA
			final NodeList signaturePolicyIdentifierList = qualifyingProps.getElementsByTagNameNS(namespaceUri,
					XAdESConstants.TAG_SIGNATURE_POLICY_IDENTIFIER);
			if (signaturePolicyIdentifierList != null && signaturePolicyIdentifierList.getLength() > 0) {
				final Element signPolicyIdElement = (Element) signaturePolicyIdentifierList.item(0);
				if (signPolicyIdElement != null) {
					final String policyOID = signPolicyIdElement
							.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_IDENTIFIER).item(0).getTextContent().trim();

					final Node descriptionElement = signPolicyIdElement.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_DESCRIPTION).item(0);
					String descriptionPolicy = ""; //$NON-NLS-1$
					if (descriptionElement != null) {
						descriptionPolicy = descriptionElement.getTextContent().trim();
					}

					if (PreferencesPanelXades.POLICY_XADES_AGE_1_9.getPolicyIdentifier().equals(policyOID)) {
						final SignaturePolicy signPolicy = new SignaturePolicy(
								SimpleAfirmaMessages.getString("PreferencesPanel.73"), //$NON-NLS-1$
								PreferencesPanelXades.POLICY_XADES_AGE_1_9);
						xadesSignDetails.setPolicy(signPolicy);
					} else {
						final String signElementNS = signElement.getNamespaceURI();
						final String digestValue = signPolicyIdElement
								.getElementsByTagNameNS(signElementNS, XAdESConstants.TAG_DIGEST_VALUE).item(0).getTextContent().trim();
						final String digestMethod = ((Element) signPolicyIdElement
								.getElementsByTagNameNS(signElementNS, XAdESConstants.TAG_DIGEST_METHOD).item(0))
										.getAttribute("Algorithm"); //$NON-NLS-1$
						String spuri = ""; //$NON-NLS-1$
						final NodeList spuriNodeList = signPolicyIdElement.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_SPURI);
						if (spuriNodeList.getLength() > 0) {
							spuri = signPolicyIdElement.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_SPURI)
									.item(0).getTextContent().trim();
						}

						final AdESPolicy policy = new AdESPolicy(policyOID, digestValue, digestMethod, spuri);

						final SignaturePolicy signPolicy = new SignaturePolicy(descriptionPolicy, policy);
						xadesSignDetails.setPolicy(signPolicy);
					}
				}
			}

			// METADATOS
			final NodeList signatureProductionPlaceList = qualifyingProps.getElementsByTagNameNS(namespaceUri,
					XAdESConstants.TAG_SIGNATURE_PRODUCTION_PLACE);
			Map<String, String> metadata = new HashMap<String, String>();
			if (signatureProductionPlaceList.getLength() > 0) {
				metadata = getProductionPlaceMetadata(signatureProductionPlaceList, namespaceUri);
			} else {
				final NodeList signatureProductionPlaceV2List = qualifyingProps.getElementsByTagNameNS(namespaceUri,
						XAdESConstants.TAG_SIGNATURE_PRODUCTION_PLACE_V2);
				if (signatureProductionPlaceV2List.getLength() > 0) {
					metadata = getProductionPlaceMetadata(signatureProductionPlaceV2List, namespaceUri);
				}
			}

			final NodeList signerRoleList = qualifyingProps.getElementsByTagNameNS(namespaceUri,
					XAdESConstants.TAG_SIGNER_ROLE);
			if (signerRoleList.getLength() > 0) {
				final String[] signerRoleArray = getClaimedRoleMetadata((Element) signerRoleList.item(0), namespaceUri);
				if (signerRoleArray != null) {
					for (int i = 0 ; i < signerRoleArray.length ; i++) {
						metadata.put("claimedRole" + i, signerRoleArray[i]); //$NON-NLS-1$
					}
				}
			} else {
				final NodeList signerRoleV2List = qualifyingProps.getElementsByTagNameNS(namespaceUri,
						XAdESConstants.TAG_SIGNER_ROLE_V2);
				if (signerRoleV2List.getLength() > 0) {
					final String[] signerRoleV2Array = getClaimedRoleMetadata((Element) signerRoleV2List.item(0), namespaceUri);
					if (signerRoleV2Array != null) {
						for (int i = 0 ; i < signerRoleV2Array.length ; i++) {
							metadata.put("claimedRole" + i, signerRoleV2Array[i]); //$NON-NLS-1$
						}
					}
				}
			}

			xadesSignDetails.setMetadata(metadata);
		}
		return xadesSignDetails;

	}

	private void buildCertDetails(final Node dataCertNode, final List<CertificateDetails> signersList) {
		if (dataCertNode != null) {
			final Element certElement = (Element) ((Element) dataCertNode).getElementsByTagNameNS(XMLConstants.DSIGNNS, XAdESConstants.TAG_X509_CERTIFICATE).item(0);
			final X509Certificate cert = Utils.getCertificate(certElement);
			final CertificateDetails certDetails = new CertificateDetails(cert);
			final Node childCertElement = certElement.getElementsByTagNameNS(XMLConstants.DSIGNNS, "X509Data").item(0); //$NON-NLS-1$
			if (childCertElement != null) {
				buildCertDetails(childCertElement, certDetails.getSubCertDetails());
			}
			signersList.add(certDetails);
		}
	}

    /** Recupera la informaci&oacute;n de la firma indicada.
     * @param signData Firma.
     * @return Informaci&oacute;n de la firma.
     * @throws IOException Si ocurren problemas relacionados con la lectura de los datos */
    private static CompleteSignInfo getSignInfo(final byte[] signData) throws IOException {
        final CompleteSignInfo signInfo = new CompleteSignInfo();
        signInfo.setSignData(signData);
        final AOSigner signer = AOSignerFactory.getSigner(signData);
        if (signer == null) {
        	LOGGER.warning("Formato de firma no reconocido"); //$NON-NLS-1$
            throw new IllegalArgumentException("Formato de firma no reconocido"); //$NON-NLS-1$
        }
        try {
            signInfo.setSignInfo(signer.getSignInfo(signData));
        }
        catch (final Exception e) {
        	LOGGER.log(Level.WARNING, "Error al leer la informacion de la firma", e); //$NON-NLS-1$
        }
        try {
        	signInfo.setSignsTree(signer.getSignersStructure(signData, true));
        }
        catch (final Exception e) {
        	LOGGER.log(Level.WARNING, "Error al extraer el arbol de firmantes", e);  //$NON-NLS-1$
        	signInfo.setSignsTree(null);
        }
        try {
            signInfo.setData(signer.getData(signData));
        }
        catch (final Exception e) {
        	LOGGER.log(Level.WARNING, "Error al extraer los datos firmados", e);  //$NON-NLS-1$
        }
        try {
        	signInfo.setTimestampsInfo(
    			TimestampsAnalyzer.getTimestamps(signData)
			);
        }
        catch (final Exception e) {
        	LOGGER.log(Level.WARNING, "Error al extraer los sellos de tiempo", e);  //$NON-NLS-1$
        }
        return signInfo;
    }

    private static Map<String, String> getProductionPlaceMetadata(final NodeList signatureProductionPlaceList, final String namespaceUri) {
    	final Map<String, String> metadata = new HashMap<String, String>();
		final Element signProdPlaceNode = (Element) signatureProductionPlaceList.item(0);
		final NodeList streetAddressNode = signProdPlaceNode
				.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_STREET_ADDRESS);
		if (streetAddressNode.getLength() > 0) {
			metadata.put(SimpleAfirmaMessages.getString("ValidationInfoDialog.11"), //$NON-NLS-1$
					streetAddressNode.item(0).getTextContent().trim());
		}
		final NodeList postalCodeNode = signProdPlaceNode
				.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_POSTAL_CODE);
		if (postalCodeNode.getLength() > 0) {
			metadata.put(SimpleAfirmaMessages.getString("ValidationInfoDialog.8"), //$NON-NLS-1$
					postalCodeNode.item(0).getTextContent().trim());
		}
		final NodeList cityNode = signProdPlaceNode.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_CITY);
		if (cityNode.getLength() > 0) {
			metadata.put(SimpleAfirmaMessages.getString("ValidationInfoDialog.6"), //$NON-NLS-1$
					cityNode.item(0).getTextContent().trim());
		}
		final NodeList stateOrProvinceNode = signProdPlaceNode.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_STATE_OR_PROVINCE);
		if (stateOrProvinceNode.getLength() > 0) {
			metadata.put(SimpleAfirmaMessages.getString("ValidationInfoDialog.7"), //$NON-NLS-1$
					stateOrProvinceNode.item(0).getTextContent().trim());
		}
		final NodeList countryNode = signProdPlaceNode.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_COUNTRY_NAME);
		if (countryNode.getLength() > 0) {
			metadata.put(SimpleAfirmaMessages.getString("ValidationInfoDialog.9"), //$NON-NLS-1$
					countryNode.item(0).getTextContent().trim());
		}
		return metadata;

    }

    private static String[] getClaimedRoleMetadata(final Element signerRole, final String namespaceUri) {
    	String [] result = null;
    	final NodeList claimedRolesNode = signerRole
    			.getElementsByTagNameNS(namespaceUri, XAdESConstants.TAG_CLAIMED_ROLE);
    	if (claimedRolesNode.getLength() > 0) {
    		result = new String[claimedRolesNode.getLength()];
			for (int i = 0 ; i < claimedRolesNode.getLength() ; i++) {
				final Element role = (Element) claimedRolesNode.item(i);
				result[i] = role.getTextContent().trim();
			}
    	}
    	return result;
    }

}
